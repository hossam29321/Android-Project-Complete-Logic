package com.magicsurvivor.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color; // Needed for Color.RED
import android.graphics.Paint; // Needed for drawing the circle

public class Explosion extends GameObject {

    private final Bitmap[] sprites;
    private int currentFrame = 0;
    private float animationTimer = 0f;
    private boolean isFinished = false;

    // Debug: Hitbox Paint
    private final Paint hitboxPaint;

    public Explosion(float x, float y, int size, Bitmap[] sprites) {
        super(x, y, size, size);
        this.sprites = sprites;

        // Initialize the red outline for the hitbox
        this.hitboxPaint = new Paint();
        this.hitboxPaint.setColor(Color.RED);
        this.hitboxPaint.setStyle(Paint.Style.STROKE);
        this.hitboxPaint.setStrokeWidth(5f); // 5 pixels thick so you can see it
    }

    @Override
    public void updateLogic() {
        // Not used
    }

    @Override
    public void updateLogic(float deltaTime) {
        if (isFinished) return;

        animationTimer += deltaTime;
        if (animationTimer >= GameConstants.EXPLOSION_ANIMATION_SPEED) {
            animationTimer = 0f;
            currentFrame++;

            // If we've played all frames, the explosion is done
            if (currentFrame >= sprites.length) {
                isFinished = true;
                currentFrame = sprites.length - 1; // Clamp to last frame
            }
        }
    }

    @Override
    public void drawOnScreen(Canvas canvas) {
        // Not used
    }

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        if (isFinished || sprites == null || sprites.length == 0) return;

        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;

        float left = drawX - sizeWidth / 2f;
        float top = drawY - sizeHeight / 2f;

        // Draw the explosion sprite
        int safeFrame = Math.min(currentFrame, sprites.length - 1);
        canvas.drawBitmap(sprites[safeFrame], left, top, null);

//        // --- DRAW HITBOX ---
//        // Get the scaled radius from GameConstants (same value used for damage)
//        float radius = GameConstants.scale(GameConstants.EXPLOSION_RADIUS);
//
//        // Draw the red circle around the center
//        canvas.drawCircle(drawX, drawY, radius, hitboxPaint);
    }

    public boolean isFinished() {
        return isFinished;
    }
}