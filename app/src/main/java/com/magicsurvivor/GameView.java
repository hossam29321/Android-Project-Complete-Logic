package com.magicsurvivor;

import android.content.Context;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.magicsurvivor.game.GameConstants;
import com.magicsurvivor.game.GameEngine;
import com.magicsurvivor.ui.Joystick;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameThread thread; // Not final anymore, we recreate it
    private GameEngine gameEngine;
    private final Joystick joystick;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        GameConstants.initialize(context);

        float outerRadius = GameConstants.scale(GameConstants.JOYSTICK_OUTER_RADIUS);
        float innerRadius = GameConstants.scale(GameConstants.JOYSTICK_INNER_RADIUS);
        this.joystick = new Joystick(0, 0, outerRadius, innerRadius);

        // MOVE GameEngine initialization here!
        // It stays alive as long as the Activity is alive.
        this.gameEngine = new GameEngine(this.joystick, context);
        if (context instanceof GameEngine.GameOverListener) {
            gameEngine.setGameOverListener((GameEngine.GameOverListener) context);
        }

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Check if thread is null or dead before starting a new one
        if (thread == null || thread.getState() == Thread.State.TERMINATED) {
            thread = new GameThread(getHolder(), gameEngine, getContext());
            thread.setRunning(true);
            thread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        if (thread != null) {
            thread.setRunning(false);
            while (retry) {
                try {
                    thread.join(); // Wait for thread to finish current frame
                    retry = false;
                    thread = null; // Important: Clear it so we can create a new one later
                } catch (InterruptedException e) {
                    // keep retrying
                }
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Set the joystick position
        float margin = GameConstants.scale(GameConstants.JOYSTICK_BOTTOM_MARGIN);
        joystick.setCenterPosition(width / 2f, height - margin);

        // Set screen dimensions
        gameEngine.setScreenDimensions(width, height);

        // Note: We do NOT reset player position here, or they will reset every time you minimize!
        // Only set player position if it's the very first launch (optional logic)
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Check for UI interaction first (Level Up Menu)
        // We need a getter for gameState or just pass it blindly
        // Ideally, check: if (gameEngine.isLevelUpState())

        // Simple way: pass to engine, let engine decide if it consumes it
        gameEngine.handleInput(event);

        // Joystick Logic (Only if NOT in level up menu)
        // We can expose the state or add a boolean check
        // For now, let's just allow joystick logic only if we didn't just click a menu

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (joystick.isHandlePressed(event)) {
                    joystick.update(event);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (joystick.isPressed()) {
                    joystick.update(event);
                }
                return true;

            case MotionEvent.ACTION_UP:
                joystick.resetHandle();
                return true;
        }
        return super.onTouchEvent(event);
    }


    public GameThread getThread() {
        return thread;
    }

    // NEW: Get access to game engine for callbacks
    public GameEngine getGameEngine() {
        return gameEngine;
    }
}