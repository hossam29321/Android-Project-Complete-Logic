package com.magicsurvivor.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ElectricField extends GameObject {

    private final Player owner;
    private float damage;
    private int level;
    private boolean isFinished = false;

    // Animation
    private final Bitmap[] sprites;
    private int currentFrame = 0;
    private float animationTimer = 0f;

    // Damage timing
    private float damageTimer = 0f;
    private final Map<Enemy, Float> enemyCooldowns; // Track per-enemy damage cooldown
    // Debug: total number of stun applications by all ElectricField instances
    public static volatile int totalStunsApplied = 0;

    // Visual
    private final Paint bitmapPaint;

    public ElectricField(Player owner, float damage, Bitmap[] sprites, int level) {
        super(owner.getPositionX(), owner.getPositionY(), 0, 0);
        this.owner = owner;
        this.damage = damage;
        this.level = level;
        this.sprites = sprites;
        this.enemyCooldowns = new HashMap<>();

        // Initialize Paint for smooth rendering
        this.bitmapPaint = new Paint();
        this.bitmapPaint.setFilterBitmap(true);
        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setAlpha(180); // Slightly transparent (0-255)
    }

    @Override
    public void updateLogic(float deltaTime) {
        // kept for compatibility but actual updating with enemies happens in updateLogic(EntityManager,...)
        // animate and follow player when called without EntityManager
        if (isFinished) return;
        this.positionX = owner.getPositionX();
        this.positionY = owner.getPositionY();
        animationTimer += deltaTime;
        if (animationTimer >= GameConstants.ELECTRIC_FIELD_ANIMATION_SPEED) {
            animationTimer = 0f;
            currentFrame = (currentFrame + 1) % sprites.length;
        }
        // shrink cooldowns
        Iterator<Map.Entry<Enemy, Float>> iterator = enemyCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Enemy, Float> entry = iterator.next();
            Enemy enemy = entry.getKey();
            float cooldown = entry.getValue();
            if (enemy.isDestroyed()) { iterator.remove(); continue; }
            cooldown -= deltaTime;
            if (cooldown <= 0) iterator.remove(); else entry.setValue(cooldown);
        }
    }

    /**
     * Full update that has access to the world (enemies) so we can apply damage & stun.
     */
    public void updateLogic(EntityManager entityManager, float deltaTime) {
        if (isFinished) return;

        // Follow the player
        this.positionX = owner.getPositionX();
        this.positionY = owner.getPositionY();

        // Animate
        animationTimer += deltaTime;
        if (animationTimer >= GameConstants.ELECTRIC_FIELD_ANIMATION_SPEED) {
            animationTimer = 0f;
            currentFrame = (currentFrame + 1) % sprites.length;
        }

        // Update per-enemy cooldowns
        Iterator<Map.Entry<Enemy, Float>> iterator = enemyCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Enemy, Float> entry = iterator.next();
            Enemy enemy = entry.getKey();
            float cooldown = entry.getValue();
            if (enemy.isDestroyed()) { iterator.remove(); continue; }
            cooldown -= deltaTime;
            if (cooldown <= 0) iterator.remove(); else entry.setValue(cooldown);
        }

        // Damage & stun nearby enemies per tick
        for (GameObject obj : entityManager.getEnemyList()) {
            if (!(obj instanceof Enemy)) continue;
            Enemy enemy = (Enemy) obj;
            if (enemy.isDestroyed()) continue;
            if (checkCollision(enemy) && canDamageEnemy(enemy)) {
                // Register that this enemy was hit so we obey per-enemy tick cooldown
                registerHit(enemy);
                // Apply damage
                try {
                    enemy.takeDamage(this.damage, entityManager);
                } catch (Exception ignored) { }
                // Apply stun per tick (short duration)
                try {
                        enemy.applyStun(GameConstants.ELECTRIC_FIELD_STUN_DURATION);
                        // Increment debug counter
                        totalStunsApplied++;
                } catch (Exception ignored) { }
            }
        }
    }

    public boolean checkCollision(Enemy enemy) {
        if (enemy.isDestroyed()) return false;

        // Use scaled hitbox radius for collision detection
        float radius = getHitboxRadius();
        float radiusSq = radius * radius;

        // Get enemy hitbox center
        RectF enemyBox = enemy.getHitBox();
        float enemyCenterX = (enemyBox.left + enemyBox.right) / 2f;
        float enemyCenterY = (enemyBox.top + enemyBox.bottom) / 2f;

        // Check distance from field center to enemy center
        float dx = enemyCenterX - this.positionX;
        float dy = enemyCenterY - this.positionY;
        float distSq = dx * dx + dy * dy;

        if (distSq <= radiusSq) return true;

        // Fallback: allow collision if the enemy's bounding size overlaps the field (helpful for large tanks)
        float enemyHalf = Math.max(enemy.getSizeWidth(), enemy.getSizeHeight()) * 0.5f;
        float extra = enemyHalf * 0.8f; // slight padding
        float effective = radius + extra;
        return distSq <= (effective * effective);
    }

    public boolean canDamageEnemy(Enemy enemy) {
        return !enemyCooldowns.containsKey(enemy);
    }

    public void registerHit(Enemy enemy) {
        enemyCooldowns.put(enemy, GameConstants.ELECTRIC_FIELD_DAMAGE_INTERVAL);
    }

    private float getFieldRadius() {
        // Visual radius for drawing
        float baseRadius = GameConstants.ELECTRIC_FIELD_BASE_RADIUS;
        float radiusPerLevel = GameConstants.ELECTRIC_FIELD_RADIUS_PER_LEVEL;
        return GameConstants.scale(baseRadius + (radiusPerLevel * (level - 1)));
    }

    private float getHitboxRadius() {
        // Damage radius (scaled version of visual radius)
        return getFieldRadius() * GameConstants.ELECTRIC_FIELD_HITBOX_SCALE;
    }

    @Override
    public void updateLogic() {}

    @Override
    public void drawOnScreen(Canvas canvas) {}

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        // Apply vertical offset for visual centering
        float offsetY = GameConstants.scale(GameConstants.ELECTRIC_FIELD_VERTICAL_OFFSET);

        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY + offsetY;

        if (sprites != null && currentFrame < sprites.length) {
            Bitmap sprite = sprites[currentFrame];
            if (sprite != null) {
                float fieldRadius = getFieldRadius();
                float fieldDiameter = fieldRadius * 2f;

                // Draw the sprite centered on the player (with offset)
                float left = drawX - fieldRadius;
                float top = drawY - fieldRadius;
                float right = drawX + fieldRadius;
                float bottom = drawY + fieldRadius;

                canvas.drawBitmap(sprite, null,
                        new android.graphics.RectF(left, top, right, bottom),
                        bitmapPaint);
            }
        }

        // DEBUG: Draw hitbox circle (using scaled hitbox radius)
//        Paint debugPaint = new Paint();
//        debugPaint.setColor(android.graphics.Color.YELLOW);
//        debugPaint.setStyle(Paint.Style.STROKE);
//        debugPaint.setStrokeWidth(3f);
//        canvas.drawCircle(drawX, drawY, getHitboxRadius(), debugPaint);
    }

    public void updateStats(float newDamage, int newLevel) {
        this.damage = newDamage;
        this.level = newLevel;
    }

    public float getDamage() {
        return damage;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void markFinished() {
        this.isFinished = true;
    }
}