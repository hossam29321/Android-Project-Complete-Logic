package com.magicsurvivor;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;

import com.magicsurvivor.game.GameEngine;

/**
 * GameThread manages the main loop with delta time for frame-rate independence.
 */
public class GameThread extends Thread {
    private final Context context;

    private final SurfaceHolder surfaceHolder;
    private final GameEngine gameEngine;
    private boolean isRunning;

    private static final double TARGET_FPS = 60.0;
    private static final double FRAME_INTERVAL_MS = 1000.0 / TARGET_FPS;

    // Delta time tracking
    private long lastFrameTime;
    private float deltaTime; // Time since last frame in SECONDS

    public GameThread(SurfaceHolder holder, GameEngine engine,Context context) {
        this.surfaceHolder = holder;
        this.gameEngine = engine;
        this.isRunning = false;
        this.lastFrameTime = System.nanoTime();
        this.context = context;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
        if (running) {
            // Reset timing when starting/resuming
            lastFrameTime = System.nanoTime();
        }
    }

    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;

        // Load assets on a separate thread to avoid blocking the game loop
        new Thread(() -> gameEngine.loadAssets(context)).start();

        while (isRunning) {
            startTime = System.currentTimeMillis();

            // Calculate delta time (time since last frame in seconds)
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
            lastFrameTime = currentTime;

            // Cap delta time to prevent huge jumps (e.g., during pause/resume)
            if (deltaTime > 0.1f) {
                deltaTime = 0.016f; // Default to ~60fps delta if too large
            }

            Canvas canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    // 1. UPDATE: Run all game logic with delta time
                    gameEngine.updateLogic(deltaTime);

                    // 2. DRAW: Clear the screen and draw all entities
                    if (canvas != null) {
                        canvas.drawRGB(0, 0, 0);
                        gameEngine.drawScreen(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            // FPS Control
            timeMillis = System.currentTimeMillis() - startTime;
            waitTime = (long) (FRAME_INTERVAL_MS - timeMillis);

            try {
                if (waitTime > 0) {
                    sleep(waitTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public float getDeltaTime() {
        return deltaTime;
    }
}