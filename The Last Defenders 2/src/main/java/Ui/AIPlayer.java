package Ui;

import java.util.Random;

/**
 * AI Player class that extends Player with AI decision-making capabilities
 * This class handles all AI logic for computer-controlled players
 */
public class AIPlayer extends Player {
    private AIStrategy strategy;
    
    public enum AIStrategy {
        AGGRESSIVE,    // Focuses on attack and buying properties
        DEFENSIVE,     // Focuses on health and medicine
        BALANCED,      // Balanced approach
        ECONOMIC       // Focuses on money and influence
    }
    
    public AIPlayer(String name, String color, AIStrategy strategy) {
        super(name, color);
        this.strategy = strategy;
        this.setAI(true);
    }
    
    public AIPlayer(String name, String color) {
        this(name, color, AIStrategy.BALANCED);
    }
    
    /**
     * AI decision for buying properties
     * @param tile The tile being considered for purchase
     * @param price The price of the tile
     * @return true if AI wants to buy, false otherwise
     */
    public boolean shouldBuyProperty(Tile tile, int price) {
        if (getMoney() < price) return false;
        
        // Calculate property value based on strategy
        double value = calculatePropertyValue(tile, price);
        
        // AI decision threshold based on strategy
        double threshold = getBuyThreshold();
        
        return value >= threshold;
    }
    
    /**
     * AI decision for upgrading properties
     * @param tile The tile to potentially upgrade
     * @param upgradeCost The cost of the upgrade
     * @return true if AI wants to upgrade, false otherwise
     */
    public boolean shouldUpgradeProperty(Tile tile, int upgradeCost) {
        if (getMoney() < upgradeCost) return false;
        if (tile.getUpgradeLevel() >= 3) return false; // Max level
        
        // Calculate upgrade value
        double value = calculateUpgradeValue(tile, upgradeCost);
        double threshold = getUpgradeThreshold();
        
        return value >= threshold;
    }
    
    /**
     * AI decision for using medicine
     * @return true if AI wants to use medicine, false otherwise
     */
    public boolean shouldUseMedicine() {
        if (getMedicine() <= 0) return false;
        
        // Use medicine if health is below threshold
        double healthPercentage = (double) getHealth() / getMaxHealth();
        double threshold = getMedicineThreshold();
        
        return healthPercentage < threshold;
    }
    
    /**
     * AI decision for attacking other players
     * @param target The potential target player
     * @return true if AI wants to attack, false otherwise
     */
    public boolean shouldAttackPlayer(Player target) {
        if (target == this) return false;
        
        // Don't attack if we're weaker
        if (getAttack() < target.getAttack()) return false;
        
        // Calculate attack value based on strategy
        double value = calculateAttackValue(target);
        double threshold = getAttackThreshold();
        
        return value >= threshold;
    }
    
    /**
     * AI decision for funding lab cure
     * @param cost The cost to fund the lab cure
     * @return true if AI wants to fund, false otherwise
     */
    public boolean shouldFundLabCure(int cost) {
        if (getMoney() < cost) return false;
        if (isLabCureFunded()) return false; // Already funded
        
        // Calculate lab cure value
        double value = calculateLabCureValue(cost);
        double threshold = getLabCureThreshold();
        
        return value >= threshold;
    }
    
    // Private helper methods for AI calculations
    
    private double calculatePropertyValue(Tile tile, int price) {
        TerritorySpec spec = TerritoryCatalog.getSpecForPosition(tile.getPosition());
        if (spec == null) return 0.0;
        
        double baseValue = 1.0;
        
        // Factor in tile type
        switch (tile.getType()) {
            case SAFEHAVEN:
                baseValue = 2.0; // High value for safe havens
                break;
            case TERRITORY:
                baseValue = 1.5;
                break;
            case RESOURCE:
                baseValue = 1.3; // Good value for resources
                break;
            case SPECIAL:
                baseValue = 0.8; // Lower value for special tiles
                break;
            case QUARANTINE:
                baseValue = 0.5; // Low value for quarantine
                break;
            case ZOMBIE:
                baseValue = 0.3; // Very low value for zombie tiles
                break;
            case FREE:
                baseValue = 0.1; // Minimal value for free tiles
                break;
            case CHECKPOST:
                baseValue = 0.6; // Medium value for checkposts
                break;
            case START:
                baseValue = 0.0; // No value for start tile
                break;
            case SCENARIO:
                baseValue = 1.0; // Neutral value for scenarios
                break;
        }
        
        // Factor in strategy
        switch (strategy) {
            case AGGRESSIVE:
                baseValue *= spec.perRoundAttack[0] > 0 ? 1.5 : 0.8;
                break;
            case DEFENSIVE:
                baseValue *= spec.perRoundMedicine[0] > 0 ? 1.5 : 0.8;
                break;
            case ECONOMIC:
                baseValue *= 1.2; // Always good for economic strategy
                break;
            case BALANCED:
                baseValue *= 1.0;
                break;
        }
        
        // Factor in current money (more money = more willing to buy)
        double moneyFactor = Math.min(2.0, (double) getMoney() / 2000.0);
        
        return baseValue * moneyFactor;
    }
    
