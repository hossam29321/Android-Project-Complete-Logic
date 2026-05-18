package com.magicsurvivor.game;

import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

/**
 * CollisionHandler detects interactions (collisions) between different entities.
 */
public class CollisionHandler {

    private final EntityManager entRef;
    private final Player pRef;
    private final GameEngine engineRef;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private final FirebaseAuth refAuth= FirebaseAuth.getInstance();
    private SpatialGrid enemyGrid = new SpatialGrid();

    public CollisionHandler(EntityManager entityManager, Player player, GameEngine engine) {
        this.entRef = entityManager;
        this.pRef = player;
        this.engineRef = engine;
    }

    private RectF getHitBox(GameObject obj) {
        float widthScale = 1.0f;
        float heightScale = 1.0f;

        if (obj instanceof Enemy) {
            widthScale = GameConstants.ENEMY_HITBOX_SCALE_WIDTH;
            heightScale = GameConstants.ENEMY_HITBOX_SCALE_HEIGHT;
        }

        float actualWidth = obj.getSizeWidth() * widthScale;
        float actualHeight = obj.getSizeHeight() * heightScale;

        float left = obj.getPositionX() - (actualWidth / 2f);
        float top = obj.getPositionY() - (actualHeight / 2f);
        float right = obj.getPositionX() + (actualWidth / 2f);
        float bottom = obj.getPositionY() + (actualHeight / 2f);

        return new RectF(left, top, right, bottom);
    }

    private RectF getHitBoxPlayer(GameObject obj) {
        float hitboxScaleWidth = GameConstants.PLAYER_HITBOX_SCALE_WIDTH;
        float hitboxScaleHeight = GameConstants.PLAYER_HITBOX_SCALE_HEIGHT;

        float actualWidth = obj.getSizeWidth() * hitboxScaleWidth;
        float actualHeight = obj.getSizeHeight() * hitboxScaleHeight;

        float left = obj.getPositionX() - (actualWidth / 2f);
        float top = obj.getPositionY() - (actualHeight / 1.3f);
        float right = obj.getPositionX() + (actualWidth / 2f);
        float bottom = obj.getPositionY() + (actualHeight / 2f);
        return new RectF(left, top, right, bottom);
    }

    public void checkAllHits() {
        enemyGrid.clear();
        for (GameObject enemy : entRef.getEnemyList()) {
            enemyGrid.add(enemy);
        }

        skillHitsEnemy();
        enemyHitsPlayer();
        playerCollectsGem();
        playerCollectsHealthOrb();
        playerCollectsCoin();
    }

    private void playerCollectsHealthOrb() {
        float pickupRadius = GameConstants.scale(GameConstants.GEM_PICKUP_RADIUS);
        float pickupRadiusSq = pickupRadius * pickupRadius;

        List<GameObject> orbs = entRef.getHealthOrbList();

        for (int i = orbs.size() - 1; i >= 0; i--) {
            HealthOrb orb = (HealthOrb) orbs.get(i);

            float dx = pRef.getPositionX() - orb.getPositionX();
            float dy = pRef.getPositionY() - orb.getPositionY();
            float distSq = dx*dx + dy*dy;

            if (distSq <= pickupRadiusSq) {
                pRef.heal(orb.getHealAmount());
                entRef.removeHealthOrb(orb);
            }
        }
    }

