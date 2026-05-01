package com.magicsurvivor.screens;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.magicsurvivor.R;
import com.magicsurvivor.game.Character;
import com.magicsurvivor.game.CharacterManager;

/**
 * CharacterSelectionScreen allows players to view, purchase, and select characters.
 * Displays character stats and special perks.
 */
public class CharacterSelectionScreen extends AppCompatActivity {
    
    private CharacterManager characterManager;
    private SharedPreferences sharedPreferences;
    private int playerMoney;
    private String userId;
    private Character currentViewedCharacter;
    
    private LinearLayout characterButtonsContainer;
    private TextView characterDetailsTextView;
    private TextView characterNameTextView;
    private TextView characterCostTextView;
    private Button actionButton; // Purchase or Select
    private Button backButton;
    private FrameLayout selectedCharacterIndicator;
    private DatabaseReference firebaseRef;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_selection);
        
        // Initialize character manager
        characterManager = new CharacterManager();
        
        // Get user ID from intent
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        String moneyFromIntent = intent.getStringExtra("money");
        
        // CharacterSelection: userId and moneyFromIntent loaded
        
        // Get shared preferences for character selection
        sharedPreferences = getSharedPreferences("GameData", MODE_PRIVATE);
        
        // Initialize Firebase reference for money
        firebaseRef = FirebaseDatabase.getInstance().getReference();
        
        // Initialize UI elements
        initializeUI();
        
        // Load player money and character unlocks from Firebase
        if (userId != null && !userId.isEmpty()) {
            loadPlayerMoneyFromFirebase(() -> {
                loadCharacterUnlocksFromFirebase(() -> {
                    createCharacterButtons();
                    if (characterManager.getAllCharacters().length > 0) {
                        // Restore previously selected character if available
                        String saved = sharedPreferences.getString("selectedCharacter", "MAGE");
                        try {
                            Character.CharacterType savedType = Character.CharacterType.valueOf(saved);
                            boolean ok = characterManager.selectCharacter(savedType);
                            if (ok) {
                                displayCharacterDetails(characterManager.getSelectedCharacter());
                            } else {
                                displayCharacterDetails(characterManager.getAllCharacters()[0]);
                            }
                        } catch (Exception e) {
                            displayCharacterDetails(characterManager.getAllCharacters()[0]);
                        }
                    }
                });
            });
        } else {
            // Fallback: use money from intent
            try {
                if (moneyFromIntent != null) {
                    playerMoney = Integer.parseInt(moneyFromIntent.trim());
                }
            } catch (Exception e) {
                playerMoney = 0;
            }
            createCharacterButtons();
            if (characterManager.getAllCharacters().length > 0) {
                // Try to restore saved selection from SharedPreferences
                String saved = sharedPreferences.getString("selectedCharacter", "MAGE");
                try {
                    Character.CharacterType savedType = Character.CharacterType.valueOf(saved);
                    boolean ok = characterManager.selectCharacter(savedType);
                    if (ok) {
                        displayCharacterDetails(characterManager.getSelectedCharacter());
                    } else {
                        displayCharacterDetails(characterManager.getAllCharacters()[0]);
                    }
                } catch (Exception e) {
                    displayCharacterDetails(characterManager.getAllCharacters()[0]);
                }
            }
        }
    }
    
    /**
     * Load character unlock status from Firebase with callback
     */
    private void loadCharacterUnlocksFromFirebase(Runnable onComplete) {
        if (userId == null || userId.isEmpty()) {
            android.util.Log.w("CharacterSelection", "userId is null or empty, skipping character unlock load");
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // Loading character unlocks from Firebase
        
        firebaseRef.child(userId).child("characters").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    Character[] characters = characterManager.getAllCharacters();
                    if (dataSnapshot.exists()) {
                        // Load each character's unlock status from Firebase
                        for (Character character : characters) {
                            String charKey = character.getType().name();
                            if (dataSnapshot.hasChild(charKey)) {
                                Boolean unlocked = dataSnapshot.child(charKey).child("unlocked").getValue(Boolean.class);
                                if (unlocked != null && unlocked) {
                                    character.setUnlocked(true);
                                }
                            }
                        }
                    } else {
                        android.util.Log.w("CharacterSelection", "No characters data in Firebase, using defaults");
                    }
                } catch (Exception e) {
                    android.util.Log.e("CharacterSelection", "Error loading character unlocks", e);
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                android.util.Log.e("CharacterSelection", "Firebase error loading character unlocks: " + databaseError.getMessage());
                if (onComplete != null) onComplete.run();
            }
        });
    }
    
    /**
     * Load player money from Firebase with callback
     */
    private void loadPlayerMoneyFromFirebase(Runnable onComplete) {
        if (userId == null || userId.isEmpty()) {
            android.util.Log.w("CharacterSelection", "userId is null or empty, using default money 0");
            playerMoney = 0;
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // Loading money from Firebase
        
        firebaseRef.child(userId).child("money").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    // Firebase snapshot processed
                    
                    if (dataSnapshot.exists()) {
                        Object value = dataSnapshot.getValue();
                        // Firebase value type logged internally
                        
                        if (value instanceof Integer) {
                            playerMoney = (Integer) value;
                        } else if (value instanceof Long) {
                            playerMoney = ((Long) value).intValue();
                        } else if (value instanceof Double) {
                            playerMoney = ((Double) value).intValue();
                        } else {
                            playerMoney = 0;
                        }
                    } else {
                        android.util.Log.w("CharacterSelection", "Firebase path does not exist");
                        playerMoney = 0;
                    }
                    // Final playerMoney determined
                } catch (Exception e) {
                    android.util.Log.e("CharacterSelection", "Error loading money", e);
                    playerMoney = 0;
                }
                
                // Call callback when done
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                android.util.Log.e("CharacterSelection", "Firebase error: " + databaseError.getMessage());
                playerMoney = 0;
                if (onComplete != null) onComplete.run();
            }
        });
    }
    
    private void initializeUI() {
        characterButtonsContainer = findViewById(R.id.character_buttons_container);
        characterDetailsTextView = findViewById(R.id.character_details_text);
        characterNameTextView = findViewById(R.id.character_name_text);
        characterCostTextView = findViewById(R.id.character_cost_text);
        actionButton = findViewById(R.id.action_button);
        backButton = findViewById(R.id.back_button);
        selectedCharacterIndicator = findViewById(R.id.selected_indicator);
        
        actionButton.setOnClickListener(v -> handleActionButtonClick());
        backButton.setOnClickListener(v -> onBackPressed());
    }
    
    private void createCharacterButtons() {
        characterButtonsContainer.removeAllViews();
        
        Character[] characters = characterManager.getAllCharacters();
        for (int i = 0; i < characters.length; i++) {
            Character character = characters[i];
            Button btn = new Button(this);
            btn.setText(character.getName());
            
            // Style button
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 8, 16, 8);
            btn.setLayoutParams(params);
            
            // Color based on unlock status
            if (character.isUnlocked()) {
                btn.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
            } else {
                btn.setBackgroundColor(Color.parseColor("#9E9E9E")); // Gray
            }
            btn.setTextColor(Color.WHITE);
            
            // Add click listener
            final int index = i;
            btn.setOnClickListener(v -> displayCharacterDetails(characters[index]));
            
            characterButtonsContainer.addView(btn);
        }
    }
    
    private void displayCharacterDetails(Character character) {
        currentViewedCharacter = character;
        
        characterNameTextView.setText(character.getName());
        characterDetailsTextView.setText(character.getFullStats());
        
        // Update cost text
        if (character.isUnlocked()) {
            characterCostTextView.setText("UNLOCKED");
            characterCostTextView.setTextColor(Color.GREEN);
        } else {
            characterCostTextView.setText("Cost: " + character.getPurchaseCost() + " coins");
            characterCostTextView.setTextColor(Color.RED);
        }
        
        // Update action button
        updateActionButton(character);
        
        // Show selected indicator if this is the selected character
        if (character.isSelected()) {
            selectedCharacterIndicator.setVisibility(View.VISIBLE);
        } else {
            selectedCharacterIndicator.setVisibility(View.GONE);
        }
    }
    
    private void updateActionButton(Character character) {
        if (character.isUnlocked()) {
            if (character.isSelected()) {
                actionButton.setText("SELECTED");
                actionButton.setEnabled(false);
                actionButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            } else {
                actionButton.setText("SELECT");
                actionButton.setEnabled(true);
                actionButton.setBackgroundColor(Color.parseColor("#2196F3"));
            }
        } else {
            if (playerMoney >= character.getPurchaseCost()) {
                actionButton.setText("PURCHASE - " + character.getPurchaseCost());
                actionButton.setEnabled(true);
                actionButton.setBackgroundColor(Color.parseColor("#FF9800"));
            } else {
                actionButton.setText("NOT ENOUGH COINS");
                actionButton.setEnabled(false);
                actionButton.setBackgroundColor(Color.parseColor("#9E9E9E"));
            }
        }
    }
    
    private void handleActionButtonClick() {
        if (currentViewedCharacter == null) return;
        
        if (currentViewedCharacter.isUnlocked()) {
            // Select character
            characterManager.selectCharacter(currentViewedCharacter.getType());
            saveCharacterSelection();
            
            // Refresh UI
            createCharacterButtons();
            displayCharacterDetails(currentViewedCharacter);
        } else {
            // Purchase character
            if (playerMoney >= currentViewedCharacter.getPurchaseCost()) {
                int newMoney = playerMoney - currentViewedCharacter.getPurchaseCost();
                
                // Update money and character unlock in Firebase
                if (userId != null) {
                    firebaseRef.child(userId).child("money").setValue(newMoney);
                    // Save character unlock to Firebase
                    String charKey = currentViewedCharacter.getType().name();
                    firebaseRef.child(userId).child("characters").child(charKey).child("unlocked").setValue(true);
                }
                
                playerMoney = newMoney;
                characterManager.purchaseCharacter(currentViewedCharacter.getType(), playerMoney + currentViewedCharacter.getPurchaseCost());
                
                // Save progress
                savePlayerProgress();
                saveCharacterSelection();
                
                // Refresh UI
                createCharacterButtons();
                displayCharacterDetails(currentViewedCharacter);
            }
        }
    }
    
    private void saveCharacterSelection() {
        Character selectedCharacter = characterManager.getSelectedCharacter();
        if (selectedCharacter != null) {
            // Use commit() to ensure the selection is written synchronously
            boolean committed = sharedPreferences.edit()
                .putString("selectedCharacter", selectedCharacter.getType().name())
                .commit();
        }
    }
    
    private void savePlayerProgress() {
        sharedPreferences.edit()
                .putInt("playerMoney", playerMoney)
                .apply();
        
        // Save unlocked characters
        Character[] characters = characterManager.getAllCharacters();
        for (Character character : characters) {
            sharedPreferences.edit()
                    .putBoolean("character_unlocked_" + character.getType().name(), character.isUnlocked())
                    .apply();
        }
    }
    
    @Override
    public void onBackPressed() {
        // Return to previous screen with updated money
        Intent intent = new Intent();
        intent.putExtra("money", playerMoney);
        setResult(RESULT_OK, intent);
        finish();
    }
}