    private double calculateUpgradeValue(Tile tile, int cost) {
        TerritorySpec spec = TerritoryCatalog.getSpecForPosition(tile.getPosition());
        if (spec == null) return 0.0;
        
        int currentLevel = tile.getUpgradeLevel();
        if (currentLevel >= 3) return 0.0;
        
        // Calculate benefit of upgrade
        double attackBenefit = spec.perRoundAttack[currentLevel + 1] - spec.perRoundAttack[currentLevel];
        double medicineBenefit = spec.perRoundMedicine[currentLevel + 1] - spec.perRoundMedicine[currentLevel];
        
        double totalBenefit = attackBenefit + medicineBenefit;
        double costRatio = (double) cost / getMoney();
        
        return totalBenefit / (1.0 + costRatio);
    }
    
    private double calculateAttackValue(Player target) {
        double healthRatio = (double) target.getHealth() / target.getMaxHealth();
        double attackRatio = (double) getAttack() / target.getAttack();
        
        // More likely to attack if target is weak or we're stronger
        double baseValue = healthRatio * attackRatio;
        
        // Factor in strategy
        switch (strategy) {
            case AGGRESSIVE:
                return baseValue * 1.5;
            case DEFENSIVE:
                return baseValue * 0.5;
            case ECONOMIC:
                return baseValue * 0.8;
            case BALANCED:
                return baseValue;
        }
        
        return baseValue;
    }
    
    private double calculateLabCureValue(int cost) {
        double costRatio = (double) cost / getMoney();
        double healthRatio = (double) getHealth() / getMaxHealth();
        
        // More valuable if we have money and good health
        return (1.0 - costRatio) * healthRatio;
    }
    
    // Threshold getters based on strategy
    private double getBuyThreshold() {
        switch (strategy) {
            case AGGRESSIVE: return 0.6;
            case DEFENSIVE: return 0.7;
            case ECONOMIC: return 0.5;
            case BALANCED: return 0.65;
            default: return 0.65;
        }
    }
    
    private double getUpgradeThreshold() {
        switch (strategy) {
            case AGGRESSIVE: return 0.5;
            case DEFENSIVE: return 0.6;
            case ECONOMIC: return 0.4;
            case BALANCED: return 0.55;
            default: return 0.55;
        }
    }
    
    private double getMedicineThreshold() {
        switch (strategy) {
            case AGGRESSIVE: return 0.3; // Use medicine early
            case DEFENSIVE: return 0.6; // Use medicine more conservatively
            case ECONOMIC: return 0.4;
            case BALANCED: return 0.5;
            default: return 0.5;
        }
    }
    
    private double getAttackThreshold() {
        switch (strategy) {
            case AGGRESSIVE: return 0.4; // Attack more often
            case DEFENSIVE: return 0.8; // Attack rarely
            case ECONOMIC: return 0.6;
            case BALANCED: return 0.6;
            default: return 0.6;
        }
    }
    
    private double getLabCureThreshold() {
        switch (strategy) {
            case AGGRESSIVE: return 0.7;
            case DEFENSIVE: return 0.5; // More likely to fund cure
            case ECONOMIC: return 0.8;
            case BALANCED: return 0.6;
            default: return 0.6;
        }
    }
    
    /**
     * Get a random AI strategy for variety
     */
    public static AIStrategy getRandomStrategy() {
        AIStrategy[] strategies = AIStrategy.values();
        return strategies[new Random().nextInt(strategies.length)];
    }
    
    /**
     * Get AI strategy name for display
     */
    public String getStrategyName() {
        return strategy.toString().toLowerCase();
    }
    
    // Static methods for AI Controller functionality
    private static boolean isProcessingAI = false;
    
    /**
     * Process AI turn for the current AI player
     * This method handles all AI decision-making during their turn
     */
    public static void processAITurn(Player aiPlayer) {
        if (!aiPlayer.isAI() || isProcessingAI) {
            return;
        }
        
        isProcessingAI = true;
        System.out.println("🤖 AiController processing turn for " + aiPlayer.getName());
        
        // Process AI decisions immediately
        processAIDecisions(aiPlayer);
        isProcessingAI = false;
    }
    
