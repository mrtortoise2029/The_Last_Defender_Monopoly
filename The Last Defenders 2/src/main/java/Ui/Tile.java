package Ui;

public class Tile {
    private TileType type;
    private String name;
    private int position;
    private Player owner;
    private int influenceValue;
    private boolean isOwned;
    private int upgradeLevel;
    
    // Safe Haven specific properties
    private int tradePercentage; // 40% base, up to 60%
    private int medicineReward; // 5 base, up to 8
    private int attackReward; // 12 base, up to 20
    private boolean canSetPrices; // unlocked with high influence

    public Tile(TileType type, String name, int position) {
        this.type = type;
        this.name = name;
        this.position = position;
        this.owner = null;
        this.isOwned = false;
        this.upgradeLevel = 0;
        this.tradePercentage = 40;
        this.medicineReward = 5;
        this.attackReward = 12;
        this.canSetPrices = false;
        
        // Set influence value based on tile type
        switch (type) {
            case START:
            case FREE:
                this.influenceValue = 0;
                break;
            case CHECKPOST:
            case QUARANTINE:
                this.influenceValue = 1;
                break;
            case SAFEHAVEN:
                this.influenceValue = 5;
                break;
            case TERRITORY:
                this.influenceValue = 3;
                break;
            case ZOMBIE:
                this.influenceValue = 0;
                break;
            case RESOURCE:
                this.influenceValue = 2;
                break;
            case SCENARIO:
                this.influenceValue = 4;
                break;
            case SPECIAL:
                this.influenceValue = 6;
                break;
        }
    }

    // Getters and Setters
    public TileType getType() { return type; }
    public String getName() { return name; }
    public int getPosition() { return position; }
    public Player getOwner() { return owner; }
    public int getInfluenceValue() { return influenceValue; }
    public boolean isOwned() { return isOwned; }
    public int getUpgradeLevel() { return upgradeLevel; }
    public int getTradePercentage() { return tradePercentage; }
    public int getMedicineReward() { return medicineReward; }
    public int getAttackReward() { return attackReward; }
    public boolean canSetPrices() { return canSetPrices; }

    public void setOwner(Player owner) { 
        this.owner = owner; 
        this.isOwned = (owner != null);
    }
    
    public void setUpgradeLevel(int level) { 
        this.upgradeLevel = level;
        if (type == TileType.SAFEHAVEN) {
            // Map safe haven trade share to 40/50/60 for levels 0/1/2+ respectively
            if (level <= 0) this.tradePercentage = 40;
            else if (level == 1) this.tradePercentage = 50;
            else this.tradePercentage = 60; // cap at 60%
            // Keep in-haven rewards scaling modestly with level
            this.medicineReward = 5 + Math.min(3, level); // 5..8
            this.attackReward = 12 + Math.min(4, level * 2); // 12..20
        }
    }
    
    public void setCanSetPrices(boolean canSet) { this.canSetPrices = canSet; }
}

