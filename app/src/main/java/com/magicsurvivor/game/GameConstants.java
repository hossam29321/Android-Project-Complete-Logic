package com.magicsurvivor.game;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * GameConstants provides device-independent sizing and timing for consistent gameplay
 * across all devices and frame rates.
 */
public class GameConstants {

    // ============================================================
    // REFERENCE RESOLUTION (Design baseline)
    // ============================================================
    private static final float REFERENCE_DIAGONAL = 2202f;

    // ============================================================
    // CALCULATED SCALE FACTOR (Set once during initialization)
    // ============================================================
    private static float scaleFactor = 1.0f;

    // ============================================================
    // PLAYER CONSTANTS (in reference units)
    // ============================================================
    public static final float PLAYER_SIZE = 160f;
    public static final float PLAYER_HITBOX_SCALE_WIDTH = 0.3f;
    public static final float PLAYER_HITBOX_SCALE_HEIGHT = 0.6f;
    public static final float PLAYER_SPEED = 500;
    public static final int PLAYER_MAX_HEALTH = 100;
    public static final float PLAYER_HEALTHBAR_HEIGHT = 15f;
    public static final float PLAYER_HEALTHBAR_OFFSET = 10f;
    public static final float PLAYER_HEALTHBAR_BORDER = 2f;
    public static final float PLAYER_XP_MULTIPLIER = 1.2f;
    public static final float PLAYER_INITIAL_LEVEL_REQUIREMENT = 200f;

    // ============================================================
    // ENEMY CONSTANTS (in reference units)
    // ============================================================
    // Spawn timing: base interval (seconds between spawns), minimum interval at max difficulty,
    // and the ramp time (seconds) over which difficulty approaches its maximum.
    public static final float ENEMY_SPAWN_BASE_INTERVAL = 2.0f;  // Start with one enemy every 2 seconds
    public static final float ENEMY_SPAWN_MIN_INTERVAL = 0.01f;   // Hard cap: one enemy every 0.5 seconds
    public static final float ENEMY_SPAWN_RAMP_TIME = 15*60-30f;      // Reach near-max difficulty after 10 minutes

    //MOB RUSH STUFF
    public static final float MOB_RUSH_START = 5*60f;
    public static final float MOB_RUSH_END = MOB_RUSH_START+20.0f;

    public static float spawnInterval = ENEMY_SPAWN_BASE_INTERVAL; // Will be updated dynamically in gameEngine

    public static final float ENEMY_SIZE = 100f;
    public static final float ENEMY_SPEED = 120f;
    public static final float ENEMY_HEALTH = 1200;
    public static final int ENEMY_XP_VALUE = 1;
    public static final float ENEMY_SPAWN_MIN_DISTANCE = 500f;
    public static final float ENEMY_SPAWN_MAX_DISTANCE = 900f;
    public static final int ENEMY_DAMAGE = 5;
    public static final float ENEMY_ATTACK_COOLDOWN = 1.0f;
    public static final float ENEMY_ANIMATION_SPEED = 0.15f;
    public static final float ENEMY_HITBOX_SCALE_WIDTH = 0.7f;
    public static final float ENEMY_HITBOX_SCALE_HEIGHT = 0.7f;

    public static final float TANK_XP_VALUE = 5f;
    public static final float TANK_SPEED = 60f;
    public static final float TANK_HEALTH = 5000f;
    public static final float TANK_SIZE = 500f;
    public static final float TANK_ANIMATION_SPEED = 0.13f;//higher means slower


    public static final float TANK_HITBOX_HEIGHT = 0.7f;
    public static final float TANK_HITBOX_WIDTH = 0.7f;

    public static final float TANK_DAMAGE = 10f;

    // ============================================================
    // PROJECTILE CONSTANTS (in reference units)
    // ============================================================
    public static final float FIREBALL_SIZE = 130f;
    public static final float FIREBALL_SPEED = 500f;
    public static final float FIREBALL_RANGE = 2000f;
    public static final float FIREBALL_DAMAGE = 100f;
    public static final float SKILL_COOLDOWN_UPGRADE = 0.95f;
    public static final float SKILL_DAMAGE_UPGRADE = 1.1f;
    public static final float FIREBALL_ANIMATION_SPEED = 0.1f;
    public static final float EXPLOSION_RADIUS = 100f;
    public static final float EXPLOSION_VISUAL_SIZE = 200f;
    public static final float EXPLOSION_ANIMATION_SPEED = 0.09f;

    // ============================================================
    // SKILL TIMING (in seconds, not frames!)
    // ============================================================
    public static final float FIREBALL_COOLDOWN = 1f;

    // ============================================================
    // LOOT & UI CONSTANTS
    // ============================================================
    public static final float GEM_SIZE = 15f;
    public static final float GEM_PICKUP_RADIUS = 50f;
    public static final float GEM_SPAWN_INTERVAL = 3.0f;
    public static final int GEM_XP_VALUE = 5;

    public static final float HEALTH_ORB_SIZE = 40f;
    public static final float HEALTH_ORB_SPAWN_INTERVAL = 15.0f;
    public static final float HEALTH_ORB_HEAL_AMOUNT = 20f;

