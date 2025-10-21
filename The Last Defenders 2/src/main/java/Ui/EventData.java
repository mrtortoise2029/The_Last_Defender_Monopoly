package Ui;

public class EventData {
    private final String name;
    private final String description;
    private final int moneyChange;
    private final int medicineChange;
    private final int attackChange;
    private final int influenceChange;
    private final int healthChange;

    public EventData(String name, String description, int moneyChange, int medicineChange, int attackChange, int influenceChange, int healthChange) {
        this.name = name;
        this.description = description;
        this.moneyChange = moneyChange;
        this.medicineChange = medicineChange;
        this.attackChange = attackChange;
        this.influenceChange = influenceChange;
        this.healthChange = healthChange;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMoneyChange() { return moneyChange; }
    public int getMedicineChange() { return medicineChange; }
    public int getAttackChange() { return attackChange; }
    public int getInfluenceChange() { return influenceChange; }
    public int getHealthChange() { return healthChange; }
}