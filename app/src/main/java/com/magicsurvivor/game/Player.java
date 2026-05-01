package com.magicsurvivor.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.magicsurvivor.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Player represents the central playable character with animated sprites.
 */
public class Player extends GameObject {

    // Stats
    private float currentHealth;
    private final int maxHealth;
    private int currentXP;
    private int currentLevel;
    private int requiredXPForNextLevel;
    private float movementSpeed;
    private boolean isAlive;
    private int currentGainedCoin=0;

    // Skills
    private final List<Skill> activeSkills;

    // Animation
    private Direction currentDirection = Direction.DOWN;
    private final Map<Direction, Bitmap[]> spriteFrames;
    private int currentFrame = 0;
    private float animationTimer = 0f;
    
    // Lazy load context and flag
    private final Context playerContext;
    private volatile boolean spritesLoaded = false;
    private volatile boolean spritesLoading = false;

    // Debug
    private final Paint hitboxPaint;

    // Damage multiplier when taking damage (1.0 = normal, <1 reduces incoming damage)
    private float damageTakenMultiplier = 1.0f;
    // Health Bar
    private final Paint healthBarBackgroundPaint;
    private final Paint healthBarForegroundPaint;
    private final Paint healthBarBorderPaint;

    public Player(float startX, float startY, int size, float speed, int initialMaxHealth, Context context) {
        super(startX, startY, size, size);
        this.maxHealth = initialMaxHealth;
        this.currentHealth = initialMaxHealth;
        this.currentXP = 0;
        this.currentLevel = 1;
        this.requiredXPForNextLevel = 10;
        this.movementSpeed = speed;
        this.activeSkills = new ArrayList<>();
        this.isAlive = true;
        this.spriteFrames = new HashMap<>();
        this.playerContext = context;

        // Initialize hitbox paint (red outline)
        this.hitboxPaint = new Paint();
        this.hitboxPaint.setColor(Color.RED);
        this.hitboxPaint.setStyle(Paint.Style.STROKE);
        this.hitboxPaint.setStrokeWidth(3);

        // Initialize health bar paints
        this.healthBarBackgroundPaint = new Paint();
        this.healthBarBackgroundPaint.setColor(Color.GRAY);
        this.healthBarBackgroundPaint.setStyle(Paint.Style.FILL);

        this.healthBarForegroundPaint = new Paint();
        this.healthBarForegroundPaint.setColor(Color.GREEN); // Default color
        this.healthBarForegroundPaint.setStyle(Paint.Style.FILL);

        this.healthBarBorderPaint = new Paint();
        this.healthBarBorderPaint.setColor(Color.BLACK);
        this.healthBarBorderPaint.setStyle(Paint.Style.STROKE);
        // Use the scaled value
        this.healthBarBorderPaint.setStrokeWidth(GameConstants.scale(GameConstants.PLAYER_HEALTHBAR_BORDER));

        // Load sprites on background thread
        new Thread(this::loadSprites).start();
    }

    /**
     * Load and scale all sprite frames for each direction.
     * Runs on background thread to avoid blocking.
     */
    private void loadSprites() {
        if (spritesLoading || spritesLoaded) return;
        spritesLoading = true;
        
        try {
            // Load and scale UP sprites
            Bitmap upFrame1 = loadAndScaleSprite(playerContext, R.drawable.upfixed1);
            Bitmap upFrame2 = loadAndScaleSprite(playerContext, R.drawable.upfixed2);
            spriteFrames.put(Direction.UP, new Bitmap[]{upFrame1, upFrame2});
            spriteFrames.put(Direction.UP_LEFT, new Bitmap[]{upFrame1, upFrame2});
            spriteFrames.put(Direction.UP_RIGHT, new Bitmap[]{upFrame1, upFrame2});

            // Load and scale DOWN sprites
            Bitmap downFrame1 = loadAndScaleSprite(playerContext, R.drawable.downfixed1);
            Bitmap downFrame2 = loadAndScaleSprite(playerContext, R.drawable.downfixed2);
            spriteFrames.put(Direction.DOWN, new Bitmap[]{downFrame1, downFrame2});
            spriteFrames.put(Direction.DOWN_LEFT, new Bitmap[]{downFrame1, downFrame2});
            spriteFrames.put(Direction.DOWN_RIGHT, new Bitmap[]{downFrame1, downFrame2});
            spriteFrames.put(Direction.IDLE, new Bitmap[]{downFrame1, downFrame1});

            // Load and scale LEFT sprites
            Bitmap leftFrame1 = loadAndScaleSprite(playerContext, R.drawable.leftfixed1);
            Bitmap leftFrame2 = loadAndScaleSprite(playerContext, R.drawable.leftfixed2);
            spriteFrames.put(Direction.LEFT, new Bitmap[]{leftFrame1, leftFrame2});

            // Load and scale RIGHT sprites
            Bitmap rightFrame1 = loadAndScaleSprite(playerContext, R.drawable.rightfixed1);
            Bitmap rightFrame2 = loadAndScaleSprite(playerContext, R.drawable.rightfixed2);
            spriteFrames.put(Direction.RIGHT, new Bitmap[]{rightFrame1, rightFrame2});

            spritesLoaded = true;
        } catch (Exception e) {
            android.util.Log.e("Player", "Error loading sprites: " + e.getMessage());
        } finally {
            spritesLoading = false;
        }
    }

