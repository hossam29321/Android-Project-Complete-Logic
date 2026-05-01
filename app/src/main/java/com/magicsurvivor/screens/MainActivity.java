package com.magicsurvivor.screens;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.magicsurvivor.GameView;
import com.magicsurvivor.game.GameEngine;

/**
 * MainActivity is the main entry point for the Android application.
 * It sets the custom GameView as the content, handles full-screen settings,
 * and manages the application's lifecycle (pause/resume).
 */
public class MainActivity extends Activity implements GameEngine.GameOverListener {
    private GameView gameView; // Reference to your custom game surface\
    private String money;
    private int mapDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setWindowAnimations(0);//TEST

        requestWindowFeature(Window.FEATURE_NO_TITLE);//TEST

        // 1. Set Full Screen Mode
        // Removes the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Get intent extras FIRST before initializing GameView
        Intent intent = getIntent();
        String uid = intent.getStringExtra("userId");
        money = intent.getStringExtra("money");
        if (money != null) {
            money = money.trim();
        }
        mapDrawable = intent.getIntExtra("selectedMap", -1);
        String mapUri = intent.getStringExtra("selectedMapUri");
        
        // Selected map info (if any)

        // 2. Instantiate and Set Content
        // Create an instance of custom SurfaceView
        gameView = new GameView(this);
        
        // Set the background based on selected map
        if (mapUri != null) {
            // Custom image from gallery or camera
            gameView.getGameEngine().setBackgroundFromUri(Uri.parse(mapUri), this);
        } else if (mapDrawable > 0) {
            // Resource drawable
            gameView.getGameEngine().setBackground(mapDrawable, this);
        }

        // Set the custom GameView as the content of this Activity
        setContentView(gameView);
        
        // Load character unlocks from Firebase
        if (uid != null) {
            gameView.getGameEngine().loadCharacterUnlocksFromFirebase(uid);
            initializeFirebaseUser(uid);
        }
    }

    // --- Lifecycle Management ---

    @Override
    protected void onPause() {
        super.onPause();
        // Stop the game thread when the user minimizes the app or switches focus
        if (gameView != null && gameView.getThread() != null) {
            gameView.getThread().setRunning(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume the game thread
        if (gameView != null && gameView.getThread() != null) {
            gameView.getThread().setRunning(true);
        }
    }

    /**
     * Initializes the user in Firebase database with their UID and last login timestamp.
     */
    private void initializeFirebaseUser(String uid) {
        // Run absolutely everything Firebase-related off-thread
        new Thread(() -> {
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference myRef = db.getReference().child(uid);
            myRef.child("testtest").setValue(1);
            myRef.child("lastLogin").setValue(ServerValue.TIMESTAMP);
        }).start();
    }

    // NEW: Game Over Listener Callbacks
    @Override
    public void onPlayAgain() {
        // Restart the game by creating a new MainActivity
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.putExtra("userId", getIntent().getStringExtra("userId"));
        // Get current money from database for display
        int earnedMoney = gameView.getGameEngine().getMoneyCollected();
        int initialMoney = 0;
        try {
            String trimmedMoney = money != null ? money.trim() : "0";
            initialMoney = Integer.parseInt(trimmedMoney);
        } catch (NumberFormatException e) {
            initialMoney = 0;
        }
        int totalMoney = initialMoney + earnedMoney;
        intent.putExtra("money", String.valueOf(totalMoney));
        startActivity(intent);
        finish();
    }

    @Override
    public void onReturnToMenu() {
        // Get money earned during this session
        int earnedMoney = gameView.getGameEngine().getMoneyCollected();
        
        // Parse initial money from intent
        int initialMoney = 0;
        try {
            String trimmedMoney = money != null ? money.trim() : "0";
            initialMoney = Integer.parseInt(trimmedMoney);
        } catch (NumberFormatException e) {
            initialMoney = 0;
        }
        
        // Calculate total money
        int totalMoney = initialMoney + earnedMoney;
        
        // Save to SharedPreferences (local backup)
        android.content.SharedPreferences prefs = getSharedPreferences("GameData", MODE_PRIVATE);
        prefs.edit()
            .putInt("playerMoney", totalMoney)
            .apply();
        
        // Save to Firebase (main storage)
        String uid = getIntent().getStringExtra("userId");
        if (uid != null && !uid.isEmpty()) {
            DatabaseReference firebaseRef = FirebaseDatabase.getInstance().getReference();
            firebaseRef.child(uid).child("money").setValue(totalMoney);
        }
        
        // Return to main menu
        Intent intent = new Intent(MainActivity.this, MainMenu.class);
        intent.putExtra("money", String.valueOf(totalMoney));
        startActivity(intent);
        finish();
    }





}