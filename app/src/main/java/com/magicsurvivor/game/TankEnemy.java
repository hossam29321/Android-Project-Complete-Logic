package com.magicsurvivor.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

public class TankEnemy extends Enemy {

    private final Bitmap[] sprites;


    // We pass 'null' for sprites because this enemy draws itself procedurally
    public TankEnemy(float startX, float startY, Bitmap[] sprites) {
        super(startX, startY,
                (int)GameConstants.scale(GameConstants.TANK_SIZE),
                GameConstants.TANK_HEALTH,
                GameConstants.TANK_SPEED,
                GameConstants.TANK_XP_VALUE,
                sprites); // No sprites

//        // Dark Red Body
//        this.bodyPaint = new Paint();
//        this.bodyPaint.setColor(Color.rgb(150, 0, 0));
//        this.bodyPaint.setStyle(Paint.Style.FILL);

        this.sprites=sprites;

        // Bright Red Hitbox Outline
        //    private final Paint bodyPaint;
        Paint hitboxPaint = new Paint();
        hitboxPaint.setColor(Color.RED);
        hitboxPaint.setStyle(Paint.Style.STROKE);
        hitboxPaint.setStrokeWidth(3f);
    }

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;

        float left = drawX - sizeWidth / 2f;
        float top = drawY - sizeHeight / 2f;

        // 1. Draw Sprite (respect stun tint)
        if (sprites != null && sprites.length > 0) {
            Bitmap sprite = sprites[currentFrame];
            if (sprite != null) {
                if (isStunned()) {
                    Paint tint = new Paint();
                    tint.setFilterBitmap(true);
                    tint.setAntiAlias(true);
                    tint.setColorFilter(new PorterDuffColorFilter(Color.argb(100, 50, 110, 200), PorterDuff.Mode.SRC_ATOP));
                    canvas.drawBitmap(sprite, left, top, tint);
                } else {
                    canvas.drawBitmap(sprite, left, top, null);
                }
            }
        }

        // 2. Draw Hitbox (Adjustable Rectangle)
//        float hbWidth = sizeWidth * GameConstants.TANK_HITBOX_WIDTH;
//        float hbHeight = sizeHeight * GameConstants.TANK_HITBOX_HEIGHT;
//
//        float hbLeft = drawX - (hbWidth / 2f);
//        float hbTop = drawY - (hbHeight / 2f);
//
//        canvas.drawRect(hbLeft, hbTop, hbLeft + hbWidth, hbTop + hbHeight, hitboxPaint);
    }

    // Override Hitbox Logic to use Tank Constants
    @Override
    public RectF getHitBox() {
        float width = sizeWidth * GameConstants.TANK_HITBOX_WIDTH;
        float height = sizeHeight * GameConstants.TANK_HITBOX_HEIGHT;

        float left = positionX - (width / 2f);
        float top = positionY - (height / 2f);
        float right = positionX + (width / 2f);
        float bottom = positionY + (height / 2f);

        return new RectF(left, top, right, bottom);
    }
    @Override
    public void updateLogic(Player player, float deltaTime) {
        super.updateLogic(player, deltaTime);

        // Only advance animation if not stunned
        if (!isStunned()) {
            animationTimer += deltaTime;
            if (animationTimer >= GameConstants.TANK_ANIMATION_SPEED) {
                animationTimer = 0f; // Reset timer
                // The '% spriteSheet.length' makes the animation loop
                currentFrame = (currentFrame + 1) % sprites.length;
            }
        }
    }

    // Override damage getter
    @Override
    public float getDmg() {
        return GameConstants.TANK_DAMAGE;
    }
}