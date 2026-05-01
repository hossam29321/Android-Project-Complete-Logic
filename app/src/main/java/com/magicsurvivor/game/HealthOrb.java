package com.magicsurvivor.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class HealthOrb extends GameObject {

    private final float healAmount;
    private final Paint orbPaint;
    private final Paint crossPaint;

    public HealthOrb(float x, float y, float healAmount) {
        super(x, y, (int)GameConstants.scale(GameConstants.HEALTH_ORB_SIZE), (int)GameConstants.scale(GameConstants.HEALTH_ORB_SIZE));
        this.healAmount = healAmount;

        // Red Circle
        this.orbPaint = new Paint();
        this.orbPaint.setColor(Color.RED);
        this.orbPaint.setStyle(Paint.Style.FILL);

        // White Cross
        this.crossPaint = new Paint();
        this.crossPaint.setColor(Color.WHITE);
        this.crossPaint.setStrokeWidth(5f);
        this.crossPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void updateLogic(float deltaTime) {
        // Static for now
    }

    @Override
    public void updateLogic() {}

    @Override
    public void drawOnScreen(Canvas canvas) {}

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;
        float radius = sizeWidth / 2f;

        // Draw Red Circle
        canvas.drawCircle(drawX, drawY, radius, orbPaint);

        // Draw White Cross (+)
        float crossSize = radius * 0.6f;
        canvas.drawLine(drawX - crossSize, drawY, drawX + crossSize, drawY, crossPaint); // Horizontal
        canvas.drawLine(drawX, drawY - crossSize, drawX, drawY + crossSize, crossPaint); // Vertical
    }

    public float getHealAmount() { return healAmount; }
}