    /**
     * Helper method to load and scale a single sprite bitmap.
     */
    private Bitmap loadAndScaleSprite(Context context, int resourceId) {
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resourceId);
        if (raw == null) {
            throw new RuntimeException("Failed to load sprite resource: " + resourceId);
        }
        return Bitmap.createScaledBitmap(raw, sizeWidth, sizeHeight, true);
    }

    @Override
    public void updateLogic() {
        // Required by GameObject but not used - GameEngine calls the delta time version
    }

    public void updateLogic(EntityManager entityManager, float deltaTime) {
        if (!isAlive) return;

        // Move the player (speed is in units/second, multiply by deltaTime)
        float frameSpeed = GameConstants.getFrameAdjustedSpeed(this.movementSpeed, deltaTime);
        this.positionX += this.movementVectorX * frameSpeed;
        this.positionY += this.movementVectorY * frameSpeed;

        // Update animation (only if moving)
        if (currentDirection != Direction.IDLE) {
            animationTimer += deltaTime;
            if (animationTimer >= GameConstants.PLAYER_ANIMATION_SPEED) {
                animationTimer = 0f;
                currentFrame = (currentFrame + 1) % 2;
            }
        } else {
            currentFrame = 0;
            animationTimer = 0f;
        }

        // Update all active skills
        for (Skill skill : activeSkills) {
            skill.updateLogic(entityManager, deltaTime);
        }

        // Reset movement vector each frame
        this.movementVectorX = 0;
        this.movementVectorY = 0;
    }

    @Override
    public void drawOnScreen(Canvas canvas) {
        Bitmap[] frames = spriteFrames.get(currentDirection);
        if (frames != null && frames[currentFrame] != null) {
            float left = positionX - (sizeWidth / 2f);
            float top = positionY - (sizeHeight / 2f);
            canvas.drawBitmap(frames[currentFrame], left, top, null);

//            // Draw hitbox
//            canvas.drawRect(left, top, left + sizeWidth, top + sizeHeight, hitboxPaint);
        }
    }

    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        Bitmap[] frames = spriteFrames.get(currentDirection);
        if (frames != null && frames[currentFrame] != null) {
            float left = centerX - (sizeWidth / 2f);
            float top = centerY - (sizeHeight / 2f);

            // 1. Draw the player sprite
            canvas.drawBitmap(frames[currentFrame], left, top, null);

            // --- 2. Draw the Health Bar ---
            float healthPercent = (float) currentHealth / maxHealth;

            // Update color based on health
            if (healthPercent <= 0.3f) {
                healthBarForegroundPaint.setColor(Color.RED);
            } else if (healthPercent <= 0.6f) {
                healthBarForegroundPaint.setColor(Color.YELLOW);
            } else {
                healthBarForegroundPaint.setColor(Color.GREEN);
            }

            // Define bar dimensions and position
            float barWidth = sizeWidth * 0.8f; // This is already scaled (sizeWidth is scaled)
            // Use scaled constants
            float barHeight = GameConstants.scale(GameConstants.PLAYER_HEALTHBAR_HEIGHT);
            float barOffset = GameConstants.scale(GameConstants.PLAYER_HEALTHBAR_OFFSET);

            // Calculate coordinates
            float spriteBottom = top + sizeHeight;
            float barTop = spriteBottom + barOffset;
            float barBottom = barTop + barHeight;
            float barLeft = centerX - (barWidth / 2f);
            float barRight = centerX + (barWidth / 2f);

            // Draw the background (empty part)
            canvas.drawRect(barLeft, barTop, barRight, barBottom, healthBarBackgroundPaint);

            // Draw the foreground (filled part)
            float fillWidth = barWidth * healthPercent;
            canvas.drawRect(barLeft, barTop, barLeft + fillWidth, barBottom, healthBarForegroundPaint);

            // Draw the border
            canvas.drawRect(barLeft, barTop, barRight, barBottom, healthBarBorderPaint);
            // --- End Health Bar ---


//            // 3. Draw hitbox (scaled)
//            float hitboxWidth = sizeWidth * GameConstants.PLAYER_HITBOX_SCALE_WIDTH;
//            float hitboxHeight = sizeHeight * GameConstants.PLAYER_HITBOX_SCALE_HEIGHT;
//            float hitboxLeft = centerX - (hitboxWidth / 2f);
//            float hitboxTop = centerY - (hitboxHeight / 1.3f);
//            canvas.drawRect(hitboxLeft, hitboxTop, hitboxLeft + hitboxWidth,
//                    hitboxTop + hitboxHeight, hitboxPaint);
        }
    }

    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY) {
        drawOnScreen(canvas);
    }

    public void move(float inputVectorX, float inputVectorY) {
        // Update movement vectors
        this.movementVectorX = inputVectorX;
        this.movementVectorY = inputVectorY;

        // Check for IDLE
        if (inputVectorX == 0 && inputVectorY == 0) {
            currentDirection = Direction.IDLE;
            return;
        }

        // Determine direction based on input magnitude
        float absX = Math.abs(inputVectorX);
        float absY = Math.abs(inputVectorY);

        // Diagonal movement (both axes have significant input)
        if (absX > 0.3f && absY > 0.3f) {
            if (inputVectorX > 0 && inputVectorY < 0) {
                currentDirection = Direction.UP_RIGHT;
            } else if (inputVectorX > 0 && inputVectorY > 0) {
                currentDirection = Direction.DOWN_RIGHT;
            } else if (inputVectorX < 0 && inputVectorY > 0) {
                currentDirection = Direction.DOWN_LEFT;
            } else if (inputVectorX < 0 && inputVectorY < 0) {
                currentDirection = Direction.UP_LEFT;
            }
        }
        // Cardinal movement (dominant axis)
        else if (absX > absY) {
            currentDirection = (inputVectorX > 0) ? Direction.RIGHT : Direction.LEFT;
        } else {
            currentDirection = (inputVectorY > 0) ? Direction.DOWN : Direction.UP;
        }
    }

    public int gainXP(int amount, GameEngine gameEngine) {
        this.currentXP += amount;
        int level_amount = 0;
        while (this.currentXP >= this.requiredXPForNextLevel) {
            this.currentLevel++;
            this.currentXP -= this.requiredXPForNextLevel;
            this.requiredXPForNextLevel = (int) (this.requiredXPForNextLevel * GameConstants.PLAYER_XP_MULTIPLIER);
            level_amount++;
        }

        // Queue all the level-ups at once
        for (int i = 0; i < level_amount; i++) {
            gameEngine.handleLevelUp();
        }
        return level_amount;
    }
    public void gainCoin(int amount, GameEngine gameEngine) {
        this.currentGainedCoin += amount;
        //handling adding money later
    }

    public void takeDamage(float damage) {
        float actual = damage * damageTakenMultiplier;
        this.currentHealth -= actual;
        if (this.currentHealth <= 0) {
            this.currentHealth = 0;
            this.isAlive = false;
        }
    }
    // NEW METHOD
    public void heal(float amount) {
        this.currentHealth += amount;
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }
    }

    public void addSkill(Skill skill) {
        activeSkills.add(skill);
    }

    // Getters
    public PointF getPosition() { return new PointF(positionX, positionY); }
    public float getPositionX() { return positionX; }
    public float getPositionY() { return positionY; }
    public int getCurrentLevel() { return currentLevel; }
    public Direction getCurrentDirection() { return currentDirection; }
    public int getCurrentXP() { return currentXP; }
    public int getRequiredXP() { return requiredXPForNextLevel; }
    public List<Skill> getSkills() { return activeSkills; } // Needed for upgrade
    public boolean isAlive() { return isAlive; } // NEW: Check if player is alive
    public int getCurrentGainedCoin() { return currentGainedCoin; } // NEW: Get coins earned this round

    public void setPosition(float x, float y) {
        this.positionX = x;
        this.positionY = y;
    }

    public Paint getPlayerPaint() {
        // Legacy method for compatibility
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        return paint;
    }

    /**
     * Apply health bonus from selected character
     * @param healthBonusPercentage The health bonus as a decimal (e.g., 0.15 for 15% bonus)
     */
    public void applyHealthBonus(float healthBonusPercentage) {
        // Note: Since maxHealth is final, we can't directly modify it
        // Instead, we'll heal the player by the bonus amount
        float bonusHealth = maxHealth * healthBonusPercentage;
        this.currentHealth += bonusHealth;
        if (this.currentHealth > this.maxHealth) {
            this.currentHealth = this.maxHealth;
        }
    }

    /**
     * Apply movement speed bonus from selected character
     * @param speedBonusPercentage The speed bonus as a decimal (e.g., 0.15 for 15% bonus)
     */
    public void applyMovementSpeedBonus(float speedBonusPercentage) {
        this.movementSpeed *= (1.0f + speedBonusPercentage);
    }

    /**
     * Set multiplier applied to incoming damage (e.g., 0.7f for 30% damage reduction)
     */
    public void setDamageTakenMultiplier(float mult) {
        this.damageTakenMultiplier = mult;
    }

    public float getDamageTakenMultiplier() { return this.damageTakenMultiplier; }

    /**
     * Get current movement speed (for character perks)
     */
    public float getMovementSpeed() {
        return this.movementSpeed;
    }

    /**
     * Get current max health
     */
    public int getMaxHealth() {
        return this.maxHealth;
    }

    /**
     * Get current health
     */
    public float getCurrentHealth() {
        return this.currentHealth;
    }
}