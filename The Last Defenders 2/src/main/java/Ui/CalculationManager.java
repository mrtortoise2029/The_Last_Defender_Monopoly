package Ui;

import java.util.Random;

public class CalculationManager {
    private static final Random random = new Random();

    public static double getScenarioChance(int attack, int influence, int difficulty) {
        // Simple calculation: base chance + attack/influence bonus - difficulty penalty
        double baseChance = 50.0;
        double bonus = (attack + influence) * 0.5;
        double penalty = difficulty * 2.0;
        return Math.max(0.0, Math.min(100.0, baseChance + bonus - penalty));
    }

    public static boolean isScenarioSuccess(int attack, int influence, int difficulty) {
        double chance = getScenarioChance(attack, influence, difficulty);
        return random.nextDouble() * 100 < chance;
    }
}
