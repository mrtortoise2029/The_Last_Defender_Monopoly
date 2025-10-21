package Ui;

public class ScenarioData {
    private final String title;
    private final String description;
    private final int difficulty;
    private final int rewardMoney;
    private final int rewardAttack;
    private final int rewardInfluence;
    private final int rewardMedicine;
    private final int penaltyHealth;
    private final int penaltyAttack;

    public ScenarioData(String title, String description, int difficulty, int rewardMoney, int rewardAttack, int rewardInfluence, int rewardMedicine, int penaltyHealth, int penaltyAttack) {
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.rewardMoney = rewardMoney;
        this.rewardAttack = rewardAttack;
        this.rewardInfluence = rewardInfluence;
        this.rewardMedicine = rewardMedicine;
        this.penaltyHealth = penaltyHealth;
        this.penaltyAttack = penaltyAttack;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getDifficulty() { return difficulty; }
    public int getRewardMoney() { return rewardMoney; }
    public int getRewardAttack() { return rewardAttack; }
    public int getRewardInfluence() { return rewardInfluence; }
    public int getRewardMedicine() { return rewardMedicine; }
    public int getPenaltyHealth() { return penaltyHealth; }
    public int getPenaltyAttack() { return penaltyAttack; }
}
