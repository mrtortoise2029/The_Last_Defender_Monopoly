package Ui;

import javafx.scene.control.Alert;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EventManager {

    private static final List<EventData> events = new ArrayList<>();

    static {
        // GOOD EVENTS
        events.add(new EventData("Abandoned Car", "You found a stash of cash in an abandoned car.", +50, 0, 0, 0, 0));
        events.add(new EventData("Medical Crate", "You discovered some medicine supplies.", 0, +2, 0, 0, 0));
        events.add(new EventData("Weapon Stash", "You found working weapons.", 0, 0, +5, 0, 0));
        events.add(new EventData("Zombie Camp Raid", "You raided a zombie camp for loot.", +100, +1, 0, 0, 0));
        events.add(new EventData("Helping Survivors", "You helped survivors and gained reputation.", 0, 0, 0, +5, 0));
        events.add(new EventData("Police Station Find", "You scavenged a police station for resources.", +75, 0, +3, 0, 0));

        // BAD EVENTS
        events.add(new EventData("Zombie Ambush", "You were attacked by zombies!", 0, 0, 0, 0, -40));
        events.add(new EventData("Spoiled Medicine", "Your medicine stash went bad.", 0, -1, 0, 0, 0));
        events.add(new EventData("Bandit Toll", "Bandits robbed you at gunpoint.", -100, 0, 0, 0, 0));
        events.add(new EventData("Broken Weapon", "Your weapon broke during combat.", 0, 0, -5, 0, 0));
    }

    public static void triggerRandomEvent(Player player) {
        Random random = new Random();
        EventData event = events.get(random.nextInt(events.size()));

        // Apply effects
        if (event.getMoneyChange() != 0) player.addMoney(event.getMoneyChange());
        if (event.getMedicineChange() != 0) player.addMedicine(event.getMedicineChange());
        if (event.getAttackChange() != 0) player.increaseAttack(event.getAttackChange());
        if (event.getInfluenceChange() != 0) player.increaseInfluence(event.getInfluenceChange());
        if (event.getHealthChange() != 0) player.takeDamage(-event.getHealthChange()); // negative = damage

        // Show result
        Alert result = new Alert(Alert.AlertType.INFORMATION);
        result.setTitle(event.getName());
        result.setHeaderText("Event: " + event.getName());
        result.setContentText(event.getDescription() + "\n\n" +
                summaryOfChanges(event));
        result.showAndWait();
    }

    private static String summaryOfChanges(EventData e) {
        StringBuilder sb = new StringBuilder();
        if (e.getMoneyChange() != 0) sb.append((e.getMoneyChange() > 0 ? "+" : "")).append(e.getMoneyChange()).append(" money\n");
        if (e.getMedicineChange() != 0) sb.append((e.getMedicineChange() > 0 ? "+" : "")).append(e.getMedicineChange()).append(" medicine\n");
        if (e.getAttackChange() != 0) sb.append((e.getAttackChange() > 0 ? "+" : "")).append(e.getAttackChange()).append(" attack\n");
        if (e.getInfluenceChange() != 0) sb.append((e.getInfluenceChange() > 0 ? "+" : "")).append(e.getInfluenceChange()).append(" influence\n");
        if (e.getHealthChange() != 0) sb.append((e.getHealthChange() > 0 ? "+" : "")).append(e.getHealthChange()).append(" health\n");
        return sb.toString();
    }
}
