package com.magicsurvivor.game;

import android.graphics.Bitmap;
import android.graphics.PointF;
import java.util.List;

public class ArcaneRaySkill extends Skill {

    // Store the sprites here
    private final Bitmap[] raySprites;

    public ArcaneRaySkill(Player player, Bitmap[] sprites) {
        super(player,
                GameConstants.ARCANE_RAY_COOLDOWN,
                GameConstants.ARCANE_RAY_DAMAGE,
                "Arcane Ray",
                "Fires a piercing beam of magic.");
        this.raySprites = sprites;
        
        // Define preset upgrade pattern for Arcane Ray
        this.upgradePath = new SkillUpgrade[] {
            new SkillUpgrade(1, 1.0f, 1.0f, 1.0f, 0, 0, "Arcane Ray Lvl 1: Single ray"),
            new SkillUpgrade(2, 1.12f, 1.0f, 1.0f, 0, 1, "Arcane Ray Lvl 2: +12% damage, dual ray"),
            new SkillUpgrade(3, 1.12f, 0.95f, 1.0f, 0, 0, "Arcane Ray Lvl 3: +5% cooldown"),
            new SkillUpgrade(4, 1.25f, 0.9f, 1.0f, 0, 1, "Arcane Ray Lvl 4: +25% damage, 3x ray"),
            new SkillUpgrade(5, 1.25f, 0.85f, 1.0f, 0, 1, "Arcane Ray Lvl 5: +10% cooldown, 4x ray"),
            new SkillUpgrade(6, 1.4f, 0.8f, 1.0f, 0, 1, "Arcane Ray Lvl 6: +40% damage, 5x ray"),
            new SkillUpgrade(7, 1.4f, 0.75f, 1.0f, 0, 1, "Arcane Ray Lvl 7: +25% cooldown, 6x ray"),
            new SkillUpgrade(8, 1.6f, 0.7f, 1.0f, 0, 2, "Arcane Ray Lvl 8: ULTIMATE: +60% damage, 8x ray")
        };
    }

    @Override
    public void activate(EntityManager entityManager) {
        // Determine ray count based on level and upgrade path
        int rayCount = this.level;
        
        // Add extra rays from upgrade bonuses
        if (upgradePath != null && level > 0 && level <= upgradePath.length) {
            rayCount += upgradePath[level - 1].rayCountBonus;
        }
        
        List<GameObject> enemies = entityManager.getSortedEnemies(playerRef);

        int targetsFound = 0;

        // Fire at closest enemies (already sorted by distance)
        for (int i = 0; i < enemies.size() && targetsFound < rayCount; i++) {
            GameObject target = enemies.get(i);
            fireRay(entityManager, target.getPositionX(), target.getPositionY());
            targetsFound++;
        }

        // Fire leftovers randomly
        while (targetsFound < rayCount) {
            double randomAngle = Math.random() * 2 * Math.PI;
            float aimX = playerRef.getPositionX() + (float) Math.cos(randomAngle) * 1000;
            float aimY = playerRef.getPositionY() + (float) Math.sin(randomAngle) * 1000;
            fireRay(entityManager, aimX, aimY);
            targetsFound++;
        }
    }

    private void fireRay(EntityManager entityManager, float tx, float ty) {
        // Apply character damage bonus
        float damageMultiplier = getCharacterDamageMultiplier();
        float finalDamage = getCurrentDamage() * damageMultiplier;
        
        // Pass the sprites to the projectile
        ArcaneRayProjectile ray = new ArcaneRayProjectile(
                playerRef, tx, ty, finalDamage, raySprites
        );
        entityManager.addSkill(ray);
    }
}