package Ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Objects;

public class LobbyController {



    // Menus
    @FXML private VBox mainMenu;
    @FXML private VBox startSubMenu;
    @FXML private VBox aiSubMenu;
    @FXML private VBox playerSelectMenu;
    @FXML private VBox coopSubMenu;
    @FXML private VBox settingsMenu;

    // Main menu buttons
    @FXML private Button btnStart;
    @FXML private Button btnLoad;
    @FXML private Button btnSettings;
    @FXML private Button btnExit;

    // Start submenu
    @FXML private Button btnAI;
    @FXML private Button btnCoop;
    @FXML private Button btnPrevious1;

    // AI submenu
    @FXML private Button btnSelectPlayer;
    @FXML private Button btnPrevious2;

    //select player sub menu
    @FXML private Button btn2Players;
    @FXML private Button btn3Players;
    @FXML private Button btn4Players;
    @FXML private Button btnBackFromPlayerSelect;



    // Co-op submenu
    @FXML private Button btnCreateRoom;
    @FXML private Button btnSearchRoom;
    @FXML private Button btnPrevious3;

    // Settings
    @FXML private CheckBox chkSound;
    @FXML private TextField txtPlayerName;
    @FXML private Button btnSaveSettings;
    @FXML private Button btnPrevious4;

    // Waiting submenu
    @FXML private VBox waitingSubMenu;
    @FXML private Label lblWaitingMessage;
    @FXML private Button btnCancelWaiting;

    // Data
    private String playerName = "Player1";
    private boolean soundEnabled = true;

    @FXML
    private void initialize() {
        // Initialize audio manager with sound setting
        AudioManager.get().setEnabled(chkSound.isSelected());
        
        // Try to start menu music, but don't let it crash the application
        try {
            AudioManager.get().playMenuLoop();
        } catch (Exception e) {
            System.out.println("Audio system not available, running in silent mode: " + e.getMessage());
        }

        // Auto-enter co-op mode if auto-coop is enabled
        if (GameState.get().isAutoCoop()) {
            if (GameState.get().isHostRole()) {
                // Host: auto-create room
                autoCreateRoom();
            } else {
                // Client: auto-join room
                autoJoinRoom();
            }
        }
        // --- Main menu actions ---
        btnStart.setOnAction(e -> switchMenu(mainMenu, startSubMenu));
        btnLoad.setOnAction(e -> loadPreviousGame());
        btnSettings.setOnAction(e -> switchMenu(mainMenu, settingsMenu));
        btnExit.setOnAction(e -> exitGame());

        // --- Start submenu ---
        btnAI.setOnAction(e -> {
            GameState.get().resetGameState(); // Clear any previous game data
            GameState.get().setAIMode(true);
            GameState.get().setCoopMode(false);
            switchMenu(startSubMenu, aiSubMenu);
        });
        btnCoop.setOnAction(e -> {
            GameState.get().resetGameState(); // Clear any previous game data
            GameState.get().setAIMode(false);
            GameState.get().setCoopMode(true);
            switchMenu(startSubMenu, coopSubMenu);
        });
        btnPrevious1.setOnAction(e -> switchMenu(startSubMenu, mainMenu));

        // --- AI submenu ---
        btnSelectPlayer.setOnAction(event -> switchMenu(aiSubMenu,playerSelectMenu));
        btnBackFromPlayerSelect.setOnAction(e -> switchMenu(playerSelectMenu,aiSubMenu));
        btnPrevious2.setOnAction(e -> switchMenu(aiSubMenu, startSubMenu));

        // Player count selection -> ask for names then start
        btn2Players.setOnAction(e -> promptNamesAndStart(2));
        btn3Players.setOnAction(e -> promptNamesAndStart(3));
        btn4Players.setOnAction(e -> promptNamesAndStart(4));

        // --- Co-op submenu ---
        btnCreateRoom.setOnAction(e -> onCreateRoom());
        btnSearchRoom.setOnAction(e -> onSearchRoom());
        btnPrevious3.setOnAction(e -> switchMenu(coopSubMenu, startSubMenu));

        // --- Settings submenu ---
        btnSaveSettings.setOnAction(e -> {
            playerName = txtPlayerName.getText().isEmpty() ? playerName : txtPlayerName.getText();
            soundEnabled = chkSound.isSelected();
            AudioManager.get().setEnabled(soundEnabled);
            if (soundEnabled && AudioManager.get().isMediaAvailable()) {
                AudioManager.get().playMenuLoop();
            } else if (!soundEnabled) {
                AudioManager.get().stopAll();
            }
            System.out.println("Settings saved: " + playerName + " | Sound: " + soundEnabled);
            switchMenu(settingsMenu, mainMenu);
        });
        btnPrevious4.setOnAction(e -> switchMenu(settingsMenu, mainMenu));

        // --- Waiting submenu ---
        btnCancelWaiting.setOnAction(e -> onCancelWaiting());
    }

