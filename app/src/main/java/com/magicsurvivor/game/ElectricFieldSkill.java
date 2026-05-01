package com.magicsurvivor.game;

import android.graphics.Bitmap;

public class ElectricFieldSkill extends Skill {

    private final Bitmap[] fieldSprites;
    private ElectricField activeField;

    public ElectricFieldSkill(Player player, Bitmap[] sprites) {
        super(player,
                0f, // No cooldown - it's always active
                GameConstants.ELECTRIC_FIELD_DAMAGE,
                "Electric Field",
                "Creates a shocking aura around you.");
        this.fieldSprites = sprites;
        
        // Define preset upgrade pattern for Electric Field
        this.upgradePath = new SkillUpgrade[] {
            new SkillUpgrade(1, 1.0f, 1.0f, 1.0f, "Electric Field Lvl 1: Base field"),
            new SkillUpgrade(2, 1.15f, 1.0f, 1.1f, "Electric Field Lvl 2: +15% damage, +10% radius"),
            new SkillUpgrade(3, 1.15f, 1.0f, 1.15f, "Electric Field Lvl 3: +5% radius"),
            new SkillUpgrade(4, 1.3f, 1.0f, 1.2f, "Electric Field Lvl 4: +30% damage, +20% radius"),
            new SkillUpgrade(5, 1.3f, 1.0f, 1.25f, "Electric Field Lvl 5: +5% radius"),
            new SkillUpgrade(6, 1.5f, 1.0f, 1.3f, "Electric Field Lvl 6: +50% damage, +30% radius"),
            new SkillUpgrade(7, 1.5f, 1.0f, 1.35f, "Electric Field Lvl 7: +5% radius"),
            new SkillUpgrade(8, 1.75f, 1.0f, 1.4f, "Electric Field Lvl 8: ULTIMATE: +75% damage, +40% radius")
        };
    }

    @Override
    public void activate(EntityManager entityManager) {
        // Check if field already exists
        if (activeField == null || activeField.isFinished()) {
            // Apply character damage bonus
            float damageMultiplier = getCharacterDamageMultiplier();
            float finalDamage = getCurrentDamage() * damageMultiplier;
            
            // Create new field
            activeField = new ElectricField(
                    playerRef,
                    finalDamage,
                    fieldSprites,
                    this.level
            );
            entityManager.addSkill(activeField);
        }
    }

    @Override
    public void updateLogic(EntityManager entityManager, float deltaTime) {
        // Override parent behavior - we don't use cooldown
        // Just ensure the field exists
        if (activeField == null || activeField.isFinished()) {
            activate(entityManager);
        }
    }

    /**
     * Whether the electric field is currently active in the world
     */
    public boolean isFieldActive() {
        return activeField != null && !activeField.isFinished();
    }

    /**
     * Current damage of the active field (or 0 if none)
     */
    public float getActiveFieldDamage() {
        if (activeField == null || activeField.isFinished()) return 0f;
        return activeField.getDamage();
    }

    @Override
    public void applyUpgrade() {
        if (level < maxLevel) {
            level++;
            
            // Apply preset upgrades if available
            if (upgradePath != null && upgradePath.length >= level) {
                SkillUpgrade upgrade = upgradePath[level - 1];
                this.baseDamage *= upgrade.damageMultiplier;
                // Electric Field doesn't use standard cooldown
            }
        }
        
        // Update the active field's properties
        if (activeField != null && !activeField.isFinished()) {
            float damageMultiplier = getCharacterDamageMultiplier();
            float finalDamage = getCurrentDamage() * damageMultiplier;
            activeField.updateStats(finalDamage, this.level);
        }
    }
}