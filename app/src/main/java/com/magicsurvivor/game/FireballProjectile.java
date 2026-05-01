package com.magicsurvivor.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import java.util.List;

import android.graphics.RectF; // Import Rect

public class FireballProjectile extends GameObject {

    private final float projectileSpeed;
    private final float travelDistance;
    private float distanceTraveled = 0.0f;
    private boolean isFinished = false;
    private final float damage;

    // Animation
    private final Bitmap[] sprites;
    private final Bitmap[] explosionSprites; // Store explosion sprites
    private int currentFrame = 0;
    private float animationTimer = 0f;

    // Updated Constructor
    public FireballProjectile(float startX, float startY, int size, PointF target, float damage, Bitmap[] sprites, Bitmap[] explosionSprites) {
        super(startX, startY, size, size);
        this.damage = damage;
        this.projectileSpeed = GameConstants.FIREBALL_SPEED;
        this.travelDistance = GameConstants.scale(GameConstants.FIREBALL_RANGE);
        this.sprites = sprites;
        this.explosionSprites = explosionSprites;

        float dx = target.x - startX;
        float dy = target.y - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            this.movementVectorX = (float) (dx / distance);
            this.movementVectorY = (float) (dy / distance);
        }
    }

    @Override
    public void updateLogic() {}

    public void updateLogic(float deltaTime) {
        if (isFinished) return;

        float frameSpeed = GameConstants.getFrameAdjustedSpeed(projectileSpeed, deltaTime);
        this.positionX += this.movementVectorX * frameSpeed;
        this.positionY += this.movementVectorY * frameSpeed;
        distanceTraveled += frameSpeed;

        if (distanceTraveled >= travelDistance) {
            isFinished = true;
        }

        animationTimer += deltaTime;
        if (animationTimer >= GameConstants.FIREBALL_ANIMATION_SPEED) {
            animationTimer = 0f;
            currentFrame = (currentFrame + 1) % sprites.length;
        }
    }
    // === UPDATED EXPLODE METHOD ===
    public void explode(EntityManager entityManager) {
        // 1. Create visual explosion
        int expSize = (int) GameConstants.scale(GameConstants.EXPLOSION_VISUAL_SIZE);
        Explosion explosion = new Explosion(positionX, positionY, expSize, explosionSprites);
        entityManager.addSkill(explosion);

        // 2. Calculate AOE Damage (Rectangle vs Circle Collision)
        float radius = GameConstants.scale(GameConstants.EXPLOSION_RADIUS);
        float radiusSq = radius * radius;

        List<GameObject> enemies = entityManager.getEnemyList();

        for (GameObject obj : enemies) {
            Enemy enemy = (Enemy) obj;

            // Get the enemy's actual hitbox
            RectF enemyBox = enemy.getHitBox();

            // Find the closest point on the enemy rectangle to the explosion center
            float closestX = clamp(this.positionX, enemyBox.left, enemyBox.right);
            float closestY = clamp(this.positionY, enemyBox.top, enemyBox.bottom);

            // Calculate distance from explosion center to that closest point
            float distanceX = this.positionX - closestX;
            float distanceY = this.positionY - closestY;
            float distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);

            // If that distance is less than the radius, we hit!
            if (distanceSquared < radiusSq) {
                enemy.takeDamage(this.damage, entityManager);
            }
        }

        // 3. Remove projectile
        this.markForRemoval();
    }
    // Helper method to constrain a value between min and max
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void drawOnScreen(Canvas canvas) {}

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;

        double angleRad = Math.atan2(movementVectorY, movementVectorX);
        float angleDeg = (float) Math.toDegrees(angleRad);
        float rotation = angleDeg - 180f;

        canvas.save();
        canvas.rotate(rotation, drawX, drawY);

        float left = drawX - sizeWidth / 2f;
        float top = drawY - sizeHeight / 2f;

        if (sprites != null && sprites.length > 0) {
            canvas.drawBitmap(sprites[currentFrame], left, top, null);
        }
        canvas.restore();
    }

    public boolean isFinished() { return isFinished; }
    public float getDamage() { return damage; }
    public void markForRemoval() { this.isFinished = true; }
}