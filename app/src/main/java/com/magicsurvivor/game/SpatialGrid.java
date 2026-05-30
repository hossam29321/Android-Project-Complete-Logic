package com.magicsurvivor.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SpatialGrid - Divides the game world into a grid of squares.
 *
 * Instead of checking if fireball hits ALL 50 enemies (expensive),
 * we only check enemies in nearby squares (fast).
 */
public class SpatialGrid {

    // Each grid square is 300×300 world units
    // Smaller = more squares = more memory but more precise
    // Larger = fewer squares = less memory but less precise
    // 300 is good for ~50 enemies on a typical screen
    private static final float CELL_SIZE = 300f;

    // The actual grid: stores enemies in each square
    // Key = unique ID for that square (like "row 5, column 3")
    // Value = list of enemies in that square
    private final Map<Long, List<GameObject>> grid = new HashMap<>();

    /**
     * Convert world position (x, y) into grid square ID
     *
     * Example: position (450, 600) with CELL_SIZE 300:
     * - gridX = 450 / 300 = 1 (column 1)
     * - gridY = 600 / 300 = 2 (row 2)
     * - Result: unique ID for square at row 2, column 1
     *
     * This lets us say: "Which square am I in?"
     */
    private long getGridKey(float x, float y) {
        // Which grid column? (0, 1, 2, etc)
        int gridX = (int) (x / CELL_SIZE);

        // Which grid row? (0, 1, 2, etc)
        int gridY = (int) (y / CELL_SIZE);

        // Combine into one unique number
        // (This is like postal code: "Row 2, Column 3" → unique ID)
        return ((long) gridX << 32) | (gridY & 0xFFFFFFFFL);
    }

    /**
     * Clear the entire grid (call this at start of each frame)
     *
     * We rebuild it fresh each frame because enemies move around.
     * Old positions are no longer valid.
     */
    public void clear() {
        grid.clear();
    }

    /**
     * Add an enemy to the grid at its current position
     *
     * Example: Enemy at (450, 600) gets added to square (column 1, row 2)
     * Next frame: Enemy moves to (750, 300) and gets added to square (column 2, row 1)
     */
    public void add(GameObject obj) {
        // Get which square this enemy is in
        long squareID = getGridKey(obj.getPositionX(), obj.getPositionY());

        // Put this enemy into that square's list
        // computeIfAbsent = "if that square doesn't exist yet, create empty list"
        grid.computeIfAbsent(squareID, k -> new ArrayList<>()).add(obj);
    }

    /**
     * Get all enemies near a position (for collision checking)
     *
     * Returns enemies in:
     * - The center square
     * - All 8 surrounding squares (forms a 3×3 block)
     *
     * Grid layout (9 squares):
     *   +-+-+-+
     *   |1|2|3|
     *   +-+-+-+
     *   |4|X|5|  (X = center, check all 9)
     *   +-+-+-+
     *   |6|7|8|
     *   +-+-+-+
     *
     * Why 3×3? Because a projectile at edge of square 5 might hit
     * an enemy at edge of square 3 or 2. Better to be safe.
     */
    public List<GameObject> getNearby(float x, float y) {
        // Create list to store nearby enemies
        List<GameObject> nearby = new ArrayList<>();

        // Loop through 3×3 grid (center + 8 neighbors)
        for (int dx = -1; dx <= 1; dx++) {  // -1, 0, 1 (left, center, right)
            for (int dy = -1; dy <= 1; dy++) {  // -1, 0, 1 (up, center, down)
                // Calculate grid position for this neighbor
                int gridX = (int) (x / CELL_SIZE) + dx;
                int gridY = (int) (y / CELL_SIZE) + dy;

                // Get unique ID for this neighbor square
                long squareID = ((long) gridX << 32) | (gridY & 0xFFFFFFFFL);

                // Get list of enemies in that square (if it exists)
                List<GameObject> cell = grid.get(squareID);

                // If that square has enemies, add them all to our "nearby" list
                if (cell != null) {
                    nearby.addAll(cell);
                }
            }
        }

        // Return all enemies found in these 9 squares
        return nearby;
    }

    /**
     * DEBUG: Print grid info (for testing)
     * Shows how many enemies are in each square
     */
    public void printDebugInfo() {
        System.out.println("=== Spatial Grid Debug ===");
        System.out.println("Total squares with enemies: " + grid.size());
        int totalEnemies = 0;
        for (List<GameObject> enemies : grid.values()) {
            totalEnemies += enemies.size();
        }
        System.out.println("Total enemies: " + totalEnemies);
        System.out.println("======================");
    }
}
