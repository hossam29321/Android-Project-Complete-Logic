package com.magicsurvivor.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import com.magicsurvivor.R;

import java.util.List;

public class FireballSkill extends Skill {

    private final Bitmap[] projectileSprites;
    private final Bitmap[] explosionSprites; // New field
    private final Context skillContext;
    private volatile boolean spritesLoading = false;
    private volatile boolean spritesLoaded = false;

    public FireballSkill(Player player, Context context) {
        super(player,
                GameConstants.FIREBALL_COOLDOWN,
                GameConstants.FIREBALL_DAMAGE,
                "Fireball",
                "Launches a projectile that explodes on impact.");

        this.skillContext = context;
        this.projectileSprites = new Bitmap[4];
        this.explosionSprites = new Bitmap[8];
        
        // Define preset upgrade pattern for levels 1-8
        this.upgradePath = new SkillUpgrade[] {
            new SkillUpgrade(1, 1.0f, 1.0f, 1.0f, 0, "Fireball Lvl 1: Base fireball"),
            new SkillUpgrade(2, 1.15f, 1.0f, 1.0f, 0, "Fireball Lvl 2: +15% damage"),
            new SkillUpgrade(3, 1.15f, 0.9f, 1.0f, 0, "Fireball Lvl 3: +10% cooldown"),
            new SkillUpgrade(4, 1.15f, 0.9f, 1.1f, 0, "Fireball Lvl 4: +10% explosion size"),
            new SkillUpgrade(5, 1.3f, 0.85f, 1.1f, 0, "Fireball Lvl 5: +30% damage, +15% cooldown"),
            new SkillUpgrade(6, 1.3f, 0.85f, 1.15f, 1, "Fireball Lvl 6: +5% size, +1 split"),
            new SkillUpgrade(7, 1.5f, 0.8f, 1.15f, 1, "Fireball Lvl 7: +20% damage, +20% cooldown"),
            new SkillUpgrade(8, 1.5f, 0.75f, 1.2f, 2, "Fireball Lvl 8: ULTIMATE: +2 projectiles")
        };
        
        // Load sprites on background thread
        new Thread(this::loadFireballSprites).start();
    }
    
    /**
     * Load fireball and explosion sprites on background thread.
     */
    private void loadFireballSprites() {
        if (spritesLoading || spritesLoaded) return;
        spritesLoading = true;
        
        try {
            // 1. Load Fireball Sprites (f1-f4)
            int projSize = (int) GameConstants.scale(GameConstants.FIREBALL_SIZE);
            projectileSprites[0] = loadAndScaleSprite(skillContext, R.drawable.f1, projSize);
            projectileSprites[1] = loadAndScaleSprite(skillContext, R.drawable.f2, projSize);
            projectileSprites[2] = loadAndScaleSprite(skillContext, R.drawable.f3, projSize);
            projectileSprites[3] = loadAndScaleSprite(skillContext, R.drawable.f4, projSize);

            // 2. Load Explosion Sprites (e1-e8)
            int expSize = (int) GameConstants.scale(GameConstants.EXPLOSION_VISUAL_SIZE);
            explosionSprites[0] = loadAndScaleSprite(skillContext, R.drawable.e1, expSize);
            explosionSprites[1] = loadAndScaleSprite(skillContext, R.drawable.e2, expSize);
            explosionSprites[2] = loadAndScaleSprite(skillContext, R.drawable.e3, expSize);
            explosionSprites[3] = loadAndScaleSprite(skillContext, R.drawable.e4, expSize);
            explosionSprites[4] = loadAndScaleSprite(skillContext, R.drawable.e5, expSize);
            explosionSprites[5] = loadAndScaleSprite(skillContext, R.drawable.e6, expSize);
            explosionSprites[6] = loadAndScaleSprite(skillContext, R.drawable.e7, expSize);
            explosionSprites[7] = loadAndScaleSprite(skillContext, R.drawable.e8, expSize);
            
            spritesLoaded = true;
        } catch (Exception e) {
            android.util.Log.e("FireballSkill", "Error loading fireball sprites: " + e.getMessage());
        } finally {
            spritesLoading = false;
        }
    }

    @Override
    public void activate(EntityManager entityManager) {
        PointF targetPos = findTarget(entityManager);

        if (targetPos != null) {
            // Apply character damage bonus
            float damageMultiplier = getCharacterDamageMultiplier();
            float finalDamage = getCurrentDamage() * damageMultiplier;
            
            FireballProjectile newProjectile = new FireballProjectile(
                    playerRef.getPositionX(),
                    playerRef.getPositionY(),
                    (int) GameConstants.scale(GameConstants.FIREBALL_SIZE),
                    targetPos,
                    finalDamage,
                    projectileSprites,
                    explosionSprites // Pass explosion sprites here
            );
            entityManager.addSkill(newProjectile);
        }
    }

    private PointF findTarget(EntityManager entityManager) {
        List<GameObject> enemies = entityManager.getEnemyList();
        if (enemies.isEmpty()) return null;

        // Optimized: Find closest enemy to avoid hitting random far-off targets
        GameObject closest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for(GameObject enemy : enemies) {
            float dx = enemy.getPositionX() - playerRef.getPositionX();
            float dy = enemy.getPositionY() - playerRef.getPositionY();
            double dSq = dx*dx + dy*dy;
            if(dSq < minDistanceSq) {
                minDistanceSq = dSq;
                closest = enemy;
            }
        }

        if (closest != null) {
            return new PointF(closest.getPositionX(), closest.getPositionY());
        }
        return null;
    }

    private Bitmap loadAndScaleSprite(Context context, int resId, int size) {
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        return Bitmap.createScaledBitmap(raw, size, size, true);
    }
}