    // --- Helper method to switch menus ---
    private void switchMenu(VBox from, VBox to) {
        from.setVisible(false);
        from.setManaged(false);
        to.setVisible(true);
        to.setManaged(true);
    }

    // --- Load previous save ---
    private void loadPreviousGame() {
        GameState.get().loadFromFile();
        loadBoard();
    }


    // --- Load board.fxml ---
    private void loadBoard() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/Ui/Board.fxml")));
            Stage stage = null;
            try { stage = (Stage) mainMenu.getScene().getWindow(); } catch (Throwable ignore) {}
            if (stage == null) {
                // Fallback: try primary stage from any window
                for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                    if (w instanceof Stage) { stage = (Stage) w; break; }
                }
            }

            if (stage == null) {
                // Create a new stage as last resort
                stage = new Stage();
            }

            stage.setScene(new Scene(root, 1100, 900));
            stage.show();
            System.out.println("Board.fxml loaded for player " + playerName);
            if (AudioManager.get().isEnabled() && AudioManager.get().isMediaAvailable()) {
                AudioManager.get().stopAll(); // Stop lobby music
                AudioManager.get().playBoardLoop();
            }
            // Persist playerName globally so BoardController can gate local rolls
            GameState.setPlayerName(playerName);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading Board.fxml");
        }
    }

    // --- Online Co-op: Create/Join room using simple sockets and 4-digit code ---
    private void onCreateRoom() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setHeaderText(null);
        dialog.setTitle("Create Room");
        dialog.setContentText("How many players will play?");
        ButtonType two = new ButtonType("2 Players", ButtonBar.ButtonData.YES);
        ButtonType three = new ButtonType("3 Players", ButtonBar.ButtonData.APPLY);
        ButtonType four = new ButtonType("4 Players", ButtonBar.ButtonData.OK_DONE);
        ButtonType back = new ButtonType("Back", ButtonBar.ButtonData.BACK_PREVIOUS);
        dialog.getButtonTypes().setAll(two, three, four, back);
        var res = dialog.showAndWait();
        if (res.isEmpty() || res.get() == back) return;
        int count = res.get() == four ? 4 : (res.get() == three ? 3 : 2);
        GameState.setCoopPlayerCount(count);

        int code = new java.util.Random().nextInt(10000); // Random 4-digit room code (0000-9999)
        int port = 20000 + code;

        GameState.get().setCoopMode(true);
        GameState.setCoopRoomCode(code);
        GameState.setNetworkHost(true);
        NetworkSync ns = new NetworkSync(true, "0.0.0.0", port);

        // Prompt for host name first
        TextInputDialog nameDialog = new TextInputDialog(playerName);
        nameDialog.setHeaderText(null);
        nameDialog.setTitle("Host Player Name");
        nameDialog.setContentText("Enter your name:");
        var nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty()) return;
        playerName = nameResult.get().trim();
        if (playerName.isEmpty()) playerName = "Host";
        GameState.setPlayerName(playerName);

        // Set up sync
        GameState.setSync(ns);
        loadBoard();
    }

    private void onSearchRoom() {
        TextInputDialog codeDialog = new TextInputDialog("");
        codeDialog.setHeaderText(null);
        codeDialog.setTitle("Join Room");
        codeDialog.setContentText("Enter 4-digit room code:");
        var result = codeDialog.showAndWait();
        if (result.isEmpty()) return;
        String codeStr = result.get().trim();
        if (!codeStr.matches("\\d{4}")) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Invalid code. Please enter exactly 4 digits.");
            err.showAndWait();
            return;
        }
        int port = 20000 + Integer.parseInt(codeStr);

        // Prompt for host IP (default to local for same machine)
        TextInputDialog ipDialog = new TextInputDialog("127.0.0.1");
        ipDialog.setHeaderText(null);
        ipDialog.setTitle("Host IP Address");
        ipDialog.setContentText("Enter the host's IP address:");
        var ipResult = ipDialog.showAndWait();
        if (ipResult.isEmpty()) return;
        String hostIp = ipResult.get().trim();
        if (hostIp.isEmpty()) hostIp = "127.0.0.1";

        // Prompt for client name first
        TextInputDialog nameDialog = new TextInputDialog(playerName);
        nameDialog.setHeaderText(null);
        nameDialog.setTitle("Client Player Name");
        nameDialog.setContentText("Enter your name:");
        var nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty()) return;
        playerName = nameResult.get().trim();
        if (playerName.isEmpty()) playerName = "Client";

        // Start client sync and go to waiting screen
        GameState.get().setCoopMode(true);
        GameState.setNetworkHost(false);
        NetworkSync ns = new NetworkSync(false, hostIp, port);
        final java.util.concurrent.atomic.AtomicBoolean joinAnnounced = new java.util.concurrent.atomic.AtomicBoolean(false);
        ns.subscribe(new GameSync.GameSyncListener() {
            @Override
            public void onUpdate(GameState state) {
                // Notify host once that this client has joined
                if (!joinAnnounced.getAndSet(true)) {
                    ns.broadcastPlayerJoined(playerName);
                }
            }

            @Override
            public void onPlayerJoined(String playerName) {

            }

            @Override
            public void onStartGame() {
                Platform.runLater(() -> {
                    // Client loads board directly (names are synced from host)
                    loadBoard();
                });
            }
        });
        GameState.setSync(ns);
        showWaitingScreen(coopSubMenu, false, 0); // Client doesn't know player count yet
    }

    // --- Prompt for names and initialize GameState ---
    private void promptNamesAndStart(int count) {
        if (GameState.get().isAIMode()) {
            // AI mode: prompt only for human player name, rest are AI
            TextInputDialog dialog = new TextInputDialog(playerName);
            dialog.setHeaderText(null);
            dialog.setTitle("AI Mode - Human Player Name");
            dialog.setContentText("Enter name for the human player (AI will control other players):");
            // Add Back button
            ButtonType backBtn = new ButtonType("Back", ButtonBar.ButtonData.BACK_PREVIOUS);
            dialog.getDialogPane().getButtonTypes().add(backBtn);
            dialog.setResultConverter(button -> {
                if (button == backBtn) return "__BACK__";
                return dialog.getEditor().getText();
            });
            var result = dialog.showAndWait();
            if (result.isPresent() && "__BACK__".equals(result.get())) {
                // Back to player select
                return;
            }
            String humanName = result.isPresent() && !result.get().isBlank() ? result.get().trim() : "Player1";
            GameState.get().resetPlayersForAIMode(humanName, count);
            loadBoard();
        } else if (GameState.get().isCoopMode()) {
            // Co-op mode: prompt for all human player names
            String defaultBase = System.getProperty("user.name", "Player");
            TextInputDialog[] dialogs = new TextInputDialog[count];
            String[] names = new String[count];
            for (int i = 0; i < count; i++) {
                TextInputDialog d = new TextInputDialog(i == 0 ? playerName : defaultBase + (i + 1));
                d.setHeaderText(null);
                d.setTitle("Co-op Mode - Player Names");
                d.setContentText("Enter name for Player " + (i + 1) + " (all players are human):");
                // Add Back button to name prompts
                ButtonType backBtn = new ButtonType("Back", ButtonBar.ButtonData.BACK_PREVIOUS);
                d.getDialogPane().getButtonTypes().add(backBtn);
                d.setResultConverter(button -> {
                    if (button == backBtn) return "__BACK__";
                    return d.getEditor().getText();
                });
                dialogs[i] = d;
            }

            for (int i = 0; i < count; i++) {
                var result = dialogs[i].showAndWait();
                if (result.isPresent() && "__BACK__".equals(result.get())) {
                    // User chose Back during name entry: abort and return without starting
                    return;
                }
                names[i] = result.isPresent() && !result.get().isBlank() ? result.get().trim() : ("Player" + (i + 1));
            }

            java.util.List<String> list = java.util.Arrays.asList(names);
            GameState.get().resetPlayersForCoopMode(list);
            // Do not load board here for co-op, as it's handled by onStartGame
        } else {
            // Default mode: prompt for all names (fallback)
            String defaultBase = System.getProperty("user.name", "Player");
            TextInputDialog[] dialogs = new TextInputDialog[count];
            String[] names = new String[count];
            for (int i = 0; i < count; i++) {
                TextInputDialog d = new TextInputDialog(i == 0 ? playerName : defaultBase + (i + 1));
                d.setHeaderText(null);
                d.setTitle("Player Names");
                d.setContentText("Enter name for Player " + (i + 1) + ":");
                // Add Back button to name prompts
                ButtonType backBtn = new ButtonType("Back", ButtonBar.ButtonData.BACK_PREVIOUS);
                d.getDialogPane().getButtonTypes().add(backBtn);
                d.setResultConverter(button -> {
                    if (button == backBtn) return "__BACK__";
                    return d.getEditor().getText();
                });
                dialogs[i] = d;
            }

            for (int i = 0; i < count; i++) {
                var result = dialogs[i].showAndWait();
                if (result.isPresent() && "__BACK__".equals(result.get())) {
                    // User chose Back during name entry: abort and return without starting
                    return;
                }
                names[i] = result.isPresent() && !result.get().isBlank() ? result.get().trim() : ("Player" + (i + 1));
            }

            java.util.List<String> list = java.util.Arrays.asList(names);
            GameState.get().resetPlayers(list);
            loadBoard();
        }
    }


    // --- Waiting screen methods ---
    private void showWaitingScreen(VBox from, boolean isHost, int playerCount) {
        lblWaitingMessage.setText(isHost ?
            "Waiting for " + (playerCount - 1) + " more player(s) to join...\nRoom Code: " + String.format("%04d", GameState.getCoopRoomCode()) :
            "Waiting for host to start the game...");

        if (!isHost) {
            // Client subscribes to sync for updates
            GameState.getSync().subscribe(new GameSync.GameSyncListener() {
                @Override
                public void onUpdate(GameState state) {}

                @Override
                public void onPlayerJoined(String playerName) {}

                @Override
                public void onStartGame() {
                    Platform.runLater(() -> {
                        // Client loads board directly (names are synced from host)
                        loadBoard();
                    });
                }
            });
        }

        switchMenu(from, waitingSubMenu);
    }

    private void onCancelWaiting() {
        // Cancel and go back to co-op submenu
        switchMenu(waitingSubMenu, coopSubMenu);
    }

    // --- Auto-coop methods ---
    private void autoCreateRoom() {
        GameState.get().resetGameState(); // Clear any previous game data
        GameState.get().setAIMode(false);
        GameState.get().setCoopMode(true);

        int code = 0; // Fixed room code for auto-coop
        int port = 20000 + code;

        GameState.get().setCoopMode(true);
        GameState.setCoopRoomCode(code);
        GameState.setNetworkHost(true);
        NetworkSync ns = new NetworkSync(true, "0.0.0.0", port);

        // Use default names for auto-coop
        playerName = "Host";

        // Set up sync
        GameState.setSync(ns);

        // For auto-coop, subscribe listener immediately to handle early client connections
        if (GameState.get().isAutoCoop()) {
            GameState.getSync().subscribe(new GameSync.GameSyncListener() {
                @Override
                public void onUpdate(GameState state) {}

                @Override
                public void onPlayerJoined(String playerName) {
                    Platform.runLater(() -> {
                        // Auto-start with default names
                        autoStartGame();
                    });
                }

                @Override
                public void onStartGame() {}
            });
        }

        // Go to waiting screen
        showWaitingScreen(coopSubMenu, true, 2); // Host waits for 1 client
    }

    private void autoJoinRoom() {
        GameState.get().resetGameState(); // Clear any previous game data
        GameState.get().setAIMode(false);
        GameState.get().setCoopMode(true);

        int port = 20000; // Fixed port for auto-coop

        // Use default names for auto-coop
        playerName = "Client";

        // Start client sync and go to waiting screen
        GameState.get().setCoopMode(true);
        NetworkSync ns = new NetworkSync(false, "127.0.0.1", port);
        ns.subscribe(new GameSync.GameSyncListener() {
            @Override
            public void onUpdate(GameState state) {
                // Client joined successfully
            }

            @Override
            public void onPlayerJoined(String playerName) {}

            @Override
            public void onStartGame() {
                Platform.runLater(() -> {
                    // Client auto-starts game with default names
                    autoStartGame();
                });
            }
        });
        GameState.setSync(ns);
        showWaitingScreen(coopSubMenu, false, 0); // Client waits for host
    }

    private void autoStartGame() {
        // Auto-start with default names for 2 players
        java.util.List<String> names = java.util.Arrays.asList("Host", "Client");
        GameState.get().resetPlayersForCoopMode(names);
        loadBoard();
    }

    // --- Exit game ---
    private void exitGame() {
        Stage stage = (Stage) btnExit.getScene().getWindow();
        stage.close();
    }
}
