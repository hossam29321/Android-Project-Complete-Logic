package com.magicsurvivor.game;

/**
 * SkillUpgrade represents a preset upgrade pattern for a specific skill level.
 * Each upgrade defines how the skill improves at that level.
 */
public class SkillUpgrade {
    public int level;
    public float damageMultiplier;      // 1.0 = no change, 1.15 = 15% increase
    public float cooldownMultiplier;    // 0.9 = 10% cooldown reduction, 1.0 = no change
    public float sizeMultiplier;        // 1.1 = 10% size increase, 1.0 = no change
    public int projectileBonus;         // +1, +2 projectiles, 0 = no change
    public int rayCountBonus;           // For Arcane Ray: additional rays per level
    public float radiusMultiplier;      // For Electric Field: radius increase
    public String description;

    /**
     * Constructor for general skill upgrades.
     */
    public SkillUpgrade(int level, float damage, float cooldown, float size, int projectiles, String desc) {
        this.level = level;
        this.damageMultiplier = damage;
        this.cooldownMultiplier = cooldown;
        this.sizeMultiplier = size;
        this.projectileBonus = projectiles;
        this.rayCountBonus = 0;
        this.radiusMultiplier = 1.0f;
        this.description = desc;
    }

    /**
     * Constructor with ray count bonus for Arcane Ray.
     */
    public SkillUpgrade(int level, float damage, float cooldown, float size, int projectiles, int rays, String desc) {
        this.level = level;
        this.damageMultiplier = damage;
        this.cooldownMultiplier = cooldown;
        this.sizeMultiplier = size;
        this.projectileBonus = projectiles;
        this.rayCountBonus = rays;
        this.radiusMultiplier = 1.0f;
        this.description = desc;
    }

    /**
     * Constructor with radius multiplier for Electric Field.
     */
    public SkillUpgrade(int level, float damage, float cooldown, float radius, String desc) {
        this.level = level;
        this.damageMultiplier = damage;
        this.cooldownMultiplier = cooldown;
        this.sizeMultiplier = 1.0f;
        this.projectileBonus = 0;
        this.rayCountBonus = 0;
        this.radiusMultiplier = radius;
        this.description = desc;
    }
}
