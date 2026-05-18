package com.magicsurvivor.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import android.graphics.RectF; // Import RectF
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

public class Enemy extends GameObject {

    private float hp;
    private final float xpVal;
    private final float speed;
    private boolean isDestroyed;

    // Stun state (seconds remaining)
    private float stunnedTimer = 0f;

    // Removed Paint, Added Bitmaps
    private final Bitmap[] sprites;
    protected int currentFrame = 0;
    protected float animationTimer = 0f;

    // Debug: Hitbox
    private final Paint hitboxPaint; // New Paint

    public final float dmg;
    private float attackCooldownTimer = 0f;
    private static final float ATTACK_COOLDOWN = GameConstants.ENEMY_ATTACK_COOLDOWN;

    // Updated Constructor
    public Enemy(float startX, float startY, int size, float initialHp, float movementSpeed, float xpValue, Bitmap[] sprites) {
        super(startX, startY, size, size);
        this.hp = initialHp;
        this.speed = movementSpeed;
        this.xpVal = xpValue;
        this.sprites = sprites; // Store the reference
        this.isDestroyed = false;
        this.dmg = GameConstants.ENEMY_DAMAGE;

        // Initialize Hitbox Paint (Red Outline)
        this.hitboxPaint = new Paint();
        this.hitboxPaint.setColor(Color.RED);
        this.hitboxPaint.setStyle(Paint.Style.STROKE);
        this.hitboxPaint.setStrokeWidth(3f);
    }

    // === NEW METHOD ===
    public RectF getHitBox() {
        float width = sizeWidth * GameConstants.ENEMY_HITBOX_SCALE_WIDTH;
        float height = sizeHeight * GameConstants.ENEMY_HITBOX_SCALE_HEIGHT;

        float left = positionX - (width / 2f);
        float top = positionY - (height / 2f);
        float right = positionX + (width / 2f);
        float bottom = positionY + (height / 2f);

        return new RectF(left, top, right, bottom);
    }

    @Override
    public void updateLogic(float deltaTime) {
        // Generic update if needed
    }

    // Wrapper for the specific updateLogic
    public void updateLogic(Player player, float deltaTime) {
        if (isDestroyed) return;

        // Handle stun: if stunned, decrement timer and skip movement/AI
        if (stunnedTimer > 0f) {
            stunnedTimer -= deltaTime;
            if (stunnedTimer < 0f) stunnedTimer = 0f;
            // Keep animation ticking while stunned
            animationTimer += deltaTime;
            if (animationTimer >= GameConstants.ENEMY_ANIMATION_SPEED) {
                animationTimer = 0f;
                currentFrame = (currentFrame + 1) % sprites.length;
            }
            return;
        }

        if (attackCooldownTimer > 0) attackCooldownTimer -= deltaTime;

        PointF playerPos = player.getPosition();
        float dx = playerPos.x - this.positionX;
        float dy = playerPos.y - this.positionY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            this.movementVectorX = (float) (dx / distance);
            this.movementVectorY = (float) (dy / distance);
        }

        float frameSpeed = GameConstants.getFrameAdjustedSpeed(this.speed, deltaTime);
        this.positionX += this.movementVectorX * frameSpeed;
        this.positionY += this.movementVectorY * frameSpeed;

        if(!(this instanceof TankEnemy)){
            animationTimer += deltaTime;
            if (animationTimer >= GameConstants.ENEMY_ANIMATION_SPEED) {
                animationTimer = 0f;
                currentFrame = (currentFrame + 1) % sprites.length;
            }
        }
    }

    @Override
    public void updateLogic() { }

    @Override
    public void drawOnScreen(Canvas canvas) { }

    private static final Paint STUN_TINT_PAINT;
    static {  // This runs ONE time
        STUN_TINT_PAINT = new Paint();
        STUN_TINT_PAINT.setFilterBitmap(true);
        STUN_TINT_PAINT.setAntiAlias(true);
        STUN_TINT_PAINT.setColorFilter(
                new PorterDuffColorFilter(
                        Color.argb(100, 50, 110, 200),
                        PorterDuff.Mode.SRC_ATOP
                )
        );
    }
    @Override
    public void drawOnScreen(Canvas canvas, float cameraX, float cameraY, float centerX, float centerY) {
        float drawX = positionX - cameraX + centerX;
        float drawY = positionY - cameraY + centerY;

        float left = drawX - sizeWidth / 2f;
        float top = drawY - sizeHeight / 2f;

        // 1. Draw Sprite (with optional stun tint)
        if (sprites != null && sprites.length > 0) {
            Bitmap sprite = sprites[currentFrame];
            if (sprite != null) {
                if (stunnedTimer > 0f) {
                    canvas.drawBitmap(sprite, left, top, STUN_TINT_PAINT);
                } else {
                    canvas.drawBitmap(sprite, left, top, null);
                }
            }
        }

//        // 2. Draw Hitbox (NEW)
//        float hbWidth = sizeWidth * GameConstants.ENEMY_HITBOX_SCALE_WIDTH;
//        float hbHeight = sizeHeight * GameConstants.ENEMY_HITBOX_SCALE_HEIGHT;
//
//        float hbLeft = drawX - (hbWidth / 2f);
//        float hbTop = drawY - (hbHeight / 2f);
//
//        canvas.drawRect(hbLeft, hbTop, hbLeft + hbWidth, hbTop + hbHeight, hitboxPaint);
        // If no sprites available (procedural draw), still overlay a subtle tint
        if ((sprites == null || sprites.length == 0) && stunnedTimer > 0f) {
            Paint tint = new Paint();
            tint.setColorFilter(new PorterDuffColorFilter(Color.argb(80, 50, 110, 200), PorterDuff.Mode.SRC_ATOP));
            canvas.drawRect(left, top, left + sizeWidth, top + sizeHeight, tint);
        }
    }

    /**
     * Apply a stun to this enemy for the given duration (seconds).
     */
    public void applyStun(float durationSeconds) {
        if (durationSeconds <= 0f) return;
        this.stunnedTimer = Math.max(this.stunnedTimer, durationSeconds);
    }

    public boolean isStunned() { return this.stunnedTimer > 0f; }

    public void takeDamage(float damage, EntityManager entityManager) {
        this.hp -= damage;
        if (this.hp <= 0 && !this.isDestroyed) {
            this.isDestroyed = true;
            entityManager.grantPlayerXP((int)this.xpVal);
            entityManager.notifyEnemyKilled(); // NEW: Track enemy kill
        }
    }

    public boolean canAttack() { return attackCooldownTimer <= 0; }
    public void resetAttackCooldown() { this.attackCooldownTimer = ATTACK_COOLDOWN; }
    public boolean isDestroyed() { return isDestroyed; }
    public float getDmg(){ return dmg; }
}