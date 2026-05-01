package com.magicsurvivor.screens;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.magicsurvivor.R;



public class MainMenu extends AppCompatActivity {
    private TextView money;
    private AnimationDrawable menuAnimation;
    private String moneyy;
    private String userId;
    private SharedPreferences sharedPreferences;
    private DatabaseReference firebaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("GameData", MODE_PRIVATE);

        // Initialize Firebase reference
        firebaseRef = FirebaseDatabase.getInstance().getReference();

        // 1. Get the RelativeLayout by ID
        RelativeLayout layout = findViewById(R.id.main);

        // 2. Get the background and cast it
        menuAnimation = (AnimationDrawable) layout.getBackground();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        money = findViewById(R.id.money);
        Intent intent = getIntent();
        moneyy = intent.getStringExtra("money");
        userId = intent.getStringExtra("userId");

        // If userId not in intent, try to get from SharedPreferences
        if (userId == null || userId.isEmpty()) {
            userId = sharedPreferences.getString("userId", "");
        } else {
            // Save userId to SharedPreferences for persistence
            sharedPreferences.edit().putString("userId", userId).apply();
        }

        // userId and money loaded

        money.setText(moneyy);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (menuAnimation != null) {
            if (hasFocus) {
                menuAnimation.start();
            } else {
                menuAnimation.stop();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop animation when pausing to prevent crash
        if (menuAnimation != null) {
            menuAnimation.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMoneyFromFirebase();
        // Restart animation when resuming
        if (menuAnimation != null) {
            menuAnimation.start();
        }
    }

    public void play(View view) {
        //Load the animation
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);

        //Set a listener to detect when it ends
        pulse.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = new Intent(MainMenu.this, LevelSelect.class);
                intent.putExtra("money", moneyy);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();


            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        //Start the animation
        view.startAnimation(pulse);
    }

    public void store(View view) {
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        pulse.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent intent = new Intent(MainMenu.this, CharacterSelectionScreen.class);
                intent.putExtra("money", moneyy);
                intent.putExtra("userId", userId);
                startActivityForResult(intent, 1001);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(pulse);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            try {
                int updatedMoney = data.getIntExtra("money", 0);
                moneyy = String.valueOf(updatedMoney);
                money.setText(moneyy);
            } catch (Exception e) {
                android.util.Log.e("MainMenu", "Error parsing money from result", e);
            }
        }
    }

    /**
     * Refresh money from Firebase on resume
     */
    private void refreshMoneyFromFirebase() {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        firebaseRef.child(userId).child("money").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        Object value = dataSnapshot.getValue();
                        int freshMoney = 0;

                        if (value instanceof Integer) {
                            freshMoney = (Integer) value;
                        } else if (value instanceof Long) {
                            freshMoney = ((Long) value).intValue();
                        } else if (value instanceof Double) {
                            freshMoney = ((Double) value).intValue();
                        }

                        moneyy = String.valueOf(freshMoney);
                        money.setText(moneyy);
                    } else {
                        android.util.Log.w("MainMenu", "Money path does not exist in Firebase");
                    }
                } catch (Exception e) {
                    android.util.Log.e("MainMenu", "Error refreshing money", e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                android.util.Log.e("MainMenu", "Firebase error refreshing money: " + databaseError.getMessage());
            }
        });
    }
}