    private void skillHitsEnemy() {
        List<GameObject> skills = entRef.getSkillList();

        for (int i = 0; i < skills.size(); i++) {
            GameObject skillObj = skills.get(i);

            // 1. FIREBALL CHECK
            if (skillObj instanceof FireballProjectile) {
                FireballProjectile projectile = (FireballProjectile) skillObj;
                if (projectile.isFinished()) continue;

                RectF skillBox = getHitBox(projectile);
                List<GameObject> nearbyEnemies = enemyGrid.getNearby(projectile.getPositionX(), projectile.getPositionY());

                for (GameObject enemyObj : nearbyEnemies) {
                    Enemy enemy = (Enemy) enemyObj;
                    if (!enemy.isDestroyed() && skillBox.intersect(enemy.getHitBox())) {
                        projectile.explode(entRef);
                        break;
                    }
                }
            }
            // 2. ARCANE RAY CHECK
            else if (skillObj instanceof ArcaneRayProjectile) {
                ArcaneRayProjectile ray = (ArcaneRayProjectile) skillObj;
                if (ray.isFinished()) continue;

                if (!ray.isDamageActive()) continue;

                List<GameObject> nearbyEnemies = enemyGrid.getNearby(ray.getPositionX(), ray.getPositionY());

                for (GameObject enemyObj : nearbyEnemies) {
                    Enemy enemy = (Enemy) enemyObj;

                    if (!enemy.isDestroyed() && ray.checkCollision(enemy)) {
                        enemy.takeDamage(ray.getDamage(), entRef);
                        ray.registerHit(enemy);
                    }
                }
            }
            // 3. ELECTRIC FIELD CHECK (NEW)
            else if (skillObj instanceof ElectricField) {
                ElectricField field = (ElectricField) skillObj;
                if (field.isFinished()) continue;

                List<GameObject> nearbyEnemies = enemyGrid.getNearby(field.getPositionX(), field.getPositionY());

                for (GameObject enemyObj : nearbyEnemies) {
                    Enemy enemy = (Enemy) enemyObj;

                    if (!enemy.isDestroyed() && field.checkCollision(enemy)) {
                        if (field.canDamageEnemy(enemy)) {
                            enemy.takeDamage(field.getDamage(), entRef);
                            field.registerHit(enemy);
                        }
                    }
                }
            }
        }
    }

    private void enemyHitsPlayer() {
        RectF playerBox = getHitBoxPlayer(pRef);
        List<GameObject> enemies = entRef.getEnemyList();

        for (GameObject enemyObj : enemies) {
            Enemy enemy = (Enemy) enemyObj;

            if (playerBox.intersect(enemy.getHitBox())) {
                if (enemy.canAttack()) {
                    pRef.takeDamage(enemy.getDmg());
                    enemy.resetAttackCooldown();
                    engineRef.triggerDamageFlash();
                }
            }
        }
    }

    private void playerCollectsGem() {
        float pickupRadius = GameConstants.scale(GameConstants.GEM_PICKUP_RADIUS);
        float pickupRadiusSq = pickupRadius * pickupRadius;

        List<GameObject> gems = entRef.getGemList();

        for (int i = gems.size() - 1; i >= 0; i--) {
            ExperienceGem gem = (ExperienceGem) gems.get(i);

            float dx = pRef.getPositionX() - gem.getPositionX();
            float dy = pRef.getPositionY() - gem.getPositionY();
            float distSq = dx*dx + dy*dy;

            if (distSq <= pickupRadiusSq) {
                pRef.gainXP(gem.getXpValue(), engineRef);
                entRef.removeGem(gem);
            }
        }
    }

    private void playerCollectsCoin() {
        float pickupRadius = GameConstants.scale(GameConstants.COIN_PICKUP_RADIUS);
        float pickupRadiusSq = pickupRadius * pickupRadius;

        List<GameObject> coins = entRef.getCoinList();

        for (int i = coins.size() - 1; i >= 0; i--) {
            CoinOrb orb = (CoinOrb) coins.get(i);

            float dx = pRef.getPositionX() - orb.getPositionX();
            float dy = pRef.getPositionY() - orb.getPositionY();
            float distSq = dx*dx + dy*dy;

            if (distSq <= pickupRadiusSq) {
                pRef.gainCoin(orb.getMoneyVal(), engineRef);
                entRef.removeCoinOrb(orb);

                FirebaseUser user = refAuth.getCurrentUser();
                database = FirebaseDatabase.getInstance();
                assert user != null;
                myRef = database.getReference().child(user.getUid()).child("money");
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int currCoins=snapshot.getValue(Integer.class);
                        myRef.setValue(currCoins+orb.getMoneyVal());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        }
    }


}