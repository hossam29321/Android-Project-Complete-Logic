package com.magicsurvivor.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class ExperienceGem extends GameObject {

    private final int xpValue;
    private final Paint gemPaint;
    private final Paint gemOutlinePaint;

    public ExperienceGem(float x, float y, int xpValue) {
        super(x, y, (int)GameConstants.scale(GameConstants.GEM_SIZE), (int)GameConstants.scale(GameConstants.GEM_SIZE));
        this.xpValue = xpValue;

        this.gemPaint = new Paint();
        this.gemPaint.setColor(Color.CYAN);
        this.gemPaint.setStyle(Paint.Style.FILL);
        
        // Black Outline
        this.gemOutlinePaint = new Paint();
        this.gemOutlinePaint.setColor(Color.BLACK);
        this.gemOutlinePaint.setStyle(Paint.Style.STROKE);
        this.gemOutlinePaint.setStrokeWidth(3f);
    }

    @Override
    public void updateLogic() {
        // Gems are static for now, but you could add a floating animation here
    }

    @Override
    public void updateLogic(float deltaTime) {
        // Static gem
    }

    @Override
    public void drawOnScreen(Canvas canvas) {
        // Not used
    }

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;

        // Draw a simple diamond shape (rotated rect)
        canvas.save();
        canvas.rotate(45, drawX, drawY);
        float halfSize = sizeWidth / 2f;
        canvas.drawRect(drawX - halfSize, drawY - halfSize, drawX + halfSize, drawY + halfSize, gemPaint);
        canvas.drawRect(drawX - halfSize, drawY - halfSize, drawX + halfSize, drawY + halfSize, gemOutlinePaint);
        canvas.restore();
    }

    public int getXpValue() { return xpValue; }
}