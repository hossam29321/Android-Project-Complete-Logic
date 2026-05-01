package com.magicsurvivor.game;

/**
 * CharacterManager manages character selection, unlocking, and applying character-specific perks.
 * Stores user's character progression and applies modifiers to the player during gameplay.
 */
public class CharacterManager {
    
    private Character[] availableCharacters;
    private Character selectedCharacter;
    
    public CharacterManager() {
        // Initialize all characters
        availableCharacters = new Character[Character.CharacterType.values().length];
        int index = 0;
        for (Character.CharacterType type : Character.CharacterType.values()) {
            availableCharacters[index] = new Character(type);
            if (type.isDefault) {
                selectedCharacter = availableCharacters[index];
            }
            index++;
        }
    }
    
    /**
     * Get all available characters
     */
    public Character[] getAllCharacters() {
        return availableCharacters;
    }
    
    /**
     * Get the currently selected character
     */
    public Character getSelectedCharacter() {
        return selectedCharacter;
    }
    
    /**
     * Select a character (must be unlocked)
     */
    public boolean selectCharacter(Character.CharacterType type) {
        for (Character character : availableCharacters) {
            if (character.getType() == type) {
                if (character.isUnlocked()) {
                    // Deselect all others
                    for (Character c : availableCharacters) {
                        c.setSelected(false);
                    }
                    character.setSelected(true);
                    selectedCharacter = character;
                    return true;
                }
                return false;
            }
        }
        return false;
    }
    
    /**
     * Purchase and unlock a character
     */
    public boolean purchaseCharacter(Character.CharacterType type, int playerMoney) {
        for (Character character : availableCharacters) {
            if (character.getType() == type && !character.isUnlocked()) {
                if (playerMoney >= character.getPurchaseCost()) {
                    character.unlock();
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if character is already unlocked
     */
    public boolean isCharacterUnlocked(Character.CharacterType type) {
        for (Character character : availableCharacters) {
            if (character.getType() == type) {
                return character.isUnlocked();
            }
        }
        return false;
    }
    
    /**
     * Get character by type
     */
    public Character getCharacter(Character.CharacterType type) {
        for (Character character : availableCharacters) {
            if (character.getType() == type) {
                return character;
            }
        }
        return null;
    }
    
    /**
     * Apply selected character's perks to player
     * Call this method when initializing the player in the game
     */
    public void applyCharacterPerks(Player player) {
        if (selectedCharacter == null) return;
        
        // Health bonus
        if (selectedCharacter.getHealthBonus() != 0) {
            player.applyHealthBonus(selectedCharacter.getHealthBonus());
        }
        
        // Movement speed bonus
        if (selectedCharacter.getMovementSpeedBonus() != 0) {
            player.applyMovementSpeedBonus(selectedCharacter.getMovementSpeedBonus());
        }

        // Damage reduction / other passive perks applied at runtime
        if (selectedCharacter.getType() == Character.CharacterType.KNIGHT) {
            // Knight: 30% damage reduction
            player.setDamageTakenMultiplier(1.0f - 0.30f);
        } else {
            player.setDamageTakenMultiplier(1.0f);
        }
    }

    /**
     * Called when an enemy is killed to trigger character-specific on-kill effects
     */
    public void onEnemyKilled(Player player) {
        if (selectedCharacter == null) return;

        switch (selectedCharacter.getType()) {
            case KNIGHT:
                // Heal a small percentage of max health per kill
                float healAmount = player.getMaxHealth() * 0.05f; // 5% per kill
                player.heal(healAmount);
                break;
            default:
                // No on-kill passive for other characters yet
                break;
        }
    }


    
    /**
     * Get damage multiplier for a specific skill based on character selection
     */
    public float getSkillDamageMultiplier(String skillName) {
        if (selectedCharacter == null) return 1.0f;
        
        switch(skillName) {
            case "Fireball":
                return 1.0f + selectedCharacter.getFireballBonus();
            case "ElectricField":
                return 1.0f + selectedCharacter.getElectricFieldBonus();
            case "ArcaneRay":
                return 1.0f + selectedCharacter.getArcaneRayBonus();
            default:
                return 1.0f + selectedCharacter.getDamageBonus();
        }
    }
    
    /**
     * Get cooldown multiplier based on character selection
     */
    public float getCooldownMultiplier() {
        if (selectedCharacter == null) return 1.0f;
        return 1.0f - selectedCharacter.getCooldownReduction();
    }
}
