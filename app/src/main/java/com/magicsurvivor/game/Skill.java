package com.magicsurvivor.game;

/**
 * Skill is the base class for all active player abilities.
 * Now uses delta time for frame-rate independent cooldowns.
 * Supports preset upgrade paths for consistent progression.
 */
public abstract class Skill {

    protected float currentCooldown; // In seconds
    protected float maxCooldown;     // In seconds
    protected float baseDamage;
    protected float damageMultiplier;
    protected int level;
    protected final int maxLevel = 8;
    protected final Player playerRef;
    protected String name;
    protected String description;
    
    // Upgrade path - subclasses override this with their specific upgrades
    protected SkillUpgrade[] upgradePath;
    
    // Character system
    protected CharacterManager characterManager;

    public Skill(Player player, float initialCooldown, float initialDamage, String name, String description) {
        this.playerRef = player;
        this.maxCooldown = initialCooldown;
        this.currentCooldown = 0f;
        this.baseDamage = initialDamage;
        this.damageMultiplier = 1.0f;
        this.level = 1;
        this.name = name;
        this.description = description;
        this.upgradePath = null; // Subclasses will initialize this
        this.characterManager = null;
    }

    /**
     * Update logic with delta time for frame-rate independence.
     */
    public void updateLogic(EntityManager entityManager, float deltaTime) {
        if (currentCooldown > 0) {
            currentCooldown -= deltaTime;
        }

        if (currentCooldown <= 0) {
            // Skill is ready to fire
            activate(entityManager);
            // Apply character cooldown multiplier if available (supports characters that reduce cooldowns)
            float cdMult = 1.0f;
            if (characterManager != null) cdMult = characterManager.getCooldownMultiplier();
            currentCooldown = maxCooldown * cdMult;
        }
    }

    public abstract void activate(EntityManager entityManager);

    public void applyUpgrade() {
        if (level < maxLevel) {
            level++;
            
            // If this skill has a preset upgrade path, use it
            if (upgradePath != null && upgradePath.length >= level) {
                SkillUpgrade upgrade = upgradePath[level - 1];
                this.baseDamage *= upgrade.damageMultiplier;
                this.maxCooldown *= upgrade.cooldownMultiplier;
            } else {
                // Fallback to generic upgrade for skills without preset paths
                this.baseDamage *= GameConstants.SKILL_DAMAGE_UPGRADE;
                this.maxCooldown *= GameConstants.SKILL_COOLDOWN_UPGRADE;
            }
        }
    }

    public float getCurrentDamage() { return baseDamage * damageMultiplier; }
    public int getLevel() { return level; }
    // === NEW GETTER ===
    public float getMaxCooldown() { return maxCooldown; }
    public float getCurrentCooldown() { return currentCooldown; }
    
    /**
     * Get the description of the next upgrade (or current level if max).
     */
    public String getUpgradeDescription() {
        if (upgradePath != null && upgradePath.length > level) {
            return upgradePath[level].description;
        } else if (upgradePath != null && level > 0 && level <= upgradePath.length) {
            return upgradePath[level - 1].description;
        }
        return "Max level reached";
    }
    
    /**
     * Set character manager for skill perks
     */
    public void setCharacterManager(CharacterManager charManager) {
        this.characterManager = charManager;
    }
    
    /**
     * Get damage multiplier from character bonuses
     */
    protected float getCharacterDamageMultiplier() {
        if (characterManager == null) return 1.0f;
        return characterManager.getSkillDamageMultiplier(name);
    }
}