package Ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class BoardController {

    @FXML private GridPane boardGrid;
    @FXML private Button btnBackToLobby, btnRollDice, btnEndTurn;

    @FXML private Label p1Name, p1Stats, p1Pos, p1Tiles;
    @FXML private Label p2Name, p2Stats, p2Pos, p2Tiles;
    @FXML private Label p3Name, p3Stats, p3Pos, p3Tiles;
    @FXML private Label p4Name, p4Stats, p4Pos, p4Tiles;
    @FXML private TitledPane p1Pane, p2Pane, p3Pane, p4Pane;
    @FXML private Button p1HistoryBtn, p2HistoryBtn, p3HistoryBtn, p4HistoryBtn;
    @FXML private Label turnLabel, lastRollLabel, timerLabel;


    private final Map<String, ImageView> tokens = new HashMap<>();
    private Timer turnTimer;
    private int timeRemaining = 60; // 1 minute
    private boolean hasRolled = false;
    private boolean isAdvancingTurn = false; // guard to serialize turn transitions
    private boolean aiActionScheduled = false; // ensure single AI action per AI turn
    private int lastKnownCurrentIndex = -1; // track turn changes for UI enabling
    private int lastShownRoll = -1; // to detect new rolls from sync for client-side animation
    private Timer inactivityTimer; // 5s inactivity auto-end turn

    private static final int N = 11;
    private static final int PERIM = 40;
    private static final int TURN_TIME_SECONDS = 60;
    private AudioClip diceSound;
    private AudioClip footstepSound;
    private final Map<Integer, javafx.scene.shape.Polygon> ownershipIndicators = new HashMap<>();

    @FXML
    public void initialize() {
        // Build board
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(65, 65);
                cell.setStyle("-fx-border-color: black; -fx-background-color: #1b1b1b;");
                if (isEdge(r, c)) {
                    String label = tileLabelFor(r, c);
                    Button tile = new Button(label);
                    tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    tile.setStyle("-fx-background-color: " + tileColorFor(r, c) +
                            "; -fx-border-color: black; -fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: white;");
                    // Apply background images per tile label
                    if (label != null) {
                        String ll = label.toLowerCase();
                        String img = null;
                        if (ll.contains("abandoned house")) img = "/images/abhouse.jpg";
                        else if (ll.equals("airport")) img = "/images/Airport.jpg";
                        else if (ll.contains("apartment")) img = "/images/apartment.jpg";
                        else if (ll.contains("check-post") || ll.contains("checkpost")) img = "/images/Checkpost.jpg";
                        else if (ll.equals("special")) img = "/images/Special.jpg";
                        else if (ll.equals("events") || ll.equals("event")) img = "/images/Event.jpg";
                        else if (ll.contains("gas station")) img = "/images/gasstation.jpg";
                        else if (ll.contains("hardware")) img = "/images/Hardwarest.jpg";
                        else if (ll.contains("hospital")) img = "/images/Hospital.jpg";
                        else if (ll.contains("motel")) img = "/images/motelshelter.jpg";
                        else if (ll.contains("power plant")) img = "/images/powerplant.jpg";
                        else if (ll.contains("quarantine")) img = "/images/quraintine.jpg";
                        else if (ll.contains("radio tower")) img = "/images/RadioTower.jpg";
                        else if (ll.contains("research lab")) img = "/images/ReachersLab.jpg";
                        else if (ll.equals("resource") || ll.contains("resources")) img = "/images/Resources.jpg";
                        else if (ll.equals("reward")) img = "/images/Reward.jpg";
                        else if (ll.contains("safe haven")) img = "/images/safehaven.jpg";
                        else if (ll.equals("scenario")) img = "/images/Senario.jpg";
                        else if (ll.equals("supermarket")) img = "/images/spuermarket.jpg";
                        else if (ll.equals("super market 1")) img = "/images/supermarket1.jpg";
                        else if (ll.equals("super market 2")) img = "/images/supermarket2.jpg";
                        else if (ll.equals("start")) img = "/images/Start.jpg";
                        else if (ll.contains("warehouse")) img = "/images/warehouse1.jpg";
                        else if (ll.contains("factory")) img = "/images/factory.jpg";
                        else if (ll.equals("territory")) img = "/images/territory.jpg";
                        else if (ll.equals("zombie")) img = "/images/Zombie.jpg";
                        else if (ll.equals("free")) img = "/images/free.jpg";

                        if (img != null) {
                            tile.setStyle(tile.getStyle() +
                                    " -fx-background-image: url('" + img + "');" +
                                    " -fx-background-size: cover;" +
                                    " -fx-background-position: center;" +
                                    " -fx-background-repeat: no-repeat;");
                        }
                    }
                    // Default behavior without custom image for zombie tiles
                    tile.setOnAction(e -> showTileInfo(tile.getText()));
                    cell.getChildren().add(tile);
                }
                boardGrid.add(cell, c, r);
            }
        }

        // Add center background image spanning interior (ignores outer ring cells)
        StackPane centerBg = new StackPane();
        centerBg.setStyle("-fx-background-image: url('/images/bgboard.jpeg');"
                + " -fx-background-size: cover;"
                + " -fx-background-position: center;"
                + " -fx-background-repeat: no-repeat;");
        boardGrid.add(centerBg, 1, 1, N - 2, N - 2);

        // Tokens for players
        for (Player p : GameState.get().players()) {
            ImageView token = createPlayerToken(p);
            tokens.put(p.getName(), token);
            placeToken(p, p.getPos());
        }

        updatePlayerPanels();
        updateEndTurnEnabledState();
        GameState.setBoardLoaded(true);
        // Prevent dice animation on board load if there's an existing roll
        lastShownRoll = GameState.get().getLastRoll();
        // If host in co-op, display room code once on board entry and pause game
        if (GameState.get().isCoopMode() && GameState.isNetworkHost() && GameState.getCoopRoomCode() != null) {
			// Ensure host board shows placeholder players while waiting
			if (GameState.get().players().isEmpty()) {
				java.util.List<String> waitingNames = java.util.Arrays.asList(
					GameState.getPlayerName() == null || GameState.getPlayerName().isBlank() ? "Host" : GameState.getPlayerName(),
					"Waiting...");
				GameState.get().resetPlayers(waitingNames);
				updatePlayerPanels();
				// Recreate tokens for placeholders
                tokens.clear();
                for (Player p : GameState.get().players()) {
                    ImageView token = createPlayerToken(p);
                    tokens.put(p.getName(), token);
                    placeToken(p, p.getPos());
                }
			}
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Room Code");
            info.setHeaderText("Share this code to let others join");
            info.setContentText("Room Code: " + String.format("%04d", GameState.getCoopRoomCode()));
            Platform.runLater(info::showAndWait);
            // Pause game
			btnRollDice.setDisable(true);
            // Subscribe to sync for player joined
            GameState.getSync().subscribe(new GameSync.GameSyncListener() {
                @Override
                public void onUpdate(GameState state) {}

                @Override
                public void onPlayerJoined(String playerName) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Player Joined");
                        alert.setHeaderText(playerName + " joined the game.");
                        alert.setContentText("Start the game now?");
                        ButtonType yes = new ButtonType("Yes");
                        ButtonType no = new ButtonType("No");
                        alert.getButtonTypes().setAll(yes, no);
                        var result = alert.showAndWait();
                        if (result.isPresent() && result.get() == yes) {
                            GameState.getSync().broadcastStartGame();
							// Use host's chosen name from GameState and the joined player's name
							java.util.List<String> names = java.util.Arrays.asList(GameState.getPlayerName(), playerName);
                            GameState.get().resetPlayersForCoopMode(names);
                            updatePlayerPanels();
                            // Start the game
                            btnRollDice.setDisable(false);
                            startTurnTimer();
                        }
                    });
                }

                @Override
                public void onStartGame() {}
            });
        }
		updateHeader();
		updateRollDiceEnabledState();
        // Apply start-of-turn bonuses for the opening player
        if (!GameState.get().players().isEmpty()) {
            var bonus = GameState.get().applyStartOfTurnBonuses();
            if (!bonus.isEmpty()) {
                if (!GameState.get().current().isAI()) {
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Start of Turn Bonuses");
                    a.setHeaderText("Income & bonuses applied to " + GameState.get().current().getName());
                    a.setContentText("Money: +" + bonus.money + ", Attack: +" + bonus.attack + ", Med: +" + bonus.medicine + ", Influence: +" + bonus.influence);
                    // Ensure dialogs are shown on the FX thread after layout/animations have finished
                    Platform.runLater(a::showAndWait);
                }
                updatePlayerPanels();
            }
        }
        refreshOwnershipVisuals();
        if (GameState.get().isAIMode() && GameState.get().current().isAI()) {
            // Process AI turn first, then auto roll
            AIPlayer.processAITurn(GameState.get().current());
            // brief delay so players can see whose turn it is
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() { Platform.runLater(() -> autoRollAndHandle()); }
            }, 800);
        } else {
            startTurnTimer();
        }




        // Preload dice rolling sound
        diceSound = loadDiceSound();
        // Preload footstep sound
        footstepSound = loadFootstepSound();

		btnRollDice.setOnAction(e -> {
			if (!hasRolled) {
				// Extra gate: only allow local current player to roll in co-op
				if (GameState.get().isCoopMode()) {
					Player current = GameState.get().current();
					String localName = GameState.getPlayerName();
					if (current == null || !current.getName().equals(localName)) {
						updateRollDiceEnabledState();
						return;
					}
				}
				Player currentPlayer = GameState.get().current();
				int roll = GameState.get().roll();
                // Show dice results with player name
                String playerName = currentPlayer.getName();
                lastRollLabel.setText(playerName + " rolled: " + roll +
                        " (" + GameState.get().getDice1() + "+" + GameState.get().getDice2() + ")");
                showDoubleDiceRollAnimation(GameState.get().getDice1(), GameState.get().getDice2());
                lastShownRoll = roll; // Prevent duplicate animation in sync listener
                // Animate movement from previous position to new position
                int newPos = currentPlayer.getPos();
                int startPos = (newPos - roll) % PERIM;
                if (startPos < 0) startPos += PERIM;
                hasRolled = true;
                btnRollDice.setDisable(true);
                updateEndTurnEnabledState();
                animatePlayerMovement(currentPlayer, startPos, newPos, () -> {
                    // After animation completes, update panels and handle tile
                    updatePlayerPanels();
                    int position = currentPlayer.getPos();
                    int[] rc = indexToRC(position);
                    String tileType = tileLabelFor(rc[0], rc[1]);
                    // Broadcast move before resolving tile so peers see position immediately
                    GameState.notifyUpdate();
                    // Install a one-shot hook:
                    // - For humans in co-op: advance immediately after dialogs close and reset timer
                    // - For AI-mode human turn end: advance then trigger AI
                    if (!currentPlayer.isAI()) {
                        if (GameState.get().isAIMode()) {
                            TileEventManager.setAfterDialogHook(() -> {
                                Platform.runLater(() -> {
                                    safeAdvanceToNextTurn();
                                    triggerAIIfNeeded();
                                });
                            });
                        } else {
                            // Co-op mode: advance immediately to next player after tile dialogs
                            TileEventManager.setAfterDialogHook(() -> Platform.runLater(() -> {
                                safeAdvanceToNextTurn();
                            }));
                        }
                    }
                    showTileInfo(tileType);
                    // Broadcast after tile resolution to sync ownership/money/etc.
                    GameState.notifyUpdate();
                    // Force UI update after sync to ensure panels show latest data
                    updatePlayerPanels();
                });
            }
        });

		btnBackToLobby.setOnAction(e -> goBackToLobby());

		btnEndTurn.setOnAction(e -> {
			if (!btnEndTurn.isDisabled()) {
				safeAdvanceToNextTurn();
			}
		});

		// Subscribe to sync updates to refresh UI on clients and host
		if (GameState.getSync() != null) {
			GameState.getSync().subscribe(new GameSync.GameSyncListener() {
				@Override
				public void onUpdate(GameState state) {
					Platform.runLater(() -> {
                        // Sync tokens: remove stale nodes, update or create tokens per player
                        java.util.List<Player> snapshotPlayers = new java.util.ArrayList<>(GameState.get().players());
                        java.util.Set<String> currentNames = new java.util.HashSet<>();
                        for (Player p : snapshotPlayers) currentNames.add(p.getName());
                        // Remove tokens for players no longer present
                        java.util.Iterator<Map.Entry<String, ImageView>> it = tokens.entrySet().iterator();
						while (it.hasNext()) {
                            Map.Entry<String, ImageView> e = it.next();
                            if (!currentNames.contains(e.getKey())) {
                                removeTokenNodeById("token-" + e.getKey());
								it.remove();
							}
						}
                        // Ensure a single token per current player and place it
                        for (Player p : snapshotPlayers) {
                            ImageView token = tokens.get(p.getName());
							if (token == null) {
								token = createPlayerToken(p);
                                token.setId("token-" + p.getName());
                                tokens.put(p.getName(), token);
							}
							// Remove any stale node before placing
							removeTokenNodeById("token-" + p.getName());
							placeToken(p, p.getPos());
						}
						updatePlayerPanels();
						refreshOwnershipVisuals();
						updateHeader();
						updateRollDiceEnabledState();
						updateEndTurnEnabledState();
						// Handle turn change first to avoid spurious dice animation at end of turn
						int idx = GameState.get().currentIndexProperty().get();
						boolean turnChanged = (idx != lastKnownCurrentIndex);
						if (turnChanged) {
							lastKnownCurrentIndex = idx;
							hasRolled = false;
							// Clear any pending inactivity timer on turn change
							if (inactivityTimer != null) { inactivityTimer.cancel(); inactivityTimer = null; }
							// Swallow current lastRoll so we don't animate due to a stale value
							lastShownRoll = GameState.get().getLastRoll();
							
							// Task 3: Reset timer for new turn in co-op mode
							if (GameState.get().isCoopMode()) {
								timeRemaining = TURN_TIME_SECONDS;
								updateTimerDisplay();
							}
							hasRolled = false;
							updateRollDiceEnabledState();
							updateEndTurnEnabledState();
						} else {
							// If we received a new roll during the same turn, show dice animation
							int lr = GameState.get().getLastRoll();
							if (lr > 0 && lr != lastShownRoll) {
								lastShownRoll = lr;
								showDoubleDiceRollAnimation(GameState.get().getDice1(), GameState.get().getDice2());
							}
						}
					});
				}

				@Override
				public void onPlayerJoined(String playerName) {}

				@Override
				public void onStartGame() {}
			});
		}

        // History button actions
        if (p1HistoryBtn != null) p1HistoryBtn.setOnAction(e -> showPlayerHistory(GameState.get().players().get(0)));
        if (p2HistoryBtn != null) p2HistoryBtn.setOnAction(e -> showPlayerHistory(GameState.get().players().get(1)));
        if (p3HistoryBtn != null) p3HistoryBtn.setOnAction(e -> showPlayerHistory(GameState.get().players().get(2)));
        if (p4HistoryBtn != null) p4HistoryBtn.setOnAction(e -> showPlayerHistory(GameState.get().players().get(3)));
    }

	private void updateRollDiceEnabledState() {
		boolean enable;
		if (GameState.get().isCoopMode() && GameState.getSync() != null) {
			String localName = GameState.getPlayerName();
			Player currentPlayer = GameState.get().players().isEmpty() ? null : GameState.get().current();
			enable = currentPlayer != null && currentPlayer.getName().equals(localName) && !hasRolled;
		} else {
			enable = !hasRolled;
		}
		btnRollDice.setDisable(!enable);
	}

	private void updateEndTurnEnabledState() {
		boolean enable = false;
		if (hasRolled) {
			if (GameState.get().isCoopMode() && GameState.getSync() != null) {
				String localName = GameState.getPlayerName();
				Player currentPlayer = GameState.get().players().isEmpty() ? null : GameState.get().current();
				enable = currentPlayer != null && currentPlayer.getName().equals(localName) && !currentPlayer.isAI();
			} else {
				Player currentPlayer = GameState.get().current();
				enable = currentPlayer != null && !currentPlayer.isAI();
			}
		}
		btnEndTurn.setDisable(!enable);
	}

	private void removeTokenNodeById(String id) {
		for (Node node : boardGrid.getChildren()) {
			if (node instanceof StackPane sp) {
				java.util.List<Node> toRemove = new java.util.ArrayList<>();
				for (Node child : sp.getChildren()) {
					if (id.equals(child.getId())) toRemove.add(child);
				}
				sp.getChildren().removeAll(toRemove);
			}
		}
	}



    private void updatePlayerPanels() {
        var players = GameState.get().players();
        int n = players.size();

        // Toggle visibility and managed state so layout collapses for missing players
        if (p1Pane != null) { p1Pane.setVisible(n >= 1); p1Pane.setManaged(n >= 1); }
        if (p2Pane != null) { p2Pane.setVisible(n >= 2); p2Pane.setManaged(n >= 2); }
        if (p3Pane != null) { p3Pane.setVisible(n >= 3); p3Pane.setManaged(n >= 3); }
        if (p4Pane != null) { p4Pane.setVisible(n >= 4); p4Pane.setManaged(n >= 4); }

        if (n > 0) updatePanel(players.get(0), p1Name, p1Stats, p1Pos, p1Tiles); else clearPanel(p1Name, p1Stats, p1Pos, p1Tiles);
        if (n > 1) updatePanel(players.get(1), p2Name, p2Stats, p2Pos, p2Tiles); else clearPanel(p2Name, p2Stats, p2Pos, p2Tiles);
        if (n > 2) updatePanel(players.get(2), p3Name, p3Stats, p3Pos, p3Tiles); else clearPanel(p3Name, p3Stats, p3Pos, p3Tiles);
        if (n > 3) updatePanel(players.get(3), p4Name, p4Stats, p4Pos, p4Tiles); else clearPanel(p4Name, p4Stats, p4Pos, p4Tiles);
    }

    private void updatePanel(Player p, Label name, Label stats, Label pos, Label tiles) {
        name.setText(p.getName());
        StringBuilder sb = new StringBuilder();
        sb.append("HP: ").append(p.getHealth()).append("/").append(p.getMaxHealth()).append("\n");
        sb.append("Money: $").append(p.getMoney()).append("\n");
        sb.append("Attack: ").append(p.getAttack()).append("\n");
        sb.append("Influence: ").append(p.getInfluence()).append("\n");
        sb.append("Medicine: ").append(p.getMedicine());
        stats.setText(sb.toString());
        pos.setText("Position: " + p.getPos() + " / " + PERIM);

        // Update owned tiles display
        updateOwnedTilesDisplay(p, tiles);
    }

    private void updateOwnedTilesDisplay(Player p, Label tilesLabel) {
        StringBuilder tilesText = new StringBuilder();
        tilesText.append("Owned Tiles: ");

        boolean hasOwnedTiles = false;
        for (Tile tile : BoardRegistry.tiles().values()) {
            // Fix: compare by name since snapshots create new Player objects
            if (tile.getOwner() != null && tile.getOwner().getName().equals(p.getName())) {
                if (hasOwnedTiles) {
                    tilesText.append(", ");
                }
                tilesText.append(tile.getName()).append("(L").append(tile.getUpgradeLevel()).append(")");
                hasOwnedTiles = true;
            }
        }

        if (!hasOwnedTiles) {
            tilesText.append("None");
        }

        tilesLabel.setText(tilesText.toString());
    }

    private void clearPanel(Label name, Label stats, Label pos, Label tiles) {
        name.setText("-");
        stats.setText("");
        pos.setText("");
        tiles.setText("");
    }

    private void updateHeader() {
        if (GameState.get().players().isEmpty()) return;
        Player current = GameState.get().current();
        turnLabel.setText("Turn: " + current.getName());
        lastRollLabel.setText("Last Roll: " + GameState.get().getLastRoll());
        updatePlayerPanels();
    }

    private boolean isEdge(int r, int c) {
        return r == 0 || c == 0 || r == N - 1 || c == N - 1;
    }

    private String tileLabelFor(int r, int c) {
        int position = getPositionFromRC(r, c);
        TileSpec spec = tileSpecForPosition(position);
        if (spec != null) return spec.label;

        // Corners as in the reference board (clockwise from top-left)
        // 0: Free (top-left), 10: Check-post (top-right), 20: Start (bottom-right), 30: Quarantine (bottom-left)
        if (position == 0)  return TileType.FREE.getDisplayName();
        if (position == 10) return TileType.CHECKPOST.getDisplayName();
        if (position == 20) return TileType.START.getDisplayName();
        if (position == 30) return TileType.QUARANTINE.getDisplayName();

        // Sides/zones according to the picture
        // Top row (Industrial) positions 1..9
        if (position >= 1 && position <= 9) {
            return switch (position) {
                case 3 -> TileType.SCENARIO.getDisplayName();
                case 5 -> TileType.SAFEHAVEN.getDisplayName();
                case 8 -> TileType.ZOMBIE.getDisplayName();
                default -> TileType.TERRITORY.getDisplayName(); // Factories, Power Plant, Warehouses, Radio Tower
            };
        }

        // Right column (Military) positions 11..19
        if (position >= 11 && position <= 19) {
            return switch (position) {
                case 13 -> TileType.RESOURCE.getDisplayName(); // Reward
                case 14 -> TileType.SCENARIO.getDisplayName();
                case 15 -> TileType.SAFEHAVEN.getDisplayName();
                case 16 -> TileType.SPECIAL.getDisplayName();  // Event
                default -> TileType.TERRITORY.getDisplayName(); // Military bases, Research lab, Hospital, Airport
            };
        }

        // Bottom row (Residential) positions 21..29 (right to left from Start)
        if (position >= 21 && position <= 29) {
            return switch (position) {
                case 22 -> TileType.ZOMBIE.getDisplayName();
                case 24 -> TileType.RESOURCE.getDisplayName(); // Reward
                case 25 -> TileType.SAFEHAVEN.getDisplayName();
                case 27 -> TileType.SCENARIO.getDisplayName(); // Event
                default -> TileType.TERRITORY.getDisplayName(); // Abandoned houses, apartments, motel
            };
        }

        // Left column (Commercial) positions 31..39 (bottom to top toward Free)
        if (position >= 31 && position <= 39) {
            return switch (position) {
                case 32 -> TileType.RESOURCE.getDisplayName(); // Reward
                case 35 -> TileType.SAFEHAVEN.getDisplayName();
                case 37 -> TileType.SCENARIO.getDisplayName();
                case 38 -> TileType.ZOMBIE.getDisplayName();
                default -> TileType.TERRITORY.getDisplayName(); // Markets, gas, pharmacy, hardware
            };
        }

        // Fallback
        return TileType.FREE.getDisplayName();
    }

    private static class TileSpec {
        final TileType type;
        final String label;
        TileSpec(TileType type, String label) { this.type = type; this.label = label; }
    }

    private TileSpec tileSpecForPosition(int position) {
        // Explicit path mapping clockwise from START at bottom-right (20)
        switch (position) {
            // Corners
            case 20: return new TileSpec(TileType.START, TileType.START.getDisplayName());
            case 30: return new TileSpec(TileType.QUARANTINE, TileType.QUARANTINE.getDisplayName());
            case 10: return new TileSpec(TileType.CHECKPOST, TileType.CHECKPOST.getDisplayName());
            case 0:  return new TileSpec(TileType.FREE, TileType.FREE.getDisplayName());

            // Bottom row (Residential) 21..29 right -> left
            case 21: return new TileSpec(TileType.TERRITORY, "Abandoned House 1");
            case 22: return new TileSpec(TileType.ZOMBIE, TileType.ZOMBIE.getDisplayName());
            case 23: return new TileSpec(TileType.TERRITORY, "Abandoned House 2");
            case 24: return new TileSpec(TileType.RESOURCE, TileType.RESOURCE.getDisplayName()); // Reward
            case 25: return new TileSpec(TileType.SAFEHAVEN, TileType.SAFEHAVEN.getDisplayName());
            case 26: return new TileSpec(TileType.TERRITORY, "Apartment Complex 1");
            case 27: return new TileSpec(TileType.SCENARIO, TileType.SCENARIO.getDisplayName()); // Event
            case 28: return new TileSpec(TileType.TERRITORY, "Apartment Complex 2");
            case 29: return new TileSpec(TileType.TERRITORY, "Motel Shelter");

            // Top row (Industrial) 1..9 left -> right
            case 1: return new TileSpec(TileType.TERRITORY, "Factory 1");
            case 2: return new TileSpec(TileType.TERRITORY, "Factory 2");
            case 3: return new TileSpec(TileType.SCENARIO, TileType.SCENARIO.getDisplayName());
            case 4: return new TileSpec(TileType.TERRITORY, "Power Plant");
            case 5: return new TileSpec(TileType.SAFEHAVEN, TileType.SAFEHAVEN.getDisplayName());
            case 6: return new TileSpec(TileType.TERRITORY, "Warehouse 1");
            case 7: return new TileSpec(TileType.SPECIAL, "Events"); // Changed from Warehouse 2 to Events
            case 8: return new TileSpec(TileType.ZOMBIE, TileType.ZOMBIE.getDisplayName());
            case 9: return new TileSpec(TileType.TERRITORY, "Radio Tower");

            // Right column (Military) 11..19 top -> bottom
            case 11: return new TileSpec(TileType.SPECIAL, "Super Market 1"); // Changed from Military Base 1 to Super Market 1
            case 12: return new TileSpec(TileType.RESOURCE, "Reward"); // Changed from Military Base 2 to Reward
            case 13: return new TileSpec(TileType.SPECIAL, "Super Market 2"); // Changed from Reward to Super Market 2
            case 14: return new TileSpec(TileType.SCENARIO, TileType.SCENARIO.getDisplayName());
            case 15: return new TileSpec(TileType.SAFEHAVEN, TileType.SAFEHAVEN.getDisplayName());
            case 16: return new TileSpec(TileType.SPECIAL, TileType.SPECIAL.getDisplayName()); // Event
            case 17: return new TileSpec(TileType.TERRITORY, "Research Lab");
            case 18: return new TileSpec(TileType.TERRITORY, "Hospital");
            case 19: return new TileSpec(TileType.TERRITORY, "Airport"); // special unbuyable later

            // Left column (Commercial)
            case 32: return new TileSpec(TileType.TERRITORY, "Supermarket");
            case 33: return new TileSpec(TileType.RESOURCE, "Reward"); // Changed from Gas Station to Reward
            case 34: return new TileSpec(TileType.TERRITORY, "Gas Station");
            case 36: return new TileSpec(TileType.SPECIAL, "Events"); // Changed from Pharmacy to Events
            case 39: return new TileSpec(TileType.TERRITORY, "Hardware Store");
        }
        return null; // other positions use default zone logic
    }

    private String tileColorFor(int r, int c) {
        String label = tileLabelFor(r, c);
        for (TileType type : TileType.values()) {
            if (type.getDisplayName().equals(label)) {
                return type.getColor();
            }
        }
        return "lightgray";
    }

    private int getPositionFromRC(int r, int c) {
        int side = N - 1;
        if (r == 0) return c;
        else if (c == N - 1) return side + r;
        else if (r == N - 1) return side * 2 + (side - c);
        else return side * 3 + (side - r);
    }

    private void showTileInfo(String type) {
        Player currentPlayer = GameState.get().current();
        int position = currentPlayer.getPos();

        TileSpec spec = tileSpecForPosition(position);
        TileType tileType = (spec != null) ? spec.type : getTileTypeFromName(type);
        String name = (spec != null) ? spec.label : type;
        Tile tile = BoardRegistry.getOrCreate(position, tileType, name);

        
        // Handle tile event
        TileEventManager.handleTileEvent(currentPlayer, tile);

        // Update player panels after tile event
        updatePlayerPanels();
        refreshOwnershipVisuals();
    }

    private TileType getTileTypeFromName(String name) {
        for (TileType type : TileType.values()) {
            if (type.getDisplayName().equals(name)) {
                return type;
            }
        }
        return TileType.FREE; // Default fallback
    }

    private void placeToken(Player p, int pos) {
        ImageView existing = tokens.get(p.getName());

        final ImageView tk = existing != null ? existing : createPlayerToken(p);
        if (existing == null) {
            // Create token on-demand if missing after a snapshot
            tk.setId("token-" + p.getName());
            tokens.put(p.getName(), tk);
        }

		// Tag token with stable id per player
		if (tk.getId() == null) tk.setId("token-" + p.getName());

		// Remove from previous parent (if any)
		if (tk.getParent() != null) {
			((Pane) tk.getParent()).getChildren().remove(tk);
		}

        int[] rc = indexToRC(pos);
        Node cell = getNodeByRowColumnIndex(rc[0], rc[1], boardGrid);

		if (cell instanceof StackPane sp) {
            int idx = GameState.get().players().indexOf(p);
            StackPane.setAlignment(tk, Pos.BOTTOM_CENTER);
            tk.setTranslateX((idx - 1.5) * 14);
            tk.setTranslateY(-6);
            sp.getChildren().add(tk);
        } else {
            // Grid may not be built yet; retry once later on FX thread without logging
			Platform.runLater(() -> {
				Node laterCell = getNodeByRowColumnIndex(rc[0], rc[1], boardGrid);
				if (laterCell instanceof StackPane sp2) {
					int idx2 = GameState.get().players().indexOf(p);
					StackPane.setAlignment(tk, Pos.BOTTOM_CENTER);
					tk.setTranslateX((idx2 - 1.5) * 14);
					tk.setTranslateY(-6);
					sp2.getChildren().add(tk);
				}
			});
        }
    }


    private int[] indexToRC(int index) {
        int side = N - 1;
        if (index < side) return new int[]{0, index};
        else if (index < side * 2) return new int[]{index - side, N - 1};
        else if (index < side * 3) return new int[]{N - 1, (N - 1) - (index - side * 2)};
        else return new int[]{(N - 1) - (index - side * 3), 0};
    }

    private Node getNodeByRowColumnIndex(int row, int col, GridPane gridPane) {
        for (Node node : gridPane.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if ((r == null ? 0 : r) == row && (c == null ? 0 : c) == col) return node;
        }
        return null;
    }

    private void updateTileOwnershipVisual(int position) {
        int[] rc = indexToRC(position);
        Node cellNode = getNodeByRowColumnIndex(rc[0], rc[1], boardGrid);
        if (cellNode == null || !(cellNode instanceof StackPane)) return;
        StackPane cell = (StackPane) cellNode;
        javafx.scene.shape.Polygon indicator = ownershipIndicators.get(position);
        if (indicator != null) {
            cell.getChildren().remove(indicator);
            ownershipIndicators.remove(position);
        }
        TileSpec spec = tileSpecForPosition(position);
        TileType type = (spec != null) ? spec.type : TileType.FREE;
        String name = (spec != null) ? spec.label : "Unknown";
        Tile tile = BoardRegistry.getOrCreate(position, type, name);
        if (tile.isOwned()) {
            Player owner = tile.getOwner();
            // Create a 10x10 star-shaped indicator
            javafx.scene.shape.Polygon star = createStarShape(10, 10);
            star.setFill(Paint.valueOf(owner.getColor()));
            star.setStroke(Paint.valueOf("white"));
            star.setStrokeWidth(1.0);
            star.setId("ownershipIndicator-" + position);
            // Add a subtle glow effect
            javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
            shadow.setRadius(2);
            shadow.setColor(javafx.scene.paint.Color.valueOf(owner.getColor()));
            shadow.setOffsetX(0);
            shadow.setOffsetY(0);
            star.setEffect(shadow);
            StackPane.setAlignment(star, Pos.TOP_LEFT);
            cell.getChildren().add(star);
            ownershipIndicators.put(position, star);
        }
    }

    /**
     * Create a star-shaped polygon for ownership indicators
     */
    private javafx.scene.shape.Polygon createStarShape(double width, double height) {
        javafx.scene.shape.Polygon star = new javafx.scene.shape.Polygon();
        
        // Create a 5-pointed star
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double outerRadius = Math.min(width, height) / 2.0;
        double innerRadius = outerRadius * 0.4;
        
        // Calculate star points (10 points total: 5 outer + 5 inner)
        double[] points = new double[20]; // 10 points * 2 coordinates
        int pointIndex = 0;
        
        for (int i = 0; i < 5; i++) {
            double outerAngle = Math.PI / 2.0 + (2.0 * Math.PI * i) / 5.0; // Start from top
            double innerAngle = outerAngle + Math.PI / 5.0; // Half angle offset
            
            // Outer point
            points[pointIndex++] = centerX + outerRadius * Math.cos(outerAngle);
            points[pointIndex++] = centerY + outerRadius * Math.sin(outerAngle);
            
            // Inner point
            points[pointIndex++] = centerX + innerRadius * Math.cos(innerAngle);
            points[pointIndex++] = centerY + innerRadius * Math.sin(innerAngle);
        }
        
        // Convert double[] to Double[] for addAll
        Double[] doublePoints = new Double[points.length];
        for (int i = 0; i < points.length; i++) {
            doublePoints[i] = points[i];
        }
        star.getPoints().addAll(doublePoints);
        return star;
    }

    private void refreshOwnershipVisuals() {
        for (int pos = 0; pos < PERIM; pos++) {
            updateTileOwnershipVisual(pos);
        }
    }

    private void goBackToLobby() {
        // Show confirmation dialog
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Exit Game");
        confirmationDialog.setContentText("Are you sure you want to exit?");

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        confirmationDialog.getButtonTypes().setAll(yesButton, noButton);

        var result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            // Cancel any ongoing timers before leaving
            if (turnTimer != null) {
                turnTimer.cancel();
                turnTimer = null;
            }
            // Handle co-op mode: close network sync and reset flags
            if (GameState.get().isCoopMode()) {
                GameState.setSync(null); // Close network sync
                GameState.get().setCoopMode(false);
                GameState.get().setAutoCoop(false);
                GameState.setCoopRoomCode(null);
                GameState.setNetworkHost(false);
            }
            GameState.get().saveToFile(); // Save before leaving
            Platform.runLater(() -> {
                try {
                    // Load lobby using the same method as in LobbyController
                    Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/Ui/Lobby.fxml")));
                    Stage stage = (Stage) btnBackToLobby.getScene().getWindow();
                    Scene lobbyScene = new Scene(root, 800, 600);
                    stage.setScene(lobbyScene);
                    stage.setWidth(800);
                    stage.setHeight(600);
                    stage.centerOnScreen();
                    stage.setTitle("The Last Defenders");
                    stage.show();
                    System.out.println("Returned to lobby from game");
                    // Play lobby music if audio is enabled
                    if (AudioManager.get().isEnabled()) {
                        AudioManager.get().playMenuLoop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error loading Lobby.fxml");
                    // Show error alert to user
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to return to lobby");
                    errorAlert.setContentText("An error occurred while trying to return to the lobby. Please try again.");
                    errorAlert.showAndWait();
                }
            });
        }
        // If user clicks "No", do nothing and stay in the game
    }

    private void showPlayerHistory(Player player) {
        if (player == null) return;

        Alert historyDialog = new Alert(Alert.AlertType.INFORMATION);
        historyDialog.setTitle("Player History - " + player.getName());
        historyDialog.setHeaderText("Complete Activity Summary");

        StringBuilder content = new StringBuilder();
        PlayerHistory history = player.getHistory();

        // Summary statistics
        content.append("=== SUMMARY ===\n");
        content.append("Owned Tiles: ").append(history.getOwnedTilesCount()).append("\n");
        content.append("Total Money Spent: $").append(history.getTotalMoneySpent()).append("\n");
        content.append("Total Money Earned: $").append(history.getTotalMoneyEarned()).append("\n");
        content.append("Net Money: $").append(history.getTotalMoneyEarned() - history.getTotalMoneySpent()).append("\n\n");

        // Owned tiles list
        List<String> ownedTiles = history.getOwnedTiles();
        if (!ownedTiles.isEmpty()) {
            content.append("=== OWNED TILES ===\n");
            for (String tile : ownedTiles) {
                content.append("• ").append(tile).append("\n");
            }
            content.append("\n");
        }

        // Recent activity (last 10 entries)
        List<PlayerHistory.HistoryEntry> recentHistory = history.getRecentHistory(10);
        if (!recentHistory.isEmpty()) {
            content.append("=== RECENT ACTIVITY ===\n");
            for (PlayerHistory.HistoryEntry entry : recentHistory) {
                content.append(entry.toString()).append("\n");
            }
        } else {
            content.append("=== RECENT ACTIVITY ===\n");
            content.append("No recent activity recorded.\n");
        }

        historyDialog.setContentText(content.toString());
        historyDialog.getDialogPane().setPrefSize(500, 400);
        historyDialog.showAndWait();
    }


    private void showDoubleDiceRollAnimation(int finalD1, int finalD2) {
        Platform.runLater(() -> {
            // Resolve a safe owner window if available
            javafx.stage.Window ownerWindow = null;
            try {
                if (btnRollDice != null && btnRollDice.getScene() != null) {
                    ownerWindow = btnRollDice.getScene().getWindow();
                }
                if (ownerWindow == null) {
                    // Fallback: any showing window
                    for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                        if (w.isShowing()) { ownerWindow = w; break; }
                    }
                }
            } catch (Throwable ignore) {}

            // If no window is available yet, skip showing animation to avoid NPEs
            if (ownerWindow == null) return;

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            if (ownerWindow instanceof Stage) {
                dialog.initOwner((Stage) ownerWindow);
            }
            dialog.initModality(Modality.NONE);

            HBox diceBox = new HBox(12);
            diceBox.setAlignment(Pos.CENTER);
            diceBox.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-padding: 12; -fx-background-radius: 10;");

            ImageView die1 = new ImageView();
            ImageView die2 = new ImageView();
            die1.setFitWidth(84);
            die1.setFitHeight(84);
            die2.setFitWidth(84);
            die2.setFitHeight(84);
            die1.setPreserveRatio(true);
            die2.setPreserveRatio(true);
            diceBox.getChildren().addAll(die1, die2);

            Scene scene = new Scene(diceBox);
            scene.setFill(null);
            dialog.setScene(scene);
            dialog.setWidth(220);
            dialog.setHeight(130);

            // Center relative to owner window if possible
            try {
                dialog.setX(ownerWindow.getX() + (ownerWindow.getWidth() - dialog.getWidth()) / 2);
                dialog.setY(ownerWindow.getY() + (ownerWindow.getHeight() - dialog.getHeight()) / 2);
            } catch (Throwable ignore) {}

            dialog.show();

            // Play dice rolling sound
            playDiceSound();

            // Play dice rolling sound
            if (diceSound != null) {
                diceSound.stop();
                diceSound.play();
            }

            // Animate both dice
            Timeline timeline = new Timeline();
            int frames = 14;
            for (int i = 0; i < frames; i++) {
                int f1 = 1 + (int) (Math.random() * 6);
                int f2 = 1 + (int) (Math.random() * 6);
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(70 * (i + 1)), ev -> {
                    setDiceImage(die1, f1);
                    setDiceImage(die2, f2);
                }));
            }

            // Final faces
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(55 * (frames + 1)), ev -> {
                setDiceImage(die1, finalD1);
                setDiceImage(die2, finalD2);
            }));

            timeline.setOnFinished(ev -> {
                Timeline closeDelay = new Timeline(new KeyFrame(Duration.millis(650), e -> dialog.close()));
                closeDelay.play();
                if (diceSound != null) {
                    diceSound.stop();
                }
            });

            timeline.play();
        });
    }

    private void playDiceSound() {
        try {
            if (diceSound != null) {
                diceSound.stop();
                diceSound.play();
            }
        } catch (Throwable ignore) {}
    }

    private void setDiceImage(ImageView imageView, int face) {
        String resource = "/images/dice" + face + ".png";
        Image img = null;
        try {
            img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(resource)));
        } catch (Exception e) {
            // Fallback: try alternative names like "dice 1 .png" if provided with spaces
            String alt = "/images/dice " + face + " .png";
            try {
                img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(alt)));
            } catch (Exception ignore) {
                // ignore if not found
            }
        }
        if (img != null) {
            imageView.setImage(img);
        }
    }

    private AudioClip loadDiceSound() {
        try {
            var res = getClass().getResource("/sound/Dice Rolling.mp3");
            if (res == null) return null;
            AudioClip clip = new AudioClip(res.toExternalForm());
            clip.setVolume(0.8);
            return clip;
        } catch (Throwable t) {
            return null; // Media not available; continue without sound
        }
    }

    private AudioClip loadFootstepSound() {
        try {
            var res = getClass().getResource("/sound/Domino Jumping.mp3");
            if (res == null) return null;
            AudioClip clip = new AudioClip(res.toExternalForm());
            clip.setVolume(0.6);
            return clip;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Animate player movement from startPos (exclusive) to endPos (inclusive),
     * stepping one tile at a time, playing footstep sound on each step.
     */
    private void animatePlayerMovement(Player player, int startPos, int endPos, Runnable onFinish) {
        List<Integer> path = new java.util.ArrayList<>();
        int steps = (endPos - startPos + PERIM) % PERIM;
        for (int i = 1; i <= steps; i++) {
            path.add((startPos + i) % PERIM);
        }
        if (path.isEmpty()) {
            if (onFinish != null) Platform.runLater(onFinish);
            return;
        }

        Timeline timeline = new Timeline();
        int stepMs = 220; // milliseconds per step
        for (int i = 0; i < path.size(); i++) {
            final int nextPos = path.get(i);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis((i + 1) * stepMs), ev -> {
                placeToken(player, nextPos);
                if (footstepSound != null) {
                    try {
                        footstepSound.play();
                    } catch (Throwable ignore) {}
                }
            }));
        }

        timeline.setOnFinished(ev -> {
            // Schedule onFinish to run on the FX thread after the current pulse finishes
            if (onFinish != null) Platform.runLater(onFinish);
        });
        timeline.play();
    }

    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
        }

        // Fixed timer length for all turns
        timeRemaining = TURN_TIME_SECONDS;
        hasRolled = false;
        updateRollDiceEnabledState();
        updateTimerDisplay();
        isAdvancingTurn = false;
        aiActionScheduled = false;

        turnTimer = new Timer();
        turnTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (isAdvancingTurn) return;
                    
                    // Task 3: Only count down timer for current player in co-op mode
                    Player currentPlayer = GameState.get().current();
                    boolean shouldCountDown = true;
                    
                    if (GameState.get().isCoopMode()) {
                        String localName = GameState.getPlayerName();
                        shouldCountDown = currentPlayer != null && currentPlayer.getName().equals(localName);
                    }
                    
                    if (shouldCountDown) {
                        timeRemaining--;
                        updateTimerDisplay();

                        if (timeRemaining <= 0) {
                            // Time's up - advance to next player and trigger AI if needed
                            safeAdvanceToNextTurn();
                            triggerAIIfNeeded();
                        }
                    }
                });
            }
        }, 1000, 1000); // Start after 1 second, repeat every 1 second
    }

    private void updateTimerDisplay() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        
        // Task 3: Show current player name in co-op mode
        if (GameState.get().isCoopMode()) {
            Player currentPlayer = GameState.get().current();
            String playerName = currentPlayer != null ? currentPlayer.getName() : "Unknown";
            String localName = GameState.getPlayerName();
            boolean isMyTurn = currentPlayer != null && currentPlayer.getName().equals(localName);
            
            if (isMyTurn) {
                timerLabel.setText(String.format("Your Turn: %d:%02d", minutes, seconds));
            } else {
                timerLabel.setText(String.format("%s's Turn: %d:%02d", playerName, minutes, seconds));
            }
        } else {
            timerLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
        }

        // Change color based on time remaining
        if (timeRemaining <= 10) {
            timerLabel.setStyle("-fx-text-fill: red; -fx-font-size: 16; -fx-font-weight: bold;");
        } else if (timeRemaining <= 30) {
            timerLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 16; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: green; -fx-font-size: 16; -fx-font-weight: bold;");
        }
    }

    private void advanceToNextPlayer() {
        if (turnTimer != null) {
            turnTimer.cancel();
        }

        GameState.get().endTurn();
        updateHeader();
        // Apply start-of-turn bonuses for the new current player
        var bonus = GameState.get().applyStartOfTurnBonuses();
        if (!bonus.isEmpty()) {
            if (!GameState.get().current().isAI()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Start of Turn Bonuses");
                a.setHeaderText("Income & bonuses applied to " + GameState.get().current().getName());
                a.setContentText("Money: +" + bonus.money + ", Attack: +" + bonus.attack + ", Med: +" + bonus.medicine + ", Influence: +" + bonus.influence);
                // Ensure dialogs are shown on the FX thread after layout/animations have finished
                Platform.runLater(a::showAndWait);
            }
            updatePlayerPanels();
        }
        // Always clear AI scheduling flag on any turn change so the next AI can act
        aiActionScheduled = false;
        if (GameState.get().isAIMode() && GameState.get().current().isAI()) {
            // Process AI turn first, then trigger AI actions
            AIPlayer.processAITurn(GameState.get().current());
            // brief delay before AI acts
            triggerAIIfNeeded();
        } else {
            startTurnTimer();
        }
    }

    private void safeAdvanceToNextTurn() {
        if (isAdvancingTurn) return;
        isAdvancingTurn = true;
        if (inactivityTimer != null) { inactivityTimer.cancel(); inactivityTimer = null; }
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        advanceToNextPlayer();
        isAdvancingTurn = false;
    }

    private void triggerAIIfNeeded() {
        if (!GameState.get().isAIMode()) return;
        if (!GameState.get().current().isAI()) return;
        if (aiActionScheduled) return;
        aiActionScheduled = true;
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() { Platform.runLater(() -> autoRollAndHandle()); }
        }, 800);
    }

    private void scheduleInactivityAutoEnd(Player playerAtStart) {
        if (GameState.get().isAIMode()) return; // humans only
        if (inactivityTimer != null) { inactivityTimer.cancel(); inactivityTimer = null; }
        inactivityTimer = new Timer();
        inactivityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    // Only advance if it's still the same player's turn and they are human
                    Player cur = GameState.get().current();
                    if (cur != null && !cur.isAI() && cur.getName().equals(playerAtStart.getName())) {
                        safeAdvanceToNextTurn();
                    }
                });
            }
        }, 5000);
    }

    private ImageView createPlayerToken(Player p) {
        ImageView token = new ImageView();
        String imagePath = getPlayerImagePath(p);

        try {
            Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            token.setImage(img);
            token.setFitWidth(35);
            token.setFitHeight(35);
            token.setPreserveRatio(true);

            // Make it circular by clipping
            Circle clip = new Circle(17.5, 17.5, 17.5); // x, y, radius
            token.setClip(clip);

            // Optional: smooth edges by snapshotting with a transparent background
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            WritableImage roundedImage = token.snapshot(parameters, null);
            token.setClip(null); // remove clip
            token.setImage(roundedImage); // replace with circular image

        } catch (Exception e) {
            // Fallback to colored circle if image fails to load
            Circle fallback = new Circle(17.5);
            fallback.setFill(Paint.valueOf(p.getColor()));
            StackPane fallbackToken = new StackPane(fallback);
            System.err.println("Failed to load image for player " + p.getName() + ": " + imagePath);
        }

        return token;
    }

    private String getPlayerImagePath(Player p) {
        int playerIndex = GameState.get().players().indexOf(p) + 1;
        switch (playerIndex) {
            case 1: return "/images/player1.png";
            case 2: return "/images/player2.png";
            case 3: return "/images/player3.png";
            case 4: return "/images/player4.png";
            default: return "/images/player1.png"; // fallback
        }
    }

    private void autoRollAndHandle() {
        // Auto roll for AI
        int roll = GameState.get().roll();
        Player currentPlayer = GameState.get().current();
        String playerName = currentPlayer.getName();
        lastRollLabel.setText(playerName + " rolled: " + roll +
                " (" + GameState.get().getDice1() + "+" + GameState.get().getDice2() + ")");
        showDoubleDiceRollAnimation(GameState.get().getDice1(), GameState.get().getDice2());
        lastShownRoll = roll; // Prevent duplicate animation in sync listener

        // Animate movement instead of instant placement
        int newPos = currentPlayer.getPos();
        int startPos = (newPos - roll) % PERIM;
        if (startPos < 0) startPos += PERIM;
        animatePlayerMovement(currentPlayer, startPos, newPos, () -> {
            updatePlayerPanels();
            // Auto handle tile event
            int position = currentPlayer.getPos();
            int[] rc = indexToRC(position);
            String tileType = tileLabelFor(rc[0], rc[1]);
            showTileInfo(tileType); // This will handle AI without dialogs

            // Auto-advance after delay
            Timer autoAdvanceTimer = new Timer();
            autoAdvanceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        advanceToNextPlayer();
                    });
                }
            }, 2000); // Shorter delay for AI
        });
    }
}
