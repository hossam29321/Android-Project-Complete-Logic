package com.magicsurvivor.game;

import android.graphics.Canvas;

/**
 * GameObject is the base class for all entities in the game world.
 */
public abstract class GameObject {

    protected float positionX;
    protected float positionY;
    protected float movementVectorX;
    protected float movementVectorY;
    protected int sizeWidth;
    protected int sizeHeight;

    // NOTE: Add a 'protected boolean isDestroyed;' property to manage cleanup
    // in subclasses like Enemy and FireballProjectile.

    public GameObject(float x, float y, int width, int height) {
        this.positionX = x;
        this.positionY = y;
        this.sizeWidth = width;
        this.sizeHeight = height;
        this.movementVectorX = 0;
        this.movementVectorY = 0;
    }

    /** Moves the object and handles any time-based changes. */
    public abstract void updateLogic();
    /** * NEW: Update logic with delta time.
     * Default implementation does nothing, so subclasses can override if needed.
     */
    public void updateLogic(float deltaTime) {
        // Default behavior: do nothing.
        // Subclasses like FireballProjectile and Explosion will override this.
    }

    /** Draws the object's image at its position on the Canvas. */
    public abstract void drawOnScreen(Canvas canvas);

    // Getters for CollisionHandler
    public float getPositionX() { return positionX; }
    public float getPositionY() { return positionY; }
    public int getSizeWidth() { return sizeWidth; }
    public int getSizeHeight() { return sizeHeight; }
    public abstract void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY);
}