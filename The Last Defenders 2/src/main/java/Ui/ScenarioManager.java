package Ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ScenarioManager{

    private static final List<ScenarioData> scenarios = new ArrayList<>();
    private static final Random random = new Random();

    static {
        // Add your scenarios
        scenarios.add(new ScenarioData(
                "Mall Ambush",
                "You find a mall filled with supplies, but it’s overrun by zombies!",
                300,
                500, 1, 10, 10,
                100, 5
        ));

        scenarios.add(new ScenarioData(
                "Radio Distress Call",
                "A faint voice calls for help over a radio nearby. Could be a trap...",
                280,
                400, 0, 8, 12,
                80, 4
        ));

        scenarios.add(new ScenarioData(
                "Hospital Raid",
                "You find a hospital storage room locked behind barricades.",
                320,
                300, 3, 12, 8,
                120, 6
        ));

        scenarios.add(new ScenarioData(
                "Military Checkpoint",
                "You approach a deserted checkpoint with working turrets. Risk disabling them?",
                350,
                600, 0, 20, 10,
                150, 10
        ));

        scenarios.add(new ScenarioData(
                "Warehouse Cache",
                "A massive stash of supplies lies inside a warehouse. But it's crawling with infected.",
                400,
                800, 2, 25, 15,
                200, 15
        ));
        scenarios.add(new ScenarioData(
                "Bridge Blockade",
                "A collapsed bridge blocks your path. You can try to clear the debris and loot the wreckage, but it looks unstable.",
                270,
                350, 0, 8, 6,
                80, 5
        ));

        scenarios.add(new ScenarioData(
                "Underground Bunker",
                "You find an old underground bunker with sealed steel doors. Could hold priceless supplies—or a deadly trap.",
                380,
                900, 2, 15, 25,
                160, 8
        ));

        scenarios.add(new ScenarioData(
                "Refugee Convoy",
                "A convoy of survivors is stranded. You can choose to protect them or move on. Helping them may earn loyalty.",
                310,
                500, 1, 10, 20,
                100, 4
        ));

        scenarios.add(new ScenarioData(
                "Quarantine Zone Breach",
                "You stumble into a government quarantine zone. Supplies everywhere—but infected soldiers too.",
                420,
                1000, 3, 25, 30,
                200, 10
        ));

        scenarios.add(new ScenarioData(
                "Supply Train",
                "You hear a distant train horn. A running supply train passes nearby—risk jumping aboard for resources?",
                350,
                800, 2, 18, 15,
                150, 6
        ));


    }

    public static void triggerRandomScenario(Player player) {
        ScenarioData scenario = scenarios.get(random.nextInt(scenarios.size()));
        showScenarioDialog(player, scenario);
    }

    private static void showScenarioDialog(Player player, ScenarioData scenario) {
        double chance = CalculationManager.getScenarioChance(player.getAttack(), player.getInfluence(), scenario.getDifficulty());

        if (player.isAI()) {
            // AI decision: take risk if success chance > 50%, else play safe
            if (chance > 50.0) {
                boolean success = CalculationManager.isScenarioSuccess(player.getAttack(), player.getInfluence(), scenario.getDifficulty());
                if (success) applySuccessOutcome(player, scenario);
                else applyFailureOutcome(player, scenario);
            } else {
                applySafeOutcome(player);
            }
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Scenario Event: " + scenario.getTitle());
        alert.setHeaderText(scenario.getDescription());
        alert.setContentText("Chance of success (risky choice): " + String.format("%.1f", chance) + "%");

        ButtonType takeRisk = new ButtonType("Take Risk");
        ButtonType playSafe = new ButtonType("Play Safe");
        alert.getButtonTypes().setAll(takeRisk, playSafe);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == takeRisk) {
            boolean success = CalculationManager.isScenarioSuccess(player.getAttack(), player.getInfluence(), scenario.getDifficulty());
            if (success) applySuccessOutcome(player, scenario);
            else applyFailureOutcome(player, scenario);
        } else {
            applySafeOutcome(player);
        }
    }

    private static void applySuccessOutcome(Player player, ScenarioData s) {
        player.addMoney(s.getRewardMoney());
        player.addMedicine(s.getRewardMedicine());
        player.increaseAttack(s.getRewardAttack());
        player.increaseInfluence(s.getRewardInfluence());

        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Success!");
        success.setHeaderText("You succeeded in the scenario!");
        success.setContentText(String.format("You gained: +%dM, +%d med, +%d atk, +%d inf",
                s.getRewardMoney(), s.getRewardMedicine(), s.getRewardAttack(), s.getRewardInfluence()));
        success.showAndWait();
    }

    private static void applyFailureOutcome(Player player, ScenarioData s) {
        player.takeDamage(s.getPenaltyHealth());
        player.decreaseAttack(s.getPenaltyAttack());

        Alert failure = new Alert(Alert.AlertType.WARNING);
        failure.setTitle("Failure!");
        failure.setHeaderText("Your risky attempt failed!");
        failure.setContentText(String.format("You lost: -%d health, -%d attack", s.getPenaltyHealth(), s.getPenaltyAttack()));
        failure.showAndWait();
    }

    private static void applySafeOutcome(Player player) {
        player.addMoney(100);
        player.takeDamage(10);

        Alert safe = new Alert(Alert.AlertType.INFORMATION);
        safe.setTitle("Safe Choice");
        safe.setHeaderText("You avoided danger and took a smaller reward.");
        safe.setContentText("You gained 100 money but lost 10 health while escaping.");
        safe.showAndWait();
    }
}