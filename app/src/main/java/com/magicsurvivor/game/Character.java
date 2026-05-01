package com.magicsurvivor.game;

/**
 * Character represents a selectable playable character with unique perks and abilities.
 * Each character can have special bonuses that are applied during gameplay.
 */
public class Character {

    public enum CharacterType {
        MAGE("Red Mage", 2500, "Master of fire magic", true),
        STORMBORN("Stormborn", 3000, "Electric field specialist", false),
        KNIGHT("Tall Knight", 3500, "Defensive tank", false),
        ROGUE("Rogue", 2000, "Swift and agile", false);

        public final String displayName;
        public final int cost;
        public final String description;
        public final boolean isDefault;

        CharacterType(String displayName, int cost, String description, boolean isDefault) {
            this.displayName = displayName;
            this.cost = cost;
            this.description = description;
            this.isDefault = isDefault;
        }
    }

    private final CharacterType type;
    private final String name;
    private final String description;
    private final int purchaseCost;
    private boolean isUnlocked;
    private boolean isSelected;

    // Character-specific stat modifiers
    private final float healthBonus;           // Percentage bonus to max health
    private final float movementSpeedBonus;    // Percentage bonus to movement speed
    private final float damageBonus;           // Percentage bonus to skill damage
    private final float cooldownReduction;     // Percentage reduction to cooldown (0.1 = 10% faster)

    // Character-specific skill modifiers
    private final float fireballBonus;         // Bonus for fireball skill
    private final float electricFieldBonus;    // Bonus for electric field skill
    private final float arcaneRayBonus;        // Bonus for arcane ray skill

    // Description of special perks
    private final String specialPerk;

    public Character(CharacterType type) {
        this.type = type;
        this.name = type.displayName;
        this.description = type.description;
        this.purchaseCost = type.cost;
        this.isUnlocked = type.isDefault; // Mage starts unlocked
        this.isSelected = type.isDefault;

        // Initialize character-specific stats based on type
        switch (type) {
            case MAGE:
                this.healthBonus = 0.15f;        // 15% more health
                this.movementSpeedBonus = 0f;
                this.damageBonus = 0.20f;        // 20% more damage
                this.cooldownReduction = 0f;
                this.fireballBonus = 0.35f;      // 35% bonus fireball damage
                this.electricFieldBonus = 0f;
                this.arcaneRayBonus = 0f;
                this.specialPerk = "Bonus fireball at last upgrade: +50% damage";
                break;

            case STORMBORN:
                this.healthBonus = 0.10f;        // 10% more health
                this.movementSpeedBonus = 0.15f; // 15% faster
                this.damageBonus = 0.10f;        // 10% more damage
                this.cooldownReduction = 0.05f;  // 5% cooldown reduction
                this.fireballBonus = 0f;
                this.electricFieldBonus = 0.50f; // 50% bonus electric field damage
                this.arcaneRayBonus = 0f;
                this.specialPerk = "Bonus electric field damage + 20% cooldown reduction";
                break;

            case KNIGHT:
                this.healthBonus = 0.50f;        // 50% more health
                this.movementSpeedBonus = -0.20f; // 20% slower
                this.damageBonus = -0.10f;       // 10% less damage
                this.cooldownReduction = 0f;
                this.fireballBonus = 0f;
                this.electricFieldBonus = 0f;
                this.arcaneRayBonus = 0.25f;     // 25% bonus arcane ray
                this.specialPerk = "Extra armor: 30% damage reduction + health regen per enemy killed";
                break;

            case ROGUE:
                this.healthBonus = -0.20f;       // 20% less health
                this.movementSpeedBonus = 0.40f; // 40% faster
                this.damageBonus = 0.25f;        // 25% more damage
                this.cooldownReduction = 0.10f;  // 10% cooldown reduction
                this.fireballBonus = 0.20f;
                this.electricFieldBonus = 0f;
                this.arcaneRayBonus = 0f;
                this.specialPerk = "Swift strikes: 40% faster movement + 10% cooldown reduction";
                break;

            default:
                this.healthBonus = 0f;
                this.movementSpeedBonus = 0f;
                this.damageBonus = 0f;
                this.cooldownReduction = 0f;
                this.fireballBonus = 0f;
                this.electricFieldBonus = 0f;
                this.arcaneRayBonus = 0f;
                this.specialPerk = "";
                break;
        }
    }

    // Getters
    public CharacterType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPurchaseCost() {
        return purchaseCost;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getSpecialPerk() {
        return specialPerk;
    }

    // Stat modifiers
    public float getHealthBonus() {
        return healthBonus;
    }

    public float getMovementSpeedBonus() {
        return movementSpeedBonus;
    }

    public float getDamageBonus() {
        return damageBonus;
    }

    public float getCooldownReduction() {
        return cooldownReduction;
    }

    // Skill modifiers
    public float getFireballBonus() {
        return fireballBonus;
    }

    public float getElectricFieldBonus() {
        return electricFieldBonus;
    }

    public float getArcaneRayBonus() {
        return arcaneRayBonus;
    }

    // Setters
    public void unlock() {
        this.isUnlocked = true;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    /**
     * Get a formatted string showing all bonuses
     */
    public String getFullStats() {
        StringBuilder stats = new StringBuilder();
        stats.append(name).append("\n");
        stats.append("Cost: ").append(purchaseCost).append(" coins\n");
        stats.append("\nPerks:\n");

        if (healthBonus != 0) {
            stats.append(String.format("• Health: %+.0f%%\n", healthBonus * 100));
        }
        if (movementSpeedBonus != 0) {
            stats.append(String.format("• Movement Speed: %+.0f%%\n", movementSpeedBonus * 100));
        }
        if (damageBonus != 0) {
            stats.append(String.format("• Damage: %+.0f%%\n", damageBonus * 100));
        }
        if (cooldownReduction != 0) {
            stats.append(String.format("• Cooldown: %+.0f%%\n", -cooldownReduction * 100));
        }
        if (fireballBonus != 0) {
            stats.append(String.format("• Fireball: %+.0f%%\n", fireballBonus * 100));
        }
        if (electricFieldBonus != 0) {
            stats.append(String.format("• Electric Field: %+.0f%%\n", electricFieldBonus * 100));
        }
        if (arcaneRayBonus != 0) {
            stats.append(String.format("• Arcane Ray: %+.0f%%\n", arcaneRayBonus * 100));
        }

        stats.append("\nSpecial: ").append(specialPerk);

        return stats.toString();
    }

    // Getter and setter for unlock status (used by Firebase loading)
    public void setUnlocked(boolean unlocked) {
        this.isUnlocked = unlocked;
    }
}
