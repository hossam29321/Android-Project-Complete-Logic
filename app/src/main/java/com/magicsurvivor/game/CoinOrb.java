package com.magicsurvivor.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class CoinOrb extends GameObject {
    private int moneyVal;
    private Paint coinPaint;
    private Paint coinOutlinePaint;

    public int getMoneyVal() {
        return moneyVal;
    }

    public Paint getCoinPaint() {
        return coinPaint;
    }

    public CoinOrb(float x, float y) {
        super(x, y, (int)GameConstants.scale(GameConstants.COIN_ORB_SIZE), (int)GameConstants.scale(GameConstants.COIN_ORB_SIZE));

        this.moneyVal = (int)((Math.random() * (
                GameConstants.COIN_VALUE_MAX - GameConstants.COIN_VALUE_MIN)) +
                GameConstants.COIN_VALUE_MIN
        );


        // Yellow Circle
        this.coinPaint = new Paint();
        this.coinPaint.setColor(Color.YELLOW);
        this.coinPaint.setStyle(Paint.Style.FILL);
        
        // Black Outline
        this.coinOutlinePaint = new Paint();
        this.coinOutlinePaint.setColor(Color.BLACK);
        this.coinOutlinePaint.setStyle(Paint.Style.STROKE);
        this.coinOutlinePaint.setStrokeWidth(3f);
    }

    @Override
    public void updateLogic() {

    }

    public void drawOnScreen(Canvas canvas) {
    }

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;
        float radius = sizeWidth / 2f;

        // Draw Yellow Circle
        canvas.drawCircle(drawX, drawY, radius, coinPaint);
        // Draw Black Outline
        canvas.drawCircle(drawX, drawY, radius, coinOutlinePaint);
    }
}
