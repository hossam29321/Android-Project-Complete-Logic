package com.magicsurvivor.game;

import android.graphics.Canvas;
import java.util.ArrayList;
import java.util.List;

public class EntityManager {

    private final List<GameObject> enemyList;
    private final List<GameObject> skillList;
    private final List<GameObject> gemList;
    private final List<GameObject> coinList;
    private Player player;
    private GameEngine gameEngine;
    private final List<GameObject> healthOrbList;

    public EntityManager() {
        this.enemyList = new ArrayList<>();
        this.skillList = new ArrayList<>();
        this.gemList = new ArrayList<>();
        this.coinList=new ArrayList<>();
        this.healthOrbList = new ArrayList<>();
    }

    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public void grantPlayerXP(int amount) {
        if (player != null && gameEngine != null) {
            player.gainXP(amount, gameEngine);
        }
    }

    // NEW: Notify when an enemy is killed
    public void notifyEnemyKilled() {
        if (gameEngine != null) {
            gameEngine.incrementEnemiesKilled();
            // Trigger character-specific on-kill effects (e.g., Knight regen)
            if (gameEngine.getCharacterManager() != null && player != null) {
                gameEngine.getCharacterManager().onEnemyKilled(player);
            }
        }
    }

    public void setPlayer(Player player) { this.player = player; }
    public void addEnemy(GameObject enemy) { enemyList.add(enemy); }
    public void addSkill(GameObject skill) { skillList.add(skill); }
    public void addGem(GameObject gem) { gemList.add(gem); }
    public void removeGem(GameObject gem) { gemList.remove(gem); }
    public void addCoinOrb(GameObject gem) { coinList.add(gem); }
    public void removeCoinOrb(GameObject gem) { coinList.remove(gem); }

    public void updateAll(Player player, float deltaTime) {
        for (GameObject enemy:enemyList) {
            if(enemy instanceof TankEnemy)
                ((TankEnemy)enemy).updateLogic(player, deltaTime);
            else{
                ((Enemy)enemy).updateLogic(player, deltaTime);
            }
        }
        for (int i = 0; i < skillList.size(); i++) {
            GameObject obj = skillList.get(i);
            if (obj instanceof ElectricField) {
                ((ElectricField) obj).updateLogic(this, deltaTime);
            } else {
                obj.updateLogic(deltaTime);
            }
        }
        for (int i = 0; i < gemList.size(); i++) {
            gemList.get(i).updateLogic(deltaTime);
        }
        for (int i = 0; i < healthOrbList.size(); i++) {
            healthOrbList.get(i).updateLogic(deltaTime);
        }
        for (int i = 0; i < coinList.size(); i++) {
            coinList.get(i).updateLogic(deltaTime);
        }
    }

    public void drawAll(Canvas canvas, float offsetX, float offsetY, float centerX, float centerY) {
        // Draw Gems
        for (GameObject gem : gemList) {
            gem.drawOnScreen(canvas, offsetX, offsetY, centerX, centerY);
        }

        // Draw Health Orbs
        for (GameObject orb : healthOrbList) {
            orb.drawOnScreen(canvas, offsetX, offsetY, centerX, centerY);
        }

        // Draw Coin Orbs
        for (GameObject coinOrb : coinList) {
            coinOrb.drawOnScreen(canvas, offsetX, offsetY, centerX, centerY);
        }

        // Draw Enemies
        drawEnemies(canvas, offsetX, offsetY, centerX, centerY);
    }

    public void drawEnemies(Canvas canvas, float offsetX, float offsetY, float centerX, float centerY) {//
        //regular enemies
        for (GameObject enemy : enemyList) {
            if (enemy instanceof TankEnemy) {
                ((TankEnemy) enemy).drawOnScreen(canvas, offsetX, offsetY, centerX, centerY);
            }
            else if (enemy instanceof Enemy) {
                ((Enemy) enemy).drawOnScreen(canvas, offsetX, offsetY, centerX, centerY);
            }

        }


    }

    public void drawSkills(Canvas canvas, float offsetX, float offsetY, float centerX, float centerY) {
        for (GameObject skill : skillList) {
            skill.drawOnScreen(canvas, offsetX, offsetY, centerX, centerY);
        }
    }

    public void cleanUp() {
        enemyList.removeIf(gameObject -> ((Enemy) gameObject).isDestroyed());
        skillList.removeIf(gameObject -> {
            if (gameObject instanceof FireballProjectile) return ((FireballProjectile) gameObject).isFinished();
            if (gameObject instanceof Explosion) return ((Explosion) gameObject).isFinished();
            if (gameObject instanceof ArcaneRayProjectile) return ((ArcaneRayProjectile) gameObject).isFinished();
            if (gameObject instanceof ElectricField) return ((ElectricField) gameObject).isFinished();
            return false;
        });
    }

    public List<GameObject> getEnemyList() { return enemyList; }
    public List<GameObject> getSkillList() { return skillList; }
    public List<GameObject> getGemList() { return gemList; }
    public List<GameObject> getCoinList() { return coinList; }

    public void addHealthOrb(GameObject orb) { healthOrbList.add(orb); }
    public void removeHealthOrb(GameObject orb) { healthOrbList.remove(orb); }
    public List<GameObject> getHealthOrbList() { return healthOrbList; }
}