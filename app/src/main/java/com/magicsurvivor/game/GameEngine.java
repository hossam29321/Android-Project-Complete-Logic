package com.magicsurvivor.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.magicsurvivor.R;
import com.magicsurvivor.ui.Joystick;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {

    private final EntityManager entityManager;
    private final CollisionHandler collisionHandler;
    private final Player player;
    private final Joystick joystick;
    private GameState gameState;
    private Bitmap backgroundBitmap;
    private float screenCenterX, screenCenterY;
    
    // Character System
    private CharacterManager characterManager;
    private Context gameContext;

    // Timers
    private float spawnTimer = 0f;
    private float gemSpawnTimer = 0f;
    private float mobRushTimer = 0f;

    private float healthOrbSpawnTimer = 0f;
    private float coinOrbSpawnTimer = 0f;
    private float gameTimer = 0f; // NEW: Track total game time

    // Game Over Stats
    private int enemiesKilled = 0; // NEW: Track enemies killed
    private int moneyCollected = 0; // NEW: Track money collected

    // Level Up Queue
    private int pendingLevelUps = 0; // NEW: Track how many level-up menus to show

    // Damage Flash
    private float damageFlashTimer = 0f;
    private Paint damageOverlayPaint;

    // UI Paints
    private Paint xpBarBackgroundPaint;
    private Paint xpBarFillPaint;
    private Paint xpTextPaint;
    private Paint timerTextPaint; // NEW: Timer display
    private Paint menuBackgroundPaint;
    private Paint menuButtonPaint;
    private Paint menuTextPaint;
    private Paint menuDetailPaint;
    private Paint mobRushTextPaint;
    private Paint mobRushOverlayPaint;
    private Paint debugTextPaint;
    private boolean showDebugHUD = true;


    // Sprites
    private volatile Bitmap[] enemySprites;
    private volatile Bitmap[] enemyTankSprites;
    private volatile Bitmap[] raySprites;
    private volatile Bitmap[] electricFieldSprites; // NEW
    
    // Sprite loading flags
    private volatile boolean spritesLoading = false;
    private volatile boolean spritesLoaded = false;

    // UI Areas
    private RectF upgradeButtonRect;
    private RectF unlockButtonRect;
    private RectF unlockElectricFieldButtonRect; // NEW
    private RectF pauseButtonRect; // NEW: Pause button
    private RectF resumeButtonRect; // NEW: Resume button
    private RectF quitButtonRect; // NEW: Resume button
    private RectF playAgainButtonRect; // NEW: Play Again button
    private RectF mainMenuButtonRect; // NEW: Main Menu button
    private RectF debugToggleRect; // Toggle for debug HUD

    // Pause Button Paint
    private Paint pauseButtonPaint;
    private Paint pauseButtonTextPaint;
    
    // Paint initialization flag
    private volatile boolean paintsInitialized = false;

    public enum GameState {
        RUNNING, PAUSED, OVER, LEVEL_UP, MOB_RUSH, VICTORY
    }

    // NEW: Game Over Listener Interface
    public interface GameOverListener {
        void onPlayAgain();
        void onReturnToMenu();
    }

    private GameOverListener gameOverListener;

    public GameEngine(Joystick joystick, Context context) {
        this.entityManager = new EntityManager();
        this.joystick = joystick;
        this.gameContext = context;

        this.entityManager.setGameEngine(this);

        float playerSize = GameConstants.scale(GameConstants.PLAYER_SIZE);
        float playerSpeed = GameConstants.PLAYER_SPEED;
        this.player = new Player(500, 500, (int)playerSize, playerSpeed,
                GameConstants.PLAYER_MAX_HEALTH, context);
        this.entityManager.setPlayer(this.player);

        this.collisionHandler = new CollisionHandler(this.entityManager, this.player, this);
        this.gameState = GameState.RUNNING;
        
        // Initialize Character Manager and apply perks
        this.characterManager = new CharacterManager();
        loadCharacterSelection();
        characterManager.applyCharacterPerks(this.player);

        // Load persisted debug HUD preference
        try {
            SharedPreferences prefs = gameContext.getSharedPreferences("GameData", Context.MODE_PRIVATE);
            this.showDebugHUD = prefs.getBoolean("debug_show_hud", true);
        } catch (Exception ignored) { }

        FireballSkill fireball = new FireballSkill(player, context);
        fireball.setCharacterManager(characterManager);

        player.addSkill(fireball);
        
        // Initialize paints lazily on first draw to avoid blocking constructor
    }
    
    /**
     * Load previously selected character from SharedPreferences
     */
    public void loadCharacterUnlocksFromFirebase(String userId) {
          if (userId == null || userId.isEmpty()) {
              return; // Early exit if userId is invalid
          }
        
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference()
            .child(userId).child("characters")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                    try {
                        Character[] characters = characterManager.getAllCharacters();
                        if (dataSnapshot.exists()) {
                            for (Character character : characters) {
                                String charKey = character.getType().name();
                                if (dataSnapshot.hasChild(charKey)) {
                                    Boolean unlocked = dataSnapshot.child(charKey).child("unlocked").getValue(Boolean.class);
                                    if (unlocked != null && unlocked) {
                                        character.setUnlocked(true); // Set character as unlocked
                                    }
                                }
                            }
                        } else {
                            android.util.Log.w("GameEngine", "No characters data in Firebase");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("GameEngine", "Error loading character unlocks", e);
                    }
                    // After Firebase unlocks are applied, re-attempt to apply the saved character selection
                        // Firebase unlocks applied — reapplying saved selection
                    applySavedSelection();
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                    android.util.Log.e("GameEngine", "Firebase error loading character unlocks: " + databaseError.getMessage());
                }
            });
    }
    
    /**
     * Load previously selected character from SharedPreferences and load unlock states
     */
    private void loadCharacterSelection() {
        SharedPreferences prefs = gameContext.getSharedPreferences("GameData", Context.MODE_PRIVATE);
        // Load character unlock states from SharedPreferences first
        Character[] characters = characterManager.getAllCharacters();
        for (Character character : characters) {
            String unlockKey = "character_unlocked_" + character.getType().name();
            boolean isUnlocked = prefs.getBoolean(unlockKey, character.getType().isDefault);
            character.setUnlocked(isUnlocked);
        }

        // Now attempt to restore the previously selected character (after unlock states are applied)
        applySavedSelection();
    }

    /**
     * Try to read the saved selected character from SharedPreferences and apply it to the CharacterManager.
     * This method is safe to call multiple times (e.g. initially and again after Firebase unlocks load).
     */
    private void applySavedSelection() {
        SharedPreferences prefs = gameContext.getSharedPreferences("GameData", Context.MODE_PRIVATE);
        String savedChar = prefs.getString("selectedCharacter", "MAGE");
            // Saved selectedCharacter: handled
        try {
            Character.CharacterType charType = Character.CharacterType.valueOf(savedChar);
            boolean ok = characterManager.selectCharacter(charType);
                // selection result handled
            if (ok) {
                // Apply perks to the currently running player so HUD/skills reflect the selection
                try {
                    characterManager.applyCharacterPerks(GameEngine.this.player);
                    // Ensure character-specific skills (like Electric Field) are present for this selection
                    try {
                        ensureCharacterSkillsApplied();
                    } catch (Exception e) {
                        android.util.Log.e("GameEngine", "Error ensuring character skills after selection", e);
                    }
                } catch (Exception e) {
                    android.util.Log.e("GameEngine", "Error applying perks after selection", e);
                }
            } else {
                // Fallback to default if saved selection cannot be applied
                    characterManager.selectCharacter(Character.CharacterType.MAGE); // Fallback to default character
                try {
                    characterManager.applyCharacterPerks(GameEngine.this.player);
                    try {
                        ensureCharacterSkillsApplied();
                    } catch (Exception e) {
                        android.util.Log.e("GameEngine", "Error ensuring character skills after fallback selection", e);
                    }
                } catch (Exception e) {
                    android.util.Log.e("GameEngine", "Error applying perks after fallback selection", e);
                }
            }
        } catch (IllegalArgumentException e) {
                characterManager.selectCharacter(Character.CharacterType.MAGE);
            try {
                characterManager.applyCharacterPerks(GameEngine.this.player);
                try {
                    ensureCharacterSkillsApplied();
                } catch (Exception ex) {
                    android.util.Log.e("GameEngine", "Error applying perks after invalid saved character fallback", ex);
                }
            } catch (Exception ex) {
                android.util.Log.e("GameEngine", "Error applying perks after invalid saved character fallback", ex);
            }
        }
    }

    /**
     * Ensure character-specific skills are present on the player for the currently selected character.
     * Adds ElectricFieldSkill or ArcaneRaySkill if the selected character benefits from them and
     * the player doesn't already have them. Safe to call multiple times.
     */
    private void ensureCharacterSkillsApplied() {
        if (characterManager == null || characterManager.getSelectedCharacter() == null) return;
        Character sel = characterManager.getSelectedCharacter();

        // Electric Field
        if (sel.getElectricFieldBonus() > 0f) {
            Skill existing = findSkill(ElectricFieldSkill.class);
            if (existing == null) {
                    if (electricFieldSprites != null) {
                    ElectricFieldSkill newSkill = new ElectricFieldSkill(player, electricFieldSprites);
                    newSkill.setCharacterManager(characterManager);
                    player.addSkill(newSkill);
                } else {
                    // electricFieldSprites not loaded yet — will add skill later
                }
            }
        }

        // Arcane Ray
        if (sel.getArcaneRayBonus() > 0f) {
            Skill existing = findSkill(ArcaneRaySkill.class);
            if (existing == null) {
                if (raySprites != null) {
                    ArcaneRaySkill newSkill = new ArcaneRaySkill(player, raySprites);
                    newSkill.setCharacterManager(characterManager);
                    player.addSkill(newSkill);
                } else {
                    // raySprites not loaded yet — will add skill later
                }
            }
        }
    }
    
    public void setBackground(int picId,Context context){
        Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), picId);
        this.backgroundBitmap = scaleBackgroundBitmap(originalBitmap);
    }

    public void setBackgroundFromUri(android.net.Uri uri, Context context) {
        try {
            // Decode with inSampleSize to reduce memory usage while preserving quality
            java.io.InputStream inputStream = context.getContentResolver().openInputStream(uri);
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            // Allow up to 3072x3072 for photos (higher quality preservation)
            int sampleSize = calculateInSampleSize(options, 3072, 3072);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;

            inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            // Use same scaling as resource drawables for consistency
            this.backgroundBitmap = scaleBackgroundBitmap(originalBitmap);
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            android.util.Log.e("GameEngine", "Error loading background from URI", e);
        }
    }

    private static int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap scaleBackgroundBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        // Normalize all background images to a consistent size while maintaining aspect ratio
        // This ensures consistent sizing for gallery, camera, and drawable images
        final int MAX_SIZE = 4096; // Maximum dimension for any side
        
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        
        // Calculate scaling based on the largest dimension
        float scale = Math.min((float) MAX_SIZE / originalWidth, (float) MAX_SIZE / originalHeight);
        
        // If image is already smaller than max size, keep it as is
        if (scale >= 1.0f) {
            return bitmap;
        }
        
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void paints(){
        // UI Paints
        xpBarBackgroundPaint = new Paint();
        xpBarBackgroundPaint.setColor(Color.DKGRAY);

        xpBarFillPaint = new Paint();
        xpBarFillPaint.setColor(Color.BLUE);

        xpTextPaint = new Paint();
        xpTextPaint.setColor(Color.WHITE);
        xpTextPaint.setTextSize(40f);
        xpTextPaint.setTextAlign(Paint.Align.CENTER);

        // NEW: Timer Text Paint
        timerTextPaint = new Paint();
        timerTextPaint.setColor(Color.WHITE);
        timerTextPaint.setTextSize(GameConstants.scale(GameConstants.UI_TIMER_TEXT_SIZE));
        timerTextPaint.setTextAlign(Paint.Align.CENTER);
        timerTextPaint.setAntiAlias(true);

        menuBackgroundPaint = new Paint();
        menuBackgroundPaint.setColor(Color.argb(200, 0, 0, 0));

        menuButtonPaint = new Paint();
        menuButtonPaint.setColor(Color.argb(255, 40, 40, 40));

        menuTextPaint = new Paint();
        menuTextPaint.setColor(Color.LTGRAY);
        menuTextPaint.setTextSize(60f);
        menuTextPaint.setTextAlign(Paint.Align.CENTER);

        menuDetailPaint = new Paint();
        menuDetailPaint.setColor(Color.LTGRAY);
        menuDetailPaint.setTextSize(45f);
        menuDetailPaint.setTextAlign(Paint.Align.CENTER);

        damageOverlayPaint = new Paint();
        damageOverlayPaint.setColor(Color.RED);
        damageOverlayPaint.setAlpha(25);

        // NEW: Pause Button Paints
        pauseButtonPaint = new Paint();
        pauseButtonPaint.setColor(Color.argb(200, 50, 50, 50));

        pauseButtonTextPaint = new Paint();
        pauseButtonTextPaint.setColor(Color.WHITE);
        pauseButtonTextPaint.setTextSize(50f);
        pauseButtonTextPaint.setTextAlign(Paint.Align.CENTER);
        pauseButtonTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // NEW NEW: MOB RUSH PAINTS

        mobRushOverlayPaint = new Paint();
        mobRushOverlayPaint.setColor(Color.argb(25,255,50, 50));

        mobRushTextPaint= new Paint();
        mobRushTextPaint.setColor(Color.argb(150,255,50, 50));
        mobRushTextPaint.setTextSize(240f);
        mobRushTextPaint.setTextAlign(Paint.Align.CENTER);

        debugTextPaint = new Paint();
        debugTextPaint.setColor(Color.CYAN);
        debugTextPaint.setTextSize(GameConstants.scale(18f));
        debugTextPaint.setTextAlign(Paint.Align.LEFT);
        debugTextPaint.setAntiAlias(true);
    }

    private Bitmap loadAndScaleSprite(Context context, int resId, int size) {
        Bitmap raw = BitmapFactory.decodeResource(context.getResources(), resId);
        return Bitmap.createScaledBitmap(raw, size, size, true);
    }

    public void updateLogic(float deltaTime) {
        if (gameState == GameState.RUNNING||gameState==GameState.MOB_RUSH) {
            // NEW: Check if player is dead
            if (!player.isAlive()) {
                triggerGameOver();
                return;
            }
            if(mobRushTimer>GameConstants.MOB_RUSH_START&&gameState!=GameState.MOB_RUSH){
                setGameState(GameState.MOB_RUSH);
                // start fresh spawn cadence for mob rush to avoid immediate burst
                spawnTimer = 0f;
            }

            // NEW: Update game timer
            gameTimer += deltaTime;
            mobRushTimer+=deltaTime;

            // WIN CONDITION: 15 minutes (900 seconds)
            if (gameTimer >= 60*15) {
                triggerVictory();
                return;
            }

            // NEW: Update spawn interval based on game time
            GameConstants.spawnInterval = GameConstants.calculateSpawnInterval(gameTimer);

            player.move(joystick.getMovementVectorX(), joystick.getMovementVectorY());
            player.updateLogic(entityManager, deltaTime);
            entityManager.updateAll(player, deltaTime);
            collisionHandler.checkAllHits();
            entityManager.cleanUp();

            if(gameState==GameState.RUNNING){
                spawnTimer += deltaTime;
                if (spawnTimer >= GameConstants.spawnInterval) {
                    spawnTimer = 0f;
                    int rnd = (Math.random() < 0.1) ? 1 : 0;
                    spawnEnemyAroundPlayer(rnd);
                }
            }
            else if(gameState==GameState.MOB_RUSH){
                // If mob rush duration ended, return to running state
                if (mobRushTimer >= GameConstants.MOB_RUSH_END) {
                    mobRushTimer = 0f;
                    setGameState(GameState.RUNNING);
                    // reset spawn cadence after mob rush
                    spawnTimer = 0f;
                } else {
                    spawnTimer += deltaTime;
                    if (spawnTimer >= 0.1f) { // high-frequency spawns during mob rush
                        spawnTimer = 0f;
                        int rnd = (Math.random() < 0.1) ? 1 : 0;
                        spawnEnemyAroundPlayer(rnd);
                    }
                }
            }

            gemSpawnTimer += deltaTime;
            if (gemSpawnTimer >= GameConstants.GEM_SPAWN_INTERVAL) {
                gemSpawnTimer = 0f;
                spawnGemAroundPlayer();
            }

            healthOrbSpawnTimer += deltaTime;
            if (healthOrbSpawnTimer >= GameConstants.HEALTH_ORB_SPAWN_INTERVAL) {
                healthOrbSpawnTimer = 0f;
                spawnHealthOrbAroundPlayer();
            }
            //------------------
            coinOrbSpawnTimer+=deltaTime;
            if (coinOrbSpawnTimer >= GameConstants.COIN_MIN_COOLDOWN) {
                coinOrbSpawnTimer = 0f;
                if(Math.random()<=0.01)//1%chance to spawn every 15 seconds
                    spawnCoinOrbAroundPlayer();
            }

            if (damageFlashTimer > 0) {
                damageFlashTimer -= deltaTime;
            }
        }
    }

    public void drawScreen(Canvas canvas,int currentFPS) {
        // Lazy initialize paints on first draw
        if (!paintsInitialized) {
            paints();
            paintsInitialized = true;
        }
        
        float offsetX = player.getPositionX();
        float offsetY = player.getPositionY();

        drawBackground(canvas, offsetX, offsetY);
        entityManager.drawAll(canvas, offsetX, offsetY, screenCenterX, screenCenterY);
        drawPlayerCentered(canvas);
        entityManager.drawSkills(canvas, offsetX, offsetY, screenCenterX, screenCenterY);

        drawHUD(canvas,currentFPS);

        if (damageFlashTimer > 0) {
            canvas.drawRect(0, 0, screenCenterX * 2, screenCenterY * 2, damageOverlayPaint);
        }

        if (gameState == GameState.RUNNING||gameState==GameState.MOB_RUSH) {
            drawJoystick(canvas);
        }

        if (gameState == GameState.LEVEL_UP) {
            drawLevelUpMenu(canvas);
        }

        // NEW: Draw Pause Menu
        if (gameState == GameState.PAUSED) {
            drawPauseMenu(canvas);
        }

        // NEW: Draw Game Over Menu
        if (gameState == GameState.OVER) {
            drawGameOverMenu(canvas);
        }

        // NEW: Draw Victory Menu
        if (gameState == GameState.VICTORY) {
            drawVictoryMenu(canvas);
        }

        //NEW: Draw mob rush
        if (gameState == GameState.MOB_RUSH) {
            drawMobRush(canvas);
        }
    }

    public void triggerDamageFlash() {
        this.damageFlashTimer = GameConstants.DAMAGE_FLASH_DURATION;
    }

    private void spawnEnemyAroundPlayer(int rnd) {
        float playerX = player.getPositionX();
        float playerY = player.getPositionY();
        double angle = Math.random() * 2 * Math.PI;
        float minDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MIN_DISTANCE);
        float maxDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MAX_DISTANCE);
        float distance = (float)(minDist + (Math.random() * (maxDist - minDist)));
        float spawnX = playerX + (float)(distance * Math.cos(angle));
        float spawnY = playerY + (float)(distance * Math.sin(angle));

        if(rnd==0){//enemy
            float enemySize = GameConstants.scale(GameConstants.ENEMY_SIZE);
            float enemySpeed = GameConstants.ENEMY_SPEED;

            Enemy newEnemy = new Enemy(spawnX, spawnY, (int)enemySize, GameConstants.ENEMY_HEALTH, enemySpeed, GameConstants.ENEMY_XP_VALUE, enemySprites);
            entityManager.addEnemy(newEnemy);
        }
        else{//tank

            TankEnemy newTank = new TankEnemy(spawnX, spawnY, enemyTankSprites);
            entityManager.addEnemy(newTank);
        }

    }

    private void spawnGemAroundPlayer() {
        float playerX = player.getPositionX();
        float playerY = player.getPositionY();
        double angle = Math.random() * 2 * Math.PI;
        float minDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MIN_DISTANCE) * 0.8f;
        float maxDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MAX_DISTANCE) * 0.8f;
        float distance = (float)(minDist + (Math.random() * (maxDist - minDist)));
        float spawnX = playerX + (float)(distance * Math.cos(angle));
        float spawnY = playerY + (float)(distance * Math.sin(angle));
        ExperienceGem newGem = new ExperienceGem(spawnX, spawnY, GameConstants.GEM_XP_VALUE);
        entityManager.addGem(newGem);
    }

    private void spawnHealthOrbAroundPlayer() {
        float playerX = player.getPositionX();
        float playerY = player.getPositionY();
        double angle = Math.random() * 2 * Math.PI;
        float minDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MIN_DISTANCE) * 0.8f;
        float maxDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MAX_DISTANCE) * 0.8f;
        float distance = (float)(minDist + (Math.random() * (maxDist - minDist)));
        float spawnX = playerX + (float)(distance * Math.cos(angle));
        float spawnY = playerY + (float)(distance * Math.sin(angle));
        HealthOrb newOrb = new HealthOrb(spawnX, spawnY, GameConstants.HEALTH_ORB_HEAL_AMOUNT);
        entityManager.addHealthOrb(newOrb);
    }
    private void spawnCoinOrbAroundPlayer() {
        float playerX = player.getPositionX();
        float playerY = player.getPositionY();
        double angle = Math.random() * 2 * Math.PI;
        float minDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MIN_DISTANCE) * 0.8f;
        float maxDist = GameConstants.scale(GameConstants.ENEMY_SPAWN_MAX_DISTANCE) * 0.8f;
        float distance = (float)(minDist + (Math.random() * (maxDist - minDist)));
        float spawnX = playerX + (float)(distance * Math.cos(angle));
        float spawnY = playerY + (float)(distance * Math.sin(angle));
        CoinOrb newOrb = new CoinOrb(spawnX, spawnY);
        entityManager.addCoinOrb(newOrb);

    }

    private void drawHUD(Canvas canvas,int currentFPS) {
        float screenWidth = screenCenterX * 2;
        float barHeight = GameConstants.scale(GameConstants.UI_XP_BAR_HEIGHT);
        float margin = GameConstants.scale(GameConstants.UI_MARGIN);

        // Draw XP Bar
        canvas.drawRect(margin, margin, screenWidth - margin, margin + barHeight, xpBarBackgroundPaint);
        float xpRatio = (float) player.getCurrentXP() / player.getRequiredXP();
        float fillWidth = (screenWidth - (2 * margin)) * xpRatio;
        canvas.drawRect(margin, margin, margin + fillWidth, margin + barHeight, xpBarFillPaint);
        canvas.drawText("Lvl " + player.getCurrentLevel(), screenCenterX, margin + barHeight + 40, xpTextPaint);

        // NEW: Draw Timer Below XP Bar
        float timerY = margin + barHeight + 40 + GameConstants.scale(GameConstants.UI_TIMER_TOP_MARGIN);
        String timeText = formatTime(gameTimer);
        canvas.drawText(timeText, screenCenterX, timerY + 50, timerTextPaint);

        // DEBUG: Draw selected character buffs and special effect (toggleable)
        if (showDebugHUD && characterManager != null && characterManager.getSelectedCharacter() != null) {
            Character selected = characterManager.getSelectedCharacter();
            float debugX = margin + 10f;
            float debugY = timerY + 90f;

            String charLine = "Char: " + selected.getName();
            canvas.drawText(charLine, debugX, debugY, debugTextPaint);

            String buffs = String.format(Locale.US, "Buffs H:%+d%% M:%+d%% D:%+d%% CD:%+d%%",
                    Math.round(selected.getHealthBonus() * 100f),
                    Math.round(selected.getMovementSpeedBonus() * 100f),
                    Math.round(selected.getDamageBonus() * 100f),
                    Math.round(-selected.getCooldownReduction() * 100f)
            );
            canvas.drawText(buffs, debugX, debugY + GameConstants.scale(22f), debugTextPaint);

            String special = "Special: " + selected.getSpecialPerk();
            canvas.drawText(special, debugX, debugY + GameConstants.scale(44f), debugTextPaint);

            // Active skill effects and cooldowns
            List<String> skillStatus = new ArrayList<>();
            for (Skill s : player.getSkills()) {
                if (s instanceof ElectricFieldSkill) {
                    ElectricFieldSkill efs = (ElectricFieldSkill) s;
                    if (efs.isFieldActive()) {
                        skillStatus.add(String.format(Locale.US, "%s: ACTIVE (dmg=%.0f)", s.name, efs.getActiveFieldDamage()));
                    } else {
                        skillStatus.add(String.format(Locale.US, "%s: CD=%.1fs", s.name, s.getCurrentCooldown()));
                    }
                } else {
                    float cd = s.getCurrentCooldown();
                    if (cd <= 0.001f) {
                        skillStatus.add(String.format(Locale.US, "%s: READY", s.name));
                    } else {
                        skillStatus.add(String.format(Locale.US, "%s: CD=%.1fs", s.name, cd));
                    }
                }
            }

            if (skillStatus.isEmpty()) {
                canvas.drawText("Active: None", debugX, debugY + GameConstants.scale(66f), debugTextPaint);
            } else {
                float lineOffset = 66f;
                for (int i = 0; i < skillStatus.size(); i++) {
                    canvas.drawText(skillStatus.get(i), debugX, debugY + GameConstants.scale(lineOffset + (i * 18f)), debugTextPaint);
                }
            }
            // Show count of currently stunned enemies for quick verification
            int stunnedCount = 0;
            for (GameObject go : entityManager.getEnemyList()) {
                if (go instanceof Enemy) {
                    if (((Enemy) go).isStunned()) stunnedCount++;
                }
            }
            canvas.drawText("Stunned enemies: " + stunnedCount, debugX, debugY + GameConstants.scale(120f), debugTextPaint);
            // Show total stuns applied by electric fields (debug counter)
            canvas.drawText("Total field stuns: " + ElectricField.totalStunsApplied, debugX, debugY + GameConstants.scale(138f), debugTextPaint);

            // Per-enemy diagnostics (max 6) to help debug collision and cooldown
            int maxShow = 6;
            int idx = 0;
            float diagY = debugY + GameConstants.scale(160f);
            for (GameObject go : entityManager.getEnemyList()) {
                if (idx >= maxShow) break;
                if (!(go instanceof Enemy)) continue;
                Enemy en = (Enemy) go;
                boolean inField = false;
                boolean canHit = false;
                try {
                    inField = (new ElectricField(player, 0f, new android.graphics.Bitmap[1], 1)).checkCollision(en);
                } catch (Exception ignored) { }
                try { canHit = en != null && !en.isDestroyed() && !((java.util.Map) new java.util.HashMap()).containsKey(en); } catch (Exception ignored) {}
                String tag = String.format("E%d: in=%s stunned=%s", idx+1, inField?"Y":"N", en.isStunned()?"Y":"N");
                canvas.drawText(tag, debugX, diagY + GameConstants.scale(idx * 16f), debugTextPaint);
                idx++;
            }
            //Show Current actual FPS
            float diagY2 = debugY + GameConstants.scale(160f);
            canvas.drawText("FPS: " + currentFPS, debugX, diagY2 + GameConstants.scale(idx * 16f), debugTextPaint);
        }

        // NEW: Draw Pause Button (Top-Right Corner) - Only when RUNNING
        if (gameState == GameState.RUNNING||gameState==GameState.MOB_RUSH) {
            float btnSize = 60f;
            float btnMargin = 15f;
            float btnRight = screenWidth - btnMargin;
            float btnTop = margin;
            pauseButtonRect = new RectF(btnRight - btnSize, btnTop, btnRight, btnTop + btnSize);
            canvas.drawRect(pauseButtonRect, pauseButtonPaint);
            canvas.drawText("II", pauseButtonRect.centerX(), pauseButtonRect.centerY() + 20, pauseButtonTextPaint);
        }
    }

    // NEW: Format seconds into MM:SS
    private String formatTime(float seconds) {
        int totalSeconds = (int) seconds;
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, secs);
    }

    private void drawLevelUpMenu(Canvas canvas) {
        canvas.drawRect(0, 0, screenCenterX * 2, screenCenterY * 2, menuBackgroundPaint);

        float menuW = GameConstants.scale(GameConstants.UI_MENU_WIDTH);
        float btnH = GameConstants.scale(GameConstants.UI_BUTTON_HEIGHT) * 1.2f; // Slightly smaller
        float spacing = 40f; // Tighter spacing

        float left = screenCenterX - (menuW / 2f);

        // --- BUTTON 1: Upgrade Fireball (Top) ---
        float top1 = screenCenterY - (btnH * 1.5f) - spacing;
        upgradeButtonRect = new RectF(left, top1, left + menuW, top1 + btnH);
        
        Skill fireball = player.getSkills().get(0);
        if (fireball.getLevel() >= 8) {
            canvas.drawRect(upgradeButtonRect, menuButtonPaint);
            drawSkillStats(canvas, fireball, "Upgrade Fireball", upgradeButtonRect.centerY());
            upgradeButtonRect = null;
        } else {
            canvas.drawRect(upgradeButtonRect, menuButtonPaint);
            drawSkillStats(canvas, fireball, "Upgrade Fireball", upgradeButtonRect.centerY());
        }

        // --- BUTTON 2: Arcane Ray (Middle) ---
        float top2 = screenCenterY - (btnH / 2f);
        unlockButtonRect = new RectF(left, top2, left + menuW, top2 + btnH);
        canvas.drawRect(unlockButtonRect, menuButtonPaint);

        Skill arcaneRay = findSkill(ArcaneRaySkill.class);

        if (arcaneRay != null) {
            if (arcaneRay.getLevel() >= 8) {
                drawSkillStats(canvas, arcaneRay, "Upgrade Arcane Ray", unlockButtonRect.centerY());
                unlockButtonRect = null;
            } else {
                drawSkillStats(canvas, arcaneRay, "Upgrade Arcane Ray", unlockButtonRect.centerY());
            }
        } else {
            canvas.drawText("UNLOCK: Arcane Ray", screenCenterX, unlockButtonRect.centerY() - 20, menuTextPaint);
            canvas.drawText("Fires a piercing beam.", screenCenterX, unlockButtonRect.centerY() + 40, menuDetailPaint);
        }

        // --- BUTTON 3: Electric Field (Bottom) ---
        float top3 = screenCenterY + (btnH / 2f) + spacing;
        unlockElectricFieldButtonRect = new RectF(left, top3, left + menuW, top3 + btnH);
        canvas.drawRect(unlockElectricFieldButtonRect, menuButtonPaint);

        Skill electricField = findSkill(ElectricFieldSkill.class);

        if (electricField != null) {
            if (electricField.getLevel() >= 8) {
                drawSkillStats(canvas, electricField, "Upgrade Electric Field", unlockElectricFieldButtonRect.centerY());
                unlockElectricFieldButtonRect = null;
            } else {
                drawSkillStats(canvas, electricField, "Upgrade Electric Field", unlockElectricFieldButtonRect.centerY());
            }
        } else {
            canvas.drawText("UNLOCK: Electric Field", screenCenterX, unlockElectricFieldButtonRect.centerY() - 20, menuTextPaint);
            canvas.drawText("Shocks nearby enemies.", screenCenterX, unlockElectricFieldButtonRect.centerY() + 40, menuDetailPaint);
        }
    }

    // NEW: Draw Pause Menu
    private void drawPauseMenu(Canvas canvas) {
        canvas.drawRect(0, 0, screenCenterX * 2, screenCenterY * 2, menuBackgroundPaint);

        // Title
        Paint pauseTitlePaint = new Paint();
        pauseTitlePaint.setColor(Color.WHITE);
        pauseTitlePaint.setTextSize(120f);
        pauseTitlePaint.setTextAlign(Paint.Align.CENTER);
        pauseTitlePaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        canvas.drawText("PAUSED", screenCenterX, screenCenterY - 150f, pauseTitlePaint);

        // Resume Button
        float btnW = GameConstants.scale(GameConstants.UI_MENU_WIDTH) * 0.8f;
        float btnH = GameConstants.scale(GameConstants.UI_BUTTON_HEIGHT);
        float left = screenCenterX - (btnW / 2f);
        float top = screenCenterY - (btnH / 2f);
        resumeButtonRect = new RectF(left, top, left + btnW, top + btnH);
        canvas.drawRect(resumeButtonRect, menuButtonPaint);
        canvas.drawText("RESUME", screenCenterX, resumeButtonRect.centerY() + 25f, menuTextPaint);

        float top2=top+btnH+20;
        // Resume Button
        quitButtonRect = new RectF(left, top2, left + btnW, top2 + btnH);
        canvas.drawRect(quitButtonRect, menuButtonPaint);
        canvas.drawText("QUIT", screenCenterX, quitButtonRect.centerY() + 25f, menuTextPaint);
        
        // Debug Toggle Button
        float top3 = top2 + btnH + 20f;
        debugToggleRect = new RectF(left, top3, left + btnW, top3 + btnH);
        canvas.drawRect(debugToggleRect, menuButtonPaint);
        String dbgLabel = showDebugHUD ? "Debug HUD: ON" : "Debug HUD: OFF";
        canvas.drawText(dbgLabel, screenCenterX, debugToggleRect.centerY() + 20f, menuDetailPaint);
    }

    // NEW: Draw Game Over Menu with stats
    private void drawGameOverMenu(Canvas canvas) {
        // Red overlay
        Paint redOverlayPaint = new Paint();
        redOverlayPaint.setColor(Color.argb(180, 139, 0, 0)); // Dark red overlay
        canvas.drawRect(0, 0, screenCenterX * 2, screenCenterY * 2, redOverlayPaint);

        // Title Paint
        Paint gameOverTitlePaint = new Paint();
        gameOverTitlePaint.setColor(Color.WHITE);
        gameOverTitlePaint.setTextSize(140f);
        gameOverTitlePaint.setTextAlign(Paint.Align.CENTER);
        gameOverTitlePaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // Stats Paint
        Paint statsPaint = new Paint();
        statsPaint.setColor(Color.WHITE);
        statsPaint.setTextSize(60f);
        statsPaint.setTextAlign(Paint.Align.CENTER);

        // Draw "YOU DIED" title
        canvas.drawText("YOU DIED", screenCenterX, screenCenterY - 250f, gameOverTitlePaint);

        // Draw stats
        float statsStartY = screenCenterY - 80f;
        float statsSpacing = 80f;

        String timeText = String.format("Time: %s", formatTime(gameTimer));
        String moneyText = String.format("Money: $%d", moneyCollected);
        String killsText = String.format("Enemies: %d", enemiesKilled);

        canvas.drawText(timeText, screenCenterX, statsStartY, statsPaint);
        canvas.drawText(moneyText, screenCenterX, statsStartY + statsSpacing, statsPaint);
        canvas.drawText(killsText, screenCenterX, statsStartY + (statsSpacing * 2), statsPaint);

        // Draw buttons
        float btnW = GameConstants.scale(GameConstants.UI_MENU_WIDTH) * 0.7f;
        float btnH = GameConstants.scale(GameConstants.UI_BUTTON_HEIGHT) * 0.9f;
        float btnLeft = screenCenterX - (btnW / 2f);
        float spacing = 20f;

        // Play Again Button
        float top1 = statsStartY + (statsSpacing * 3.2f);
        playAgainButtonRect = new RectF(btnLeft, top1, btnLeft + btnW, top1 + btnH);
        canvas.drawRect(playAgainButtonRect, menuButtonPaint);
        canvas.drawText("PLAY AGAIN", screenCenterX, playAgainButtonRect.centerY() + 25f, menuTextPaint);

        // Main Menu Button
        float top2 = top1 + btnH + spacing;
        mainMenuButtonRect = new RectF(btnLeft, top2, btnLeft + btnW, top2 + btnH);
        canvas.drawRect(mainMenuButtonRect, menuButtonPaint);
        canvas.drawText("MAIN MENU", screenCenterX, mainMenuButtonRect.centerY() + 25f, menuTextPaint);
    }
    
    // NEW: Draw Victory Menu with stats and points
    private void drawVictoryMenu(Canvas canvas) {
        // Green overlay
        Paint greenOverlay = new Paint();
        greenOverlay.setColor(Color.argb(200, 34, 139, 34)); // Forest green overlay
        canvas.drawRect(0, 0, screenCenterX * 2, screenCenterY * 2, greenOverlay);

        // Title Paint
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(140f);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // Stats Paint
        Paint statsPaint = new Paint();
        statsPaint.setColor(Color.WHITE);
        statsPaint.setTextSize(60f);
        statsPaint.setTextAlign(Paint.Align.CENTER);

        // Draw "VICTORY!" title
        canvas.drawText("VICTORY!", screenCenterX, screenCenterY - 250f, titlePaint);

        // Draw stats
        float statsStartY = screenCenterY - 80f;
        float statsSpacing = 80f;

        String timeText = String.format("Time: %s", formatTime(gameTimer));
        String moneyText = String.format("Money: $%d", moneyCollected);
        String killsText = String.format("Enemies: %d", enemiesKilled);
        String levelText = String.format("Level: %d", player.getCurrentLevel());

        // Simple scoring formula: money + kills*10 + level*50
        int totalPoints = moneyCollected + (enemiesKilled * 10) + (player.getCurrentLevel() * 50);
        String pointsText = String.format("Score: %d", totalPoints);

        canvas.drawText(timeText, screenCenterX, statsStartY, statsPaint);
        canvas.drawText(moneyText, screenCenterX, statsStartY + statsSpacing, statsPaint);
        canvas.drawText(killsText, screenCenterX, statsStartY + (statsSpacing * 2), statsPaint);
        canvas.drawText(levelText, screenCenterX, statsStartY + (statsSpacing * 3), statsPaint);
        canvas.drawText(pointsText, screenCenterX, statsStartY + (statsSpacing * 4), statsPaint);

        // Draw buttons
        float btnW = GameConstants.scale(GameConstants.UI_MENU_WIDTH) * 0.7f;
        float btnH = GameConstants.scale(GameConstants.UI_BUTTON_HEIGHT) * 0.9f;
        float btnLeft = screenCenterX - (btnW / 2f);
        float spacing = 20f;

        // Play Again Button
        float top1 = statsStartY + (statsSpacing * 5.2f);
        playAgainButtonRect = new RectF(btnLeft, top1, btnLeft + btnW, top1 + btnH);
        canvas.drawRect(playAgainButtonRect, menuButtonPaint);
        canvas.drawText("PLAY AGAIN", screenCenterX, playAgainButtonRect.centerY() + 25f, menuTextPaint);

        // Main Menu Button
        float top2 = top1 + btnH + spacing;
        mainMenuButtonRect = new RectF(btnLeft, top2, btnLeft + btnW, top2 + btnH);
        canvas.drawRect(mainMenuButtonRect, menuButtonPaint);
        canvas.drawText("MAIN MENU", screenCenterX, mainMenuButtonRect.centerY() + 25f, menuTextPaint);
    }
    private void drawMobRush(Canvas canvas){
        if(mobRushTimer > GameConstants.MOB_RUSH_START && mobRushTimer < GameConstants.MOB_RUSH_END) {
            canvas.drawRect(0,0,screenCenterX*2,screenCenterY*2,mobRushOverlayPaint);
            canvas.drawText("MOB RUSH",screenCenterX,screenCenterY-100,mobRushTextPaint);
        }


    }

    private void drawSkillStats(Canvas canvas, Skill skill, String titlePrefix, float centerY) {
        int currentLvl = skill.getLevel();
        int nextLvl = currentLvl + 1;

        // Show MAX LEVEL if at level 8
        if (currentLvl >= 8) {
            String title = String.format("%s (Lv %d)", titlePrefix, currentLvl);
            canvas.drawText(title, screenCenterX, centerY - 20, menuTextPaint);
            canvas.drawText("MAX LEVEL", screenCenterX, centerY + 40, menuDetailPaint);
        } else {
            // Show upgrade info using the skill's upgrade description
            String title = String.format("%s (Lv %d -> %d)", titlePrefix, currentLvl, nextLvl);
            String upgradeDesc = skill.getUpgradeDescription();

            canvas.drawText(title, screenCenterX, centerY - 20, menuTextPaint);
            canvas.drawText(upgradeDesc, screenCenterX, centerY + 40, menuDetailPaint);
        }
    }

    private Skill findSkill(Class<? extends Skill> skillClass) {
        for (Skill s : player.getSkills()) {
            if (skillClass.isInstance(s)) return s;
        }
        return null;
    }

    public void handleInput(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            // NEW: Handle Game Over or Victory button clicks
            if (gameState == GameState.OVER || gameState == GameState.VICTORY) {
                // Play Again button
                if (playAgainButtonRect != null && playAgainButtonRect.contains(x, y)) {
                    triggerPlayAgain();
                }
                // Main Menu button
                else if (mainMenuButtonRect != null && mainMenuButtonRect.contains(x, y)) {
                    triggerReturnToMenu();
                }
                return;
            }

            // Check pause button press
            if (pauseButtonRect != null && pauseButtonRect.contains(x, y)) {
                if (gameState == GameState.RUNNING||gameState==GameState.MOB_RUSH) {
                    pauseGame();
                }
                return;
            }
        }

        if (gameState == GameState.PAUSED && event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            // Resume button
            if (resumeButtonRect != null && resumeButtonRect.contains(x, y)) {
                resumeGame();
            }
            // Quit button
            if (quitButtonRect != null && quitButtonRect.contains(x, y)) {
                triggerReturnToMenu();
            }
            // Debug toggle
            if (debugToggleRect != null && debugToggleRect.contains(x, y)) {
                showDebugHUD = !showDebugHUD;
                try {
                    SharedPreferences prefs = gameContext.getSharedPreferences("GameData", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("debug_show_hud", showDebugHUD).apply();
                } catch (Exception ignored) { }
            }
            return;
        }

        if (gameState == GameState.LEVEL_UP && event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            // Button 1: Fireball
            if (upgradeButtonRect != null && upgradeButtonRect.contains(x, y)) {
                player.getSkills().get(0).applyUpgrade();
                resumeGame();
            }
            // Button 2: Arcane Ray
            else if (unlockButtonRect != null && unlockButtonRect.contains(x, y)) {
                Skill arcaneRay = findSkill(ArcaneRaySkill.class);
                if (arcaneRay != null) {
                    arcaneRay.applyUpgrade();
                } else {
                    ArcaneRaySkill newSkill = new ArcaneRaySkill(player, raySprites);
                    newSkill.setCharacterManager(characterManager);
                    player.addSkill(newSkill);
                }
                resumeGame();
            }
            // Button 3: Electric Field (NEW)
            else if (unlockElectricFieldButtonRect != null && unlockElectricFieldButtonRect.contains(x, y)) {
                Skill electricField = findSkill(ElectricFieldSkill.class);
                if (electricField != null) {
                    electricField.applyUpgrade();
                } else {
                    ElectricFieldSkill newSkill = new ElectricFieldSkill(player, electricFieldSprites);
                    newSkill.setCharacterManager(characterManager);
                    player.addSkill(newSkill);
                }
                resumeGame();
            }
        }
    }

    private void resumeGame() {
        // Decrement pending level-ups and show next menu if there are more
        if (pendingLevelUps > 1) {
            pendingLevelUps--;
            setGameState(GameState.LEVEL_UP);
        } else {
            pendingLevelUps = 0;
            setGameState(GameState.RUNNING);
        }
        joystick.resetHandle();
    }

    // NEW: Pause the game
    private void pauseGame() {
        setGameState(GameState.PAUSED);
    }

    // NEW: Trigger game over
    private void triggerGameOver() {
        setGameState(GameState.OVER);
        // Capture the player's coin count from this round
        moneyCollected = player.getCurrentGainedCoin();
    }

    // NEW: Trigger victory (win after timer)
    private void triggerVictory() {
        setGameState(GameState.VICTORY);
        // Capture end-of-run stats
        moneyCollected = player.getCurrentGainedCoin();
    }

    // NEW: Play again callback (will be called from MainActivity)
    public void triggerPlayAgain() {
        if (gameOverListener != null) {
            gameOverListener.onPlayAgain();
        }
    }

    // NEW: Return to menu callback (will be called from MainActivity)
    public void triggerReturnToMenu() {
        if (gameOverListener != null) {
            gameOverListener.onReturnToMenu();
        }
    }

    // NEW: Set the game over listener
    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    // NEW: Increment enemies killed count
    public void incrementEnemiesKilled() {
        enemiesKilled++;
    }

    public int getEnemiesKilled() {
        return enemiesKilled;
    }

    public int getMoneyCollected() {
        return moneyCollected;
    }

    public int getGameTimerSeconds() {
        return (int) gameTimer;
    }

    public void handleLevelUp() {
        pendingLevelUps++;
        this.gameState = GameState.LEVEL_UP;
    }

    public Player getPlayer() {
        return player;
    }

    public void setGameState(GameState newState) {
        this.gameState = newState;
    }

    public void setScreenDimensions(float width, float height) {
        this.screenCenterX = width / 2f;
        this.screenCenterY = height / 2f;
    }

    private void drawBackground(Canvas canvas, float offsetX, float offsetY) {
        if (backgroundBitmap == null) {
            canvas.drawRGB(0, 0, 0);
            return;
        }
        int tileWidth = backgroundBitmap.getWidth();
        int tileHeight = backgroundBitmap.getHeight();
        if (tileWidth == 0 || tileHeight == 0) return;
        float screenWidth = screenCenterX * 2;
        float screenHeight = screenCenterY * 2;
        float worldLeft = offsetX - screenCenterX;
        float worldTop = offsetY - screenCenterY;
        int startTileX = (int) Math.floor(worldLeft / tileWidth);
        int startTileY = (int) Math.floor(worldTop / tileHeight);
        for (int i = startTileX; (i * tileWidth) < (worldLeft + screenWidth); i++) {
            for (int j = startTileY; (j * tileHeight) < (worldTop + screenHeight); j++) {
                float tileWorldX = i * tileWidth;
                float tileWorldY = j * tileHeight;
                float finalX = tileWorldX - offsetX + screenCenterX;
                float finalY = tileWorldY - offsetY + screenCenterY;
                canvas.drawBitmap(backgroundBitmap, finalX, finalY, null);
            }
        }
    }

    private void drawPlayerCentered(Canvas canvas) {
        player.drawOnScreen(canvas, player.getPositionX(), player.getPositionY(), screenCenterX, screenCenterY);
    }

    private void drawJoystick(Canvas canvas) {
        Paint outer = new Paint();
        outer.setColor(Color.GRAY);
        outer.setAlpha(100);
        Paint inner = new Paint();
        inner.setColor(Color.WHITE);
        inner.setAlpha(150);
        canvas.drawCircle(joystick.getCenter().x, joystick.getCenter().y, joystick.getOuterRadius(), outer);
        canvas.drawCircle(joystick.getHandlePosition().x, joystick.getHandlePosition().y, joystick.getInnerRadius(), inner);
    }

    public void loadAssets(Context context) {
        // Background - only load default if not already set
        if (this.backgroundBitmap == null) {
            Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.game_background);
            this.backgroundBitmap = scaleBackgroundBitmap(defaultBitmap);
        }


        // Load Ray Sprites
        raySprites = new Bitmap[8];
        int baseSize = 200;
        raySprites[0] = loadAndScaleSprite(context, R.drawable.ray_0, baseSize);
        raySprites[1] = loadAndScaleSprite(context, R.drawable.ray_1, baseSize);
        raySprites[2] = loadAndScaleSprite(context, R.drawable.ray_2, baseSize);
        raySprites[3] = loadAndScaleSprite(context, R.drawable.ray_3, baseSize);
        raySprites[4] = loadAndScaleSprite(context, R.drawable.ray_4, baseSize);
        raySprites[5] = loadAndScaleSprite(context, R.drawable.ray_5, baseSize);
        raySprites[6] = loadAndScaleSprite(context, R.drawable.ray_6, baseSize);
        raySprites[7] = loadAndScaleSprite(context, R.drawable.ray_7, baseSize);

        // Load Enemy Sprites
        enemySprites = new Bitmap[5];
        int enemySize = (int) GameConstants.scale(GameConstants.ENEMY_SIZE);
        enemySprites[0] = loadAndScaleSprite(context, R.drawable.enemy0, enemySize);
        enemySprites[1] = loadAndScaleSprite(context, R.drawable.enemy1, enemySize);
        enemySprites[2] = loadAndScaleSprite(context, R.drawable.enemy2, enemySize);
        enemySprites[3] = loadAndScaleSprite(context, R.drawable.enemy3, enemySize);
        enemySprites[4] = loadAndScaleSprite(context, R.drawable.enemy4, enemySize);

        // Load Tank Sprites
        enemyTankSprites = new Bitmap[5];
        int tankSize = (int) GameConstants.scale(GameConstants.TANK_SIZE);
        enemyTankSprites[0] = loadAndScaleSprite(context, R.drawable.tank_0, tankSize);
        enemyTankSprites[1] = loadAndScaleSprite(context, R.drawable.tank_1, tankSize);
        enemyTankSprites[2] = loadAndScaleSprite(context, R.drawable.tank_2, tankSize);
        enemyTankSprites[3] = loadAndScaleSprite(context, R.drawable.tank_3, tankSize);
        enemyTankSprites[4] = loadAndScaleSprite(context, R.drawable.tank_4, tankSize);

        // NEW: Load Electric Field Sprites
        electricFieldSprites = new Bitmap[4];
        int fieldSize = 400; // Base size for the field sprite
        electricFieldSprites[0] = loadAndScaleSprite(context, R.drawable.electric_field_0, fieldSize);
        electricFieldSprites[1] = loadAndScaleSprite(context, R.drawable.electric_field_1, fieldSize);
        electricFieldSprites[2] = loadAndScaleSprite(context, R.drawable.electric_field_2, fieldSize);
        electricFieldSprites[3] = loadAndScaleSprite(context, R.drawable.electric_field_3, fieldSize);
        // After sprites load, ensure any character-specific skills are added
        try {
            ensureCharacterSkillsApplied();
        } catch (Exception ignored) { }
    }
    
    /**
     * Get CharacterManager for skill integration
     */
    public CharacterManager getCharacterManager() {
        return characterManager;
    }
}