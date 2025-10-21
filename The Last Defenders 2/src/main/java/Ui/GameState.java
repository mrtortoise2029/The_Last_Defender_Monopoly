package Ui;

import com.google.gson.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class GameState {
    private static final GameState INSTANCE = new GameState();
    public static GameState get() { return INSTANCE; }

    public static final int BOARD_SIZE = 11;
    public static final int PERIMETER = 4 * (BOARD_SIZE - 1); // 40

    private final ObservableList<Player> players = FXCollections.observableArrayList();
    private final IntegerProperty currentIndex = new SimpleIntegerProperty(0);
    private final IntegerProperty lastRoll = new SimpleIntegerProperty(0);
    private final IntegerProperty dice1 = new SimpleIntegerProperty(0);
    private final IntegerProperty dice2 = new SimpleIntegerProperty(0);
    private final BooleanProperty startGameEnabled = new SimpleBooleanProperty(false);
    private boolean isAIMode = false;
    private boolean isCoopMode = false;
    private boolean isAutoCoop = false;
    private boolean isHostRole = false;

    private final Random rng = new Random();
    private static final String SAVE_FILE = "savegame.txt";

    // Optional networking sync. When set, state changes can be broadcast to peers.
    private static GameSync sync;
    private static Integer coopRoomCode = null;
    private static boolean isNetworkHost = false;
    private static int coopPlayerCount = 2;
    private static boolean boardLoaded = false;
    private static String playerName = "Player1";

    private GameState() {
        // Clear any previous game data on initialization
        BoardRegistry.clearAll();

        // Default 4 players with distinct colors
        players.add(new Player("Alice",   "#ff3b30")); // red
        players.add(new Player("Bob",     "#34c759")); // green
        players.add(new Player("Charlie", "#007aff")); // blue
        players.add(new Player("Diana",   "#ffcc00")); // yellow
    }

    public ObservableList<Player> players() { return players; }
    public int getCurrentIndex() { return currentIndex.get(); }
    public int getLastRoll() { return lastRoll.get(); }
    public IntegerProperty currentIndexProperty() { return currentIndex; }
    public IntegerProperty lastRollProperty() { return lastRoll; }
    public int getDice1() { return dice1.get(); }
    public IntegerProperty dice1Property() { return dice1; }
    public int getDice2() { return dice2.get(); }
    public IntegerProperty dice2Property() { return dice2; }
    public boolean isStartGameEnabled() { return startGameEnabled.get(); }
    public BooleanProperty startGameEnabledProperty() { return startGameEnabled; }
    public boolean isAIMode() { return isAIMode; }
    public void setAIMode(boolean isAIMode) { this.isAIMode = isAIMode; }
    public boolean isCoopMode() { return isCoopMode; }
    public void setCoopMode(boolean isCoopMode) { this.isCoopMode = isCoopMode; }
    public boolean isAutoCoop() { return isAutoCoop; }
    public void setAutoCoop(boolean isAutoCoop) { this.isAutoCoop = isAutoCoop; }
    public boolean isHostRole() { return isHostRole; }
    public void setHostRole(boolean isHostRole) { this.isHostRole = isHostRole; }

    /**
     * Completely reset the game state - clears all data
     * This should be called when starting a completely new game
     */
    public void resetGameState() {
        BoardRegistry.clearAll();
        // Clear player history for all players
        for (Player player : players) {
            if (player.getHistory() != null) {
                player.getHistory().clear();
            }
        }
        players.clear();
        currentIndex.set(0);
        lastRoll.set(0);
        dice1.set(0);
        dice2.set(0);
        startGameEnabled.set(false);
        isAIMode = false;
        isCoopMode = false;
        isAutoCoop = false;
        isHostRole = false;
    }

    public Player current() { return players.get(getCurrentIndex()); }

    public int roll() {
        int d1 = rng.nextInt(6) + 1; // 1–6
        int d2 = rng.nextInt(6) + 1; // 1–6
        int sum = d1 + d2;           // 2–12
        dice1.set(d1);
        dice2.set(d2);
        lastRoll.set(sum);
        // move current player
        Player p = current();
        int newPos = (p.getPos() + sum) % PERIMETER;
        p.setPos(newPos);
        broadcastIfPresent();
        return sum;
    }

    public void endTurn() {
        // On crossing START, decrement lab passes if cure funded
        Player prev = current();
        currentIndex.set((getCurrentIndex() + 1) % players.size());
        if (prev.isLabCureFunded() && prev.getPos() == 20) {
            int remaining = prev.getLabPassesRemaining();
            if (remaining > 0) {
                prev.setLabPassesRemaining(remaining - 1);
                if (remaining - 1 == 0) {
                    prev.setLabCureComplete(true);
                }
            }
        }
        broadcastIfPresent();
    }

    // Apply per-turn income and bonuses at the start of the current player's turn
    public BonusSummary applyStartOfTurnBonuses() {
        Player p = current();
        int money = 0;
        int attack = 0;
        int med = 0;
        int infl = 0;

        boolean ownRes1 = false, ownRes2 = false, ownRes3 = false; // 21,23,29
        boolean ownCom1 = false, ownCom2 = false, ownCom3 = false, ownCom4 = false; // 32,34,36,39
        boolean ownInd1 = false, ownInd2 = false, ownInd3 = false, ownInd4 = false; // 1,2,4,9

        for (Tile t : BoardRegistry.tiles().values()) {
            if (t.getOwner() != p) continue;
            int pos = t.getPosition();
            TerritorySpec spec = TerritoryCatalog.getSpecForPosition(pos);
            if (spec != null) {
                int lvl = Math.max(0, Math.min(3, t.getUpgradeLevel()));
                attack += spec.perRoundAttack[lvl];
                med += spec.perRoundMedicine[lvl];
            }

            // Safe Haven owner per-round bonus
            if (t.getType() == TileType.SAFEHAVEN) {
                med += 5;
                attack += 10;
            }

            // Track set ownership
            if (pos == 21) ownRes1 = true; else if (pos == 23) ownRes2 = true; else if (pos == 29) ownRes3 = true;
            if (pos == 32) ownCom1 = true; else if (pos == 34) ownCom2 = true; else if (pos == 36) ownCom3 = true; else if (pos == 39) ownCom4 = true;
            if (pos == 1) ownInd1 = true; else if (pos == 2) ownInd2 = true; else if (pos == 4) ownInd3 = true; else if (pos == 9) ownInd4 = true;
        }

        // Set bonuses
        if (ownRes1 && ownRes2 && ownRes3) {
            money += 1500;
            infl += 25;
            med += 1;
        }
        if (ownCom1 && ownCom2 && ownCom3 && ownCom4) {
            med += 1;
            attack += 5;
            infl += 5;
        }
        if (ownInd1 && ownInd2 && ownInd3 && ownInd4) {
            infl += 2;
        }

        if (money != 0) p.addMoney(money);
        if (attack != 0) p.increaseAttack(attack);
        if (med != 0) p.addMedicine(med);
        if (infl != 0) p.increaseInfluence(infl);

        return new BonusSummary(money, attack, med, infl);
    }

    public static class BonusSummary {
        public final int money; public final int attack; public final int medicine; public final int influence;
        public BonusSummary(int money, int attack, int medicine, int influence) {
            this.money = money; this.attack = attack; this.medicine = medicine; this.influence = influence;
        }
        public boolean isEmpty() { return money == 0 && attack == 0 && medicine == 0 && influence == 0; }
    }

    // --- Customization: reset players with provided names (2-4 supported) ---
    public void resetPlayers(java.util.List<String> playerNames) {
        BoardRegistry.clearAll();
        players.clear();
        String[] colors = new String[]{"#ff3b30", "#34c759", "#007aff", "#ffcc00"};
        int count = Math.max(2, Math.min(4, playerNames.size()));
        for (int i = 0; i < count; i++) {
            String name = playerNames.get(i) == null || playerNames.get(i).isBlank()
                    ? ("Player" + (i + 1))
                    : playerNames.get(i).trim();
            Player player = new Player(name, colors[i % colors.length]);
            players.add(player);
        }
        currentIndex.set(0);
        lastRoll.set(0);
        dice1.set(0);
        dice2.set(0);
        broadcastIfPresent();
    }

    public void enableStartGame() {
        startGameEnabled.set(true);
    }

    public void resetPlayersForAIMode(String humanName, int numPlayers) {
        BoardRegistry.clearAll();
        players.clear();
        String[] colors = new String[]{"#ff3b30", "#34c759", "#007aff", "#ffcc00"};
        int count = Math.max(2, Math.min(4, numPlayers));
        String hName = humanName == null || humanName.isBlank() ? "Player1" : humanName.trim();
        Player human = new Player(hName, colors[0]);
        human.setAI(false);
        players.add(human);
        // No background thread for human
        AIPlayer.AIStrategy[] strategies = AIPlayer.AIStrategy.values();
        for (int i = 1; i < count; i++) {
            String aiName = "AI " + strategies[(i - 1) % strategies.length].toString();
            AIPlayer ai = new AIPlayer(aiName, colors[i % colors.length], strategies[(i - 1) % strategies.length]);
            players.add(ai);
            // Start thread for AI player
            new Thread(ai).start();
        }
        currentIndex.set(0);
        lastRoll.set(0);
        dice1.set(0);
        dice2.set(0);
        broadcastIfPresent();
    }

    public void resetPlayersForCoopMode(java.util.List<String> playerNames) {
        BoardRegistry.clearAll();
        players.clear();
        String[] colors = new String[]{"#ff3b30", "#34c759", "#007aff", "#ffcc00"};
        int count = Math.max(2, Math.min(4, playerNames.size()));
        for (int i = 0; i < count; i++) {
            String name = playerNames.get(i) == null || playerNames.get(i).isBlank()
                    ? ("Player" + (i + 1))
                    : playerNames.get(i).trim();
            Player player = new Player(name, colors[i % colors.length]);
            player.setAI(false);
            players.add(player);
        }
        currentIndex.set(0);
        lastRoll.set(0);
        dice1.set(0);
        dice2.set(0);
        broadcastIfPresent();
    }

    // --- Save/Load using JSON but into savegame.txt ---
    public void saveToFile() {
        if (isCoopMode) {
            System.out.println("⏭️ Skipping save: co-op mode active");
            return;
        }
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                        @Override
                        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        }
                    })
                    .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                        @Override
                        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                    })
                    .setPrettyPrinting()
                    .create();

            SaveData data = new SaveData(players, getCurrentIndex(), getLastRoll(), BoardRegistry.tiles());
            gson.toJson(data, writer);
            System.out.println("✅ Game saved to " + SAVE_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFromFile() {
        try (FileReader reader = new FileReader(SAVE_FILE)) {

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                        @Override
                        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        }
                    })
                    .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                        @Override
                        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                    })
                    .create();

            Type type = new TypeToken<SaveData>() {}.getType();
            SaveData data = gson.fromJson(reader, type);

            players.clear();
            players.addAll(data.players);
            currentIndex.set(data.currentIndex);
            lastRoll.set(data.lastRoll);
            // restore tiles/ownership if present
            try {
                if (data.tiles != null) {
                    BoardRegistry.clearAll();
                    for (Tile t : data.tiles.values()) {
                        BoardRegistry.tiles().put(t.getPosition(), t);
                    }
                    // Fix owner references: match by name since Gson creates new Player instances
                    for (Tile t : data.tiles.values()) {
                        if (t.isOwned() && t.getOwner() != null) {
                            String ownerName = t.getOwner().getName();
                            Player matchingPlayer = players.stream()
                                .filter(p -> p.getName().equals(ownerName))
                                .findFirst()
                                .orElse(null);
                            if (matchingPlayer != null) {
                                t.setOwner(matchingPlayer);
                            } else {
                                // If no match, clear ownership
                                t.setOwner(null);
                            }
                        }
                    }
                }
            } catch (Throwable ignore) {}

            System.out.println("✅ Game loaded from " + SAVE_FILE);
        } catch (Exception e) {
            System.out.println("⚠️ No saved game found or failed to load.");
            e.printStackTrace();
        }
    }

    // --- Networking helpers ---
    public static GameSync getSync() {
        return sync;
    }

    public static void setSync(GameSync gameSync) {
        sync = gameSync;
        if (sync != null) {
            sync.subscribe(new GameSync.GameSyncListener() {
                @Override
                public void onUpdate(GameState state) {
                    // Apply incoming state snapshot to local instance
                    if (state != null && state != INSTANCE) {
                        INSTANCE.applyFrom(state);
                    }
                }

                @Override
                public void onPlayerJoined(String playerName) {
                    // Enable the Start Game button for the host
                    INSTANCE.enableStartGame();
                }

                @Override
                public void onStartGame() {
                    // Handle start game event if needed
                }
            });
        }
    }

    public static void setCoopRoomCode(Integer code) { coopRoomCode = code; }
    public static Integer getCoopRoomCode() { return coopRoomCode; }
    public static void setNetworkHost(boolean host) { isNetworkHost = host; }
    public static boolean isNetworkHost() { return isNetworkHost; }
    public static int getCoopPlayerCount() { return coopPlayerCount; }
    public static void setCoopPlayerCount(int count) { coopPlayerCount = count; }
    public static boolean isBoardLoaded() { return boardLoaded; }
    public static void setBoardLoaded(boolean loaded) { boardLoaded = loaded; }
    public static String getPlayerName() { return playerName; }
    public static void setPlayerName(String name) { playerName = name; }

    private void broadcastIfPresent() {
        if (sync != null) {
            sync.broadcast(this);
        }
    }

    // Public notifier for UI/gameplay code to force a sync broadcast
    public static void notifyUpdate() {
        if (sync != null) {
            sync.broadcast(INSTANCE);
        }
    }

    private void applyFrom(GameState other) {
        // Replace core fields from another snapshot
        BoardRegistry.clearAll();
        players.clear();
        players.addAll(other.players);
        currentIndex.set(other.getCurrentIndex());
        lastRoll.set(other.getLastRoll());
        dice1.set(other.getDice1());
        dice2.set(other.getDice2());
        isAIMode = other.isAIMode;
        isCoopMode = other.isCoopMode;
        isAutoCoop = other.isAutoCoop;
        isHostRole = other.isHostRole;
    }

    // --- Network DTO snapshot helpers ---
    public static class GameSnapshot {
        public List<Player> players;
        public int currentIndex;
        public int lastRoll;
        public int dice1;
        public int dice2;
        public java.util.Map<Integer, Tile> tiles;
    }

    public static GameSnapshot createSnapshot() {
        GameSnapshot s = new GameSnapshot();
        s.players = new java.util.ArrayList<>(INSTANCE.players);
        s.currentIndex = INSTANCE.getCurrentIndex();
        s.lastRoll = INSTANCE.getLastRoll();
        s.dice1 = INSTANCE.getDice1();
        s.dice2 = INSTANCE.getDice2();
        s.tiles = new java.util.HashMap<>(BoardRegistry.tiles());
        return s;
    }

    public static void applySnapshot(GameSnapshot s) {
        if (s == null) return;
        BoardRegistry.clearAll();
        INSTANCE.players.clear();
        if (s.players != null) INSTANCE.players.addAll(s.players);
        INSTANCE.currentIndex.set(Math.max(0, Math.min(Math.max(0, INSTANCE.players.size() - 1), s.currentIndex)));
        INSTANCE.lastRoll.set(s.lastRoll);
        INSTANCE.dice1.set(s.dice1);
        INSTANCE.dice2.set(s.dice2);
        if (s.tiles != null) {
            for (var e : s.tiles.entrySet()) {
                BoardRegistry.tiles().put(e.getKey(), e.getValue());
            }
            // Fix owner references: match by name since Gson creates new Player instances
            for (Tile t : s.tiles.values()) {
                if (t.isOwned() && t.getOwner() != null) {
                    String ownerName = t.getOwner().getName();
                    Player matchingPlayer = INSTANCE.players.stream()
                        .filter(p -> p.getName().equals(ownerName))
                        .findFirst()
                        .orElse(null);
                    if (matchingPlayer != null) {
                        t.setOwner(matchingPlayer);
                    } else {
                        // If no match, clear ownership
                        t.setOwner(null);
                    }
                }
            }
        }
    }

    // --- DTO for saving ---
    private static class SaveData {
        List<Player> players;
        int currentIndex;
        int lastRoll;
        java.util.Map<Integer, Tile> tiles;

        SaveData(List<Player> players, int currentIndex, int lastRoll, java.util.Map<Integer, Tile> tiles) {
            this.players = players;
            this.currentIndex = currentIndex;
            this.lastRoll = lastRoll;
            this.tiles = tiles;
        }
    }
}
