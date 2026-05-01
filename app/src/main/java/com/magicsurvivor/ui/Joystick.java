package com.magicsurvivor.ui;

import android.graphics.PointF;
import android.view.MotionEvent;

/**
 * Joystick handles touch input to calculate player movement vectors.
 * Now uses float values for proper scaling.
 */
public class Joystick {

    private final PointF center;
    private final float outerRadius;
    private final float innerRadius;
    private PointF handlePosition;

    private float movementVectorX;
    private float movementVectorY;
    private boolean isPressed;

    public Joystick(float centerX, float centerY, float outerRadius, float innerRadius) {
        this.center = new PointF(centerX, centerY);
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.handlePosition = new PointF(centerX, centerY);
    }

    public boolean isHandlePressed(MotionEvent event) {
        double touchDistance = getDistance(center.x, center.y, event.getX(), event.getY());
        if (touchDistance < outerRadius) {
            isPressed = true;
            return true;
        }
        return false;
    }

    public void update(MotionEvent event) {
        if (!isPressed) return;

        float deltaX = event.getX() - center.x;
        float deltaY = event.getY() - center.y;
        double distance = getDistance(0, 0, deltaX, deltaY);

        if (distance < outerRadius) {
            handlePosition.set(event.getX(), event.getY());
        } else {
            float ratio = outerRadius / (float) distance;
            handlePosition.set(center.x + (deltaX * ratio),
                    center.y + (deltaY * ratio));
        }

        this.movementVectorX = (handlePosition.x - center.x) / outerRadius;
        this.movementVectorY = (handlePosition.y - center.y) / outerRadius;
    }

    public void resetHandle() {
        isPressed = false;
        handlePosition.set(center.x, center.y);
        movementVectorX = 0;
        movementVectorY = 0;
    }

    private double getDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Recalculates the center position of the joystick.
     */
    public void setCenterPosition(float x, float y) {
        center.set(x, y);
        handlePosition.set(x, y);
    }

    public boolean isPressed() { return isPressed; }
    public float getMovementVectorX() { return movementVectorX; }
    public float getMovementVectorY() { return movementVectorY; }
    public PointF getCenter() { return center; }
    public PointF getHandlePosition() { return handlePosition; }
    public float getOuterRadius() { return outerRadius; }
    public float getInnerRadius() { return innerRadius; }
}