package com.magicsurvivor.game;

/**
 * Defines the cardinal and intercardinal directions the player can be facing or moving.
 */
public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UP_LEFT,     // ️ NEW DIAGONALS
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_RIGHT,
    IDLE
}