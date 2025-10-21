package Ui;


public class Player implements Runnable {
    // --- Attributes ---
    private String name;
    private int health;
    private int maxHealth;
    private int money;      // Main currency
    private int attack;
    private int influence;
    private int medicine;
    private int pos;     // board position
    private String color; // token color
    private boolean inQuarantine; // for quarantine mechanics
    private boolean labCureFunded;
    private int labPassesRemaining;
    private boolean labCureComplete;
    private boolean isAI;
    private PlayerHistory history;

    // --- Constructor ---
    public Player(String name, String color) {
        this.name = name;
        this.maxHealth = 1500;
        this.health = maxHealth;
        this.money = 1000; // start with $1000
        this.attack = 100;
        this.influence = 0;
        this.medicine = 0;
        // Start players on the Start tile (board index 20)
        this.pos = 20;
        this.color = color;
        this.inQuarantine = false;
        this.labCureFunded = false;
        this.labPassesRemaining = 0;
        this.labCureComplete = false;
        this.isAI = false;
        this.history = new PlayerHistory(this);
    }

    // --- Getters ---
    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getMoney() { return money; }
    public int getAttack() { return attack; }
    public int getInfluence() { return influence; }
    public int getMedicine() { return medicine; }
    public int getPos() { return pos; }
    public String getColor() { return color; }
    public boolean isInQuarantine() { return inQuarantine; }
    public boolean isLabCureFunded() { return labCureFunded; }
    public int getLabPassesRemaining() { return labPassesRemaining; }
    public boolean isLabCureComplete() { return labCureComplete; }
    public boolean isAI() { return isAI; }
    public void setAI(boolean isAI) { this.isAI = isAI; }
    public PlayerHistory getHistory() { return history; }

    // --- Setters ---
    public void setName(String name) { this.name = name; }
    public void setMoney(int money) { this.money = Math.max(0, money); }
    public void setAttack(int attack) { this.attack = Math.max(0, attack); }
    public void setInfluence(int influence) { this.influence = Math.max(0, influence); }
    public void setMedicine(int medicine) { this.medicine = Math.max(0, medicine); }
    public void setPos(int pos) { this.pos = Math.max(0, pos); }
    public void setColor(String color) { this.color = color; }
    public void setInQuarantine(boolean inQuarantine) { this.inQuarantine = inQuarantine; }
    public void setLabCureFunded(boolean funded) { this.labCureFunded = funded; }
    public void setLabPassesRemaining(int passes) { this.labPassesRemaining = Math.max(0, passes); }
    public void setLabCureComplete(boolean complete) { this.labCureComplete = complete; }

    // --- Game logic ---
    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health < 0) this.health = 0;
    }

    public void heal(int amount) {
        this.health += amount;
        if (this.health > this.maxHealth) this.health = this.maxHealth;
    }
    
    public void increaseMaxHealth(int amount) {
        this.maxHealth += amount;
        this.health += amount; // Also increase current health
    }

    public void addMoney(int amount) { 
        this.money += amount; 
        if (amount > 0) {
            history.addEntry(new PlayerHistory.HistoryEntry(
                PlayerHistory.HistoryEntry.Type.INCOME, 
                "Money earned", 
                amount));
        }
    }
    public void spendMoney(int amount) { 
        this.money = Math.max(0, this.money - amount); 
        if (amount > 0) {
            history.addEntry(new PlayerHistory.HistoryEntry(
                PlayerHistory.HistoryEntry.Type.EXPENSE, 
                "Money spent", 
                -amount));
        }
    }
    public void increaseAttack(int amount) { this.attack += amount; }
    public void decreaseAttack(int amount) { this.attack = Math.max(0, this.attack - amount); }
    public void increaseInfluence(int amount) { this.influence += amount; }

    public void addMedicine(int amount) {
        this.medicine += amount;
    }

    public void attackPlayer(Player target) {
        target.takeDamage(this.attack);
        history.addEntry(new PlayerHistory.HistoryEntry(
            PlayerHistory.HistoryEntry.Type.ATTACK, 
            "Attacked " + target.getName(), 
            0));
    }
    
    /**
     * Track tile purchase
     */
    public void buyTile(Tile tile, int cost) {
        spendMoney(cost);
        history.addEntry(new PlayerHistory.HistoryEntry(
            PlayerHistory.HistoryEntry.Type.BOUGHT_TILE, 
            "Bought " + tile.getName() + " (Position " + tile.getPosition() + ")", 
            -cost));
    }
    
    /**
     * Track tile upgrade
     */
    public void upgradeTile(Tile tile, int cost) {
        spendMoney(cost);
        history.addEntry(new PlayerHistory.HistoryEntry(
            PlayerHistory.HistoryEntry.Type.UPGRADED_TILE, 
            "Upgraded " + tile.getName() + " to level " + tile.getUpgradeLevel(), 
            -cost));
    }
    
    /**
     * Track tile sale
     */
    public void sellTile(Tile tile, int price) {
        addMoney(price);
        history.addEntry(new PlayerHistory.HistoryEntry(
            PlayerHistory.HistoryEntry.Type.SOLD_TILE, 
            "Sold " + tile.getName() + " (Position " + tile.getPosition() + ")", 
            price));
    }
    
    /**
     * Track medicine usage
     */
    public boolean useMedicine() {
        if (this.medicine > 0) {
            this.heal(50); // Standard rate: 1 medicine = 50 health
            this.medicine--;
            history.addEntry(new PlayerHistory.HistoryEntry(
                PlayerHistory.HistoryEntry.Type.MEDICINE_USED, 
                "Used medicine (+50 HP)", 
                0));
            return true;
        } else {
            System.out.println(name + " has no medicine!");
            return false;
        }
    }

    @Override
    public void run() {
        // This method is no longer used for automatic money generation
        // AI behavior is now handled by AiController
        // This method is kept for compatibility but does nothing
        System.out.println("Player.run() called for " + name + " - this method is deprecated");
    }

    @Override
    public String toString() {
        return "Player{" +
                "name='" + name + '\'' +
                ", health=" + health + "/" + maxHealth +
                ", money=" + money +
                ", attack=" + attack +
                ", influence=" + influence +
                ", medicine=" + medicine +
                ", pos=" + pos +
                ", color=" + color +
                '}';
    }
}