    /**
     * Process all AI decisions for the current turn
     */
    private static void processAIDecisions(Player aiPlayer) {
        if (!(aiPlayer instanceof AIPlayer)) {
            System.err.println("Player " + aiPlayer.getName() + " is marked as AI but not an AIPlayer instance");
            return;
        }
        
        AIPlayer ai = (AIPlayer) aiPlayer;
        
        // 1. Check if AI should use medicine
        if (ai.shouldUseMedicine()) {
            ai.useMedicine();
            System.out.println("🤖 " + ai.getName() + " used medicine");
        }
        
        // 2. Check for any owned tiles that can be upgraded
        checkForUpgrades(ai);
        
        // 3. Check for any special AI actions (like funding lab cure)
        checkSpecialActions(ai);
        
        System.out.println("🤖 " + ai.getName() + " finished AI decisions");
    }
    
    /**
     * Check if AI should upgrade any of their owned tiles
     */
    private static void checkForUpgrades(AIPlayer ai) {
        java.util.List<Tile> ownedTiles = getOwnedTiles(ai);
        
        for (Tile tile : ownedTiles) {
            if (tile.getUpgradeLevel() < 3) { // Max level is 3
                TerritorySpec spec = TerritoryCatalog.getSpecForPosition(tile.getPosition());
                if (spec != null) {
                    int nextLevel = tile.getUpgradeLevel() + 1;
                    int upgradeCost = spec.upgradeCosts[nextLevel - 1];
                    
                    if (ai.shouldUpgradeProperty(tile, upgradeCost)) {
                        // Perform upgrade
                        ai.spendMoney(upgradeCost);
                        tile.setUpgradeLevel(nextLevel);
                        ai.increaseAttack(spec.perRoundAttack[nextLevel]);
                        if (spec.perRoundMedicine[nextLevel] > 0) {
                            ai.addMedicine(spec.perRoundMedicine[nextLevel]);
                        }
                        
                        // Add to player history
                        ai.upgradeTile(tile, upgradeCost);
                        
                        System.out.println("🤖 " + ai.getName() + " upgraded " + tile.getName() + " to level " + nextLevel);
                        GameState.notifyUpdate();
                    }
                }
            }
        }
    }
    
    /**
     * Check for special AI actions like funding lab cure
     */
    private static void checkSpecialActions(AIPlayer ai) {
        // Check if AI should fund lab cure
        if (!ai.isLabCureFunded() && ai.getMoney() >= 500) {
            if (ai.shouldFundLabCure(500)) {
                ai.spendMoney(500);
                ai.setLabCureFunded(true);
                System.out.println("🤖 " + ai.getName() + " funded lab cure research");
                GameState.notifyUpdate();
            }
        }
    }
    
    /**
     * Get all tiles owned by the specified player
     */
    private static java.util.List<Tile> getOwnedTiles(Player player) {
        java.util.List<Tile> ownedTiles = new java.util.ArrayList<>();
        
        for (Tile tile : BoardRegistry.tiles().values()) {
            if (tile.getOwner() != null && tile.getOwner().getName().equals(player.getName())) {
                ownedTiles.add(tile);
            }
        }
        
        return ownedTiles;
    }
    
    /**
     * Make AI decision for buying a tile
     * This is called from TileEventManager when AI lands on a purchasable tile
     */
    public static boolean shouldAIBuyTile(AIPlayer ai, Tile tile, int cost, int influenceRequired, int attackRequired) {
        if (!ai.isAI()) {
            return false;
        }
        
        // Check if AI meets requirements
        if (ai.getInfluence() < influenceRequired || ai.getAttack() < attackRequired || ai.getMoney() < cost) {
            return false;
        }
        
        // Use AI's decision-making logic
        return ai.shouldBuyProperty(tile, cost);
    }
    
    /**
     * Make AI decision for buying Safe Haven
     */
    public static boolean shouldAIBuySafeHaven(AIPlayer ai, Tile tile, int cost, int influenceRequired) {
        if (!ai.isAI()) {
            return false;
        }
        
        if (ai.getInfluence() < influenceRequired || ai.getMoney() < cost) {
            return false;
        }
        
        // Safe havens are generally valuable for AI
        return ai.shouldBuyProperty(tile, cost);
    }
    
    /**
     * Make AI decision for trading at Safe Haven
     */
    public static boolean shouldAITrade(AIPlayer ai) {
        if (!ai.isAI()) {
            return false;
        }
        
        // AI should trade if they have low medicine or money for upgrades
        return ai.getMedicine() < 3 || (ai.getMoney() >= 100 && ai.getAttack() < 200);
    }
    
    /**
     * Make AI decision for paying bribe at checkpost
     */
    public static boolean shouldAIPayBribe(AIPlayer ai) {
        if (!ai.isAI()) {
            return false;
        }
        
        // AI should pay bribe if they have enough resources
        return ai.getMoney() >= 150 && ai.getMedicine() >= 1;
    }
    
    /**
     * Check if AI controller is currently processing
     */
    public static boolean isProcessing() {
        return isProcessingAI;
    }
}