    public static final float COIN_ORB_SIZE = 40f;
    public static final float COIN_VALUE_MIN = 121f;
    public static final float COIN_VALUE_MAX = 787;
    public static final float COIN_PICKUP_RADIUS = 50f;
    public static final float COIN_MIN_COOLDOWN = 1.0f;



//    public static final float COIN_ORB_SPAWN_INTERVAL = 15.0f;

    // UI - XP Bar
    public static final float UI_XP_BAR_HEIGHT = 30f;
    public static final float UI_MARGIN = 75f;

    // UI - Timer (NEW)
    public static final float UI_TIMER_TEXT_SIZE = 50f;
    public static final float UI_TIMER_TOP_MARGIN = 20f;

    // UI - Level Up Menu
    public static final float UI_MENU_WIDTH = 800f;
    public static final float UI_MENU_HEIGHT = 600f;
    public static final float UI_BUTTON_HEIGHT = 150f;

    public static final float DAMAGE_FLASH_DURATION = 0.2f;

    // ============================================================
    // ANIMATION TIMING (in seconds)
    // ============================================================
    public static final float PLAYER_ANIMATION_SPEED = 0.15f;

    // ============================================================
    // JOYSTICK CONSTANTS (in reference units)
    // ============================================================
    public static final float JOYSTICK_OUTER_RADIUS = 200f;
    public static final float JOYSTICK_INNER_RADIUS = 80f;
    public static final float JOYSTICK_BOTTOM_MARGIN = 300f;

    // ============================================================
    // ARCANE RAY CONSTANTS
    // ============================================================
    public static final float ARCANE_RAY_DAMAGE = 50f;
    public static final float ARCANE_RAY_COOLDOWN = 2.0f;
    public static final float ARCANE_RAY_DURATION = 0.3f;
    public static final float ARCANE_RAY_WIDTH = 100f;
    public static final float ARCANE_RAY_LENGTH = 1000f;
    public static final float ARCANE_RAY_ANIMATION_SPEED = 0.07f;

    // ============================================================
    // ELECTRIC FIELD CONSTANTS (in reference units)
    // ============================================================
    public static final float ELECTRIC_FIELD_DAMAGE = 30f;              // Damage per tick
    public static final float ELECTRIC_FIELD_DAMAGE_INTERVAL = 0.33f;   // Seconds between damage ticks (shorter for more frequent ticks)
    public static final float ELECTRIC_FIELD_BASE_RADIUS = 200f;        // Starting radius at level 1
    public static final float ELECTRIC_FIELD_RADIUS_PER_LEVEL = 20f;    // Radius increase per level
    public static final float ELECTRIC_FIELD_ANIMATION_SPEED = 0.1f;    // Animation frame speed (seconds)
    public static final float ELECTRIC_FIELD_VERTICAL_OFFSET = -40f;    // Vertical offset from player position (negative = up)
    public static final float ELECTRIC_FIELD_HITBOX_SCALE = 0.55f;      // Scale factor for damage radius (1.0 = same as visual)
    public static final float ELECTRIC_FIELD_STUN_DURATION = 0.25f;     // Seconds stunned per tick


    // ============================================================
    // TARGET FRAME RATE
    // ============================================================

    /**
     * Initialize the scaling system based on actual screen dimensions.
     */
    public static void initialize(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float screenHeight = metrics.heightPixels;
        float actualDiagonal = (float) Math.sqrt(screenWidth * screenWidth + screenHeight * screenHeight);
        scaleFactor = actualDiagonal / REFERENCE_DIAGONAL;

        // Screen metrics computed for scale: width=%.0f height=%.0f diagonal=%.0f scale=%.3f
    }

    /**
     * Convert a reference size to actual screen pixels.
     */
    public static float scale(float referenceSize) {
        return referenceSize * scaleFactor;
    }

    /**
     * Convert actual screen pixels back to reference units.
     */
    public static float unscale(float screenSize) {
        return screenSize / scaleFactor;
    }

    /**
     * Get the current scale factor.
     */
    public static float getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Calculate actual speed based on delta time AND screen scale.
     */
    public static float getFrameAdjustedSpeed(float referenceUnitsPerSecond, float deltaTime) {
        float scaledSpeed = scale(referenceUnitsPerSecond);
        return scaledSpeed * deltaTime;
    }

    /**
     * Calculate dynamic spawn interval based on elapsed time.
     * Returns the current spawn interval in seconds.
     */
    public static float calculateSpawnInterval(float gameTimeSeconds) {

        if (gameTimeSeconds <= 0f) return ENEMY_SPAWN_BASE_INTERVAL;
        float t = Math.min(1.0f, gameTimeSeconds / ENEMY_SPAWN_RAMP_TIME);
        // Exponential smoothing: start at base and decay toward min.
        float intervalRange = ENEMY_SPAWN_BASE_INTERVAL - ENEMY_SPAWN_MIN_INTERVAL;
        float eased = (float) Math.exp(-3.0f * t); // multiplier 3 controls steepness
        float result = ENEMY_SPAWN_MIN_INTERVAL + (intervalRange * eased);
        // Clamp to bounds for safety
        if (result < ENEMY_SPAWN_MIN_INTERVAL) result = ENEMY_SPAWN_MIN_INTERVAL;
        if (result > ENEMY_SPAWN_BASE_INTERVAL) result = ENEMY_SPAWN_BASE_INTERVAL;
        return result;
    }
}