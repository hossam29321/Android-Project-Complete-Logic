package com.magicsurvivor.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.HashSet;
import java.util.Set;

public class ArcaneRayProjectile extends GameObject {

    private final float damage;
    private final float angleRad;
    private float endX, endY;
    private final Player owner;
    private final float rayLength;

    // Animation
    private final Bitmap[] sprites;
    private int currentFrame = 0;
    private float animationTimer = 0f;
    private boolean isFinished = false;

    // Piercing Logic
    private final Set<Enemy> hitEnemies = new HashSet<>();
    private final Paint bitmapPaint; // NEW: To enable filtering for smoother drawing

    public ArcaneRayProjectile(Player owner, float targetX, float targetY, float damage, Bitmap[] sprites) {
        super(owner.getPositionX(), owner.getPositionY(), 0, 0);
        this.owner = owner;
        this.damage = damage;
        this.sprites = sprites;
        this.rayLength = GameConstants.scale(GameConstants.ARCANE_RAY_LENGTH);

        float dx = targetX - owner.getPositionX();
        float dy = targetY - owner.getPositionY();
        this.angleRad = (float) Math.atan2(dy, dx);

        // Initialize Paint for smoother scaling
        this.bitmapPaint = new Paint();
        this.bitmapPaint.setFilterBitmap(true); // Important for scaling
        this.bitmapPaint.setAntiAlias(true);

        updateRayPosition();
    }

    @Override
    public void updateLogic(float deltaTime) {
        if (isFinished) return;

        // 1. Walk with Player
        this.positionX = owner.getPositionX();
        this.positionY = owner.getPositionY();
        updateRayPosition();

        // 2. Animate
        animationTimer += deltaTime;
        if (animationTimer >= GameConstants.ARCANE_RAY_ANIMATION_SPEED) {
            animationTimer = 0f;
            currentFrame++;

            if (currentFrame >= sprites.length) {
                isFinished = true;
                currentFrame = sprites.length - 1;
            }
        }
    }

    private void updateRayPosition() {
        this.endX = this.positionX + (float) Math.cos(angleRad) * rayLength;
        this.endY = this.positionY + (float) Math.sin(angleRad) * rayLength;
    }

    public boolean isDamageActive() {
        return currentFrame == 3;
    }

    public boolean checkCollision(Enemy enemy) {
        if (!isDamageActive()) return false;
        if (hitEnemies.contains(enemy)) return false;

        float px = enemy.getPositionX();
        float py = enemy.getPositionY();
        float x1 = this.positionX;
        float y1 = this.positionY;
        float x2 = this.endX;
        float y2 = this.endY;

        float A = px - x1;
        float B = py - y1;
        float C = x2 - x1;
        float D = y2 - y1;

        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = -1;
        if (len_sq != 0) param = dot / len_sq;

        float xx, yy;
        if (param < 0) { xx = x1; yy = y1; }
        else if (param > 1) { xx = x2; yy = y2; }
        else { xx = x1 + param * C; yy = y1 + param * D; }

        float dx = px - xx;
        float dy = py - yy;
        float distSq = dx * dx + dy * dy;

        float radius = GameConstants.scale(GameConstants.ENEMY_SIZE) * 2.0f;
        return distSq < (radius * radius);
    }

    public void registerHit(Enemy enemy) {
        hitEnemies.add(enemy);
    }

    @Override
    public void updateLogic() {}

    @Override
    public void drawOnScreen(Canvas canvas) {}

    // === UPDATED DRAW METHOD ===
    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;

        float angleDeg = (float) Math.toDegrees(angleRad);

        canvas.save();
        canvas.translate(drawX, drawY);
        canvas.rotate(angleDeg);

        if (sprites != null && currentFrame < sprites.length) {
            Bitmap sprite = sprites[currentFrame];

            // 1. Determine size
            float beamThickness = GameConstants.scale(GameConstants.ARCANE_RAY_WIDTH);

            // Calculate scale to preserve aspect ratio
            float scale = beamThickness / (float) sprite.getHeight();
            float tileWidth = sprite.getWidth() * scale;

            // 2. OVERLAP FIX: Make the tile slightly wider than the step
            // This forces each segment to bleed into the next, hiding the gap.
            float overlap = 2.0f;

            // Loop from 0 to rayLength
            for (float x = 0; x < rayLength; x += tileWidth) {

                // Draw the tile slightly wider (+ overlap) to cover seams
                RectF dst = new RectF(x, -beamThickness / 2f, x + tileWidth + overlap, beamThickness / 2f);

                // Use the bitmapPaint for filtering
                canvas.drawBitmap(sprite, null, dst, bitmapPaint);
            }
        }

        canvas.restore();
    }

    public boolean isFinished() { return isFinished; }
    public float getDamage() { return damage; }
}