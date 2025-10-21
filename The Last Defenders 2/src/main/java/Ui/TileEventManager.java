package Ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TileEventManager {

    // Hook to notify when a human-facing dialog completes. One-shot.
    private static volatile Runnable afterDialogHook;

    public static void setAfterDialogHook(Runnable hook) {
        afterDialogHook = hook;
    }

    public static void handleTileEvent(Player player, Tile tile) {
        System.out.println("🎯 handleTileEvent called for " + player.getName() + " (AI: " + player.isAI() + ") on tile " + tile.getName() + " type " + tile.getType());
        switch (tile.getType()) {
            case START -> handleStartTile(player);
            case FREE -> handleFreeTile(player);
            case CHECKPOST -> handleCheckpostTile(player);
            case QUARANTINE -> handleQuarantineTile(player);
            case SAFEHAVEN -> handleSafeHavenTile(player, tile);
            case TERRITORY -> handleTerritoryTile(player, tile);
            case ZOMBIE -> handleZombieTile(player, tile);
            case RESOURCE -> handleResourceTile(player, tile);
            case SCENARIO -> handleScenarioTile(player, tile);
            case SPECIAL -> handleSpecialTile(player, tile);
        }
    }

    private static void showDialog(Runnable dialogLogic) {
        Platform.runLater(() -> {
            try {
                dialogLogic.run();
            } catch (Exception e) {
                System.err.println("Dialog error: " + e.getMessage());
                e.printStackTrace();
            }
            Runnable hook = afterDialogHook;
            if (hook != null) {
                // one-shot
                afterDialogHook = null;
                try { hook.run(); } catch (Throwable ignore) {}
            }
        });
    }

    private static void handleStartTile(Player player) {
        if (!player.isInQuarantine()) {
            player.addMoney(200);
            player.addMedicine(2);
            GameState.notifyUpdate(); // Sync player data changes

            if (!player.isAI()) {
                showDialog(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Start Tile");
                    alert.setHeaderText("Welcome to the Start!");
                    alert.setContentText("You received 200 money and 2 medicine!");
                    alert.showAndWait();
                });
            }
        } else if (!player.isAI()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Start Tile");
                alert.setHeaderText("You're in quarantine!");
                alert.setContentText("No rewards while in quarantine.");
                alert.showAndWait();
            });
        }
    }

    private static void handleFreeTile(Player player) {
        if (!player.isAI()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Free Zone");
                alert.setHeaderText("A peaceful area.");
                alert.setContentText("You can rest safely here. No events occur.");
                alert.showAndWait();
            });
        }
    }

    private static void handleCheckpostTile(Player player) {
        boolean decideBribe = false;

        if (player.isAI()) {
            if (player instanceof AIPlayer) {
                AIPlayer ai = (AIPlayer) player;
                decideBribe = AIPlayer.shouldAIPayBribe(ai);
            } else {
                // Fallback for non-AIPlayer instances
                decideBribe = player.getMoney() >= 150 && player.getMedicine() >= 1;
            }
        } else {
            final Boolean[] decision = {false};
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Check-post");
                alert.setHeaderText("You got caught in a check-post!");
                alert.setContentText("The officer threatens you to send to the quarantine zone.\n\nChoose your action:");

                ButtonType quarantineButton = new ButtonType("Go to Quarantine");
                ButtonType bribeButton = new ButtonType("Pay Bribe (150 money + 1 medicine)");
                ButtonType backButton = new ButtonType("Back", ButtonType.CANCEL.getButtonData());

                alert.getButtonTypes().setAll(quarantineButton, bribeButton, backButton);

                Optional<ButtonType> result = alert.showAndWait();
                decision[0] = result.isPresent() && result.get() == bribeButton;
            });
            decideBribe = decision[0];
        }

        if (decideBribe) {
            if (player.getMoney() >= 150 && player.getMedicine() >= 1) {
                player.spendMoney(150);
                player.addMedicine(-1);
                GameState.notifyUpdate();
                if (!player.isAI()) {
                    showDialog(() -> {
                        Alert bribeAlert = new Alert(Alert.AlertType.INFORMATION);
                        bribeAlert.setTitle("Bribe Paid");
                        bribeAlert.setHeaderText("You paid the bribe!");
                        bribeAlert.setContentText("You can continue playing.");
                        bribeAlert.showAndWait();
                    });
                }
            } else {
                player.setInQuarantine(true);
                // Move player to Quarantine tile (position 30)
                player.setPos(30);
                GameState.notifyUpdate();
                if (!player.isAI()) {
                    showDialog(() -> {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Insufficient Resources");
                        errorAlert.setHeaderText("You don't have enough money or medicine!");
                        errorAlert.setContentText("You'll be sent to quarantine instead.");
                        errorAlert.showAndWait();
                    });
                }
            }
        } else {
            player.setInQuarantine(true);
            // Move player to Quarantine tile (position 30)
            player.setPos(30);
            if (!player.isAI()) {
                showDialog(() -> {
                    Alert quarantineAlert = new Alert(Alert.AlertType.WARNING);
                    quarantineAlert.setTitle("Quarantine");
                    quarantineAlert.setHeaderText("You've been sent to quarantine!");
                    quarantineAlert.setContentText("You'll miss the next round.");
                    quarantineAlert.showAndWait();
                });
            }
            GameState.notifyUpdate();
        }
    }

    private static void handleQuarantineTile(Player player) {
        if (!player.isAI()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Quarantine Zone");
                if (player.isInQuarantine()) {
                    alert.setHeaderText("You're serving your quarantine sentence.");
                    alert.setContentText("The conditions are dire here. You'll be released next turn.");
                } else {
                    alert.setHeaderText("You've landed in the quarantine zone.");
                    alert.setContentText("The conditions are dire here, but you're free to leave.");
                }
                alert.showAndWait();
            });
        }
    }

    private static void handleSafeHavenTile(Player player, Tile tile) {
        if (tile.isOwned()) {
            // Fix: compare by name since snapshots create new Player objects
            if (tile.getOwner() != null && tile.getOwner().getName().equals(player.getName())) {
                handleOwnedSafeHaven(player, tile);
            } else {
                int medicineReward = tile.getMedicineReward();
                int attackReward = tile.getAttackReward();
                player.addMedicine(medicineReward);
                player.increaseAttack(attackReward);
                GameState.notifyUpdate(); // Sync player data changes
                if (!player.isAI()) {
                    showDialog(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Safe Haven");
                        alert.setHeaderText("Welcome to " + tile.getOwner().getName() + "'s Safe Haven!");
                        alert.setContentText("You received " + medicineReward + " medicine and " + attackReward + " attack!");
                        alert.showAndWait();
                    });
                }
            }
        } else {
            System.out.println("🏠 Safe Haven not owned, showing trade dialog for " + player.getName());
            showSafeHavenTrade(player, tile);
        }
    }

    private static void handleOwnedSafeHaven(Player player, Tile tile) {
        if (player.isAI()) {
            int upgradeCost = 500 * (tile.getUpgradeLevel() + 1);
            if (player.getMoney() >= upgradeCost) upgradeSafeHaven(player, tile);
        } else {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Your Safe Haven");
                alert.setHeaderText("Welcome to your Safe Haven!");
                alert.setContentText("What would you like to do?");

                ButtonType upgradeButton = new ButtonType("Upgrade");
                ButtonType setPricesButton = new ButtonType("Set Prices");
                ButtonType justRestButton = new ButtonType("Just Rest");
                ButtonType backButton = new ButtonType("Back", ButtonType.CANCEL.getButtonData());

                alert.getButtonTypes().setAll(upgradeButton, setPricesButton, justRestButton, backButton);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == upgradeButton) upgradeSafeHaven(player, tile);
                    else if (result.get() == setPricesButton && tile.canSetPrices()) setSafeHavenPrices(tile);
                }
            });
        }
    }

    private static void showSafeHavenTrade(Player player, Tile tile) {
        boolean decideBuy = false, decideTrade = false;

        if (player.isAI()) {
            int cost = 1000, influenceRequired = 10;
            if (player instanceof AIPlayer) {
                AIPlayer ai = (AIPlayer) player;
                decideBuy = AIPlayer.shouldAIBuySafeHaven(ai, tile, cost, influenceRequired);
                decideTrade = AIPlayer.shouldAITrade(ai);
            } else {
                // Fallback for non-AIPlayer instances
                if (player.getMoney() >= cost && player.getInfluence() >= influenceRequired) decideBuy = true;
                else if (player.getMedicine() < 5) decideTrade = true;
            }
        } else {
            // For human players, show the dialog and handle the response
            showSafeHavenDialog(player, tile);
            return; // Exit early as the dialog will handle the purchase/trading
        }

        if (decideBuy) buySafeHaven(player, tile);
        else if (decideTrade) showTradeOptions(player);
    }

    private static void showTradeOptions(Player player) {
        if (player.isAI()) {
            if (player.getMedicine() < 3 && player.getMoney() >= 50) buyMedicine(player);
            else if (player.getMoney() >= 100) buyAttackBoost(player);
            else if (player.getMoney() >= 200) buyArmor(player);
        } else {
            showDialog(() -> {
                List<String> options = new ArrayList<>();
                options.add("Buy Medicine (50 money each)");
                options.add("Buy Attack Boost (100 money for +10 attack)");
                options.add("Buy Armor (200 money for +20 health)");
                options.add("Cancel");

                ChoiceDialog<String> dialog = new ChoiceDialog<>("Cancel", options);
                dialog.setTitle("Safe Haven Trade");
                dialog.setHeaderText("Choose what to buy:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(choice -> {
                    if (choice.contains("Medicine")) buyMedicine(player);
                    else if (choice.contains("Attack")) buyAttackBoost(player);
                    else if (choice.contains("Armor")) buyArmor(player);
                });
            });
        }
    }

    private static void buyMedicine(Player player) {
        if (player.isAI()) {
            if (player.getMoney() >= 50) {
                player.spendMoney(50);
                player.addMedicine(1);
                creditSafeHavenOwnerOnTrade(player, 50);
            }
        } else {
            showDialog(() -> {
                TextInputDialog dialog = new TextInputDialog("1");
                dialog.setTitle("Buy Medicine");
                dialog.setHeaderText("How many medicine do you want to buy?");
                dialog.setContentText("Each medicine costs 50 money:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(input -> {
                    try {
                        int amount = Integer.parseInt(input);
                        int cost = amount * 50;
                        if (player.getMoney() >= cost) {
                            player.spendMoney(cost);
                            player.addMedicine(amount);
                            creditSafeHavenOwnerOnTrade(player, cost);
                            GameState.notifyUpdate();
                            Alert success = new Alert(Alert.AlertType.INFORMATION);
                            success.setTitle("Purchase Successful");
                            success.setContentText("You bought " + amount + " medicine for " + cost + " money!");
                            success.showAndWait();
                        } else {
                            Alert error = new Alert(Alert.AlertType.ERROR);
                            error.setTitle("Insufficient Money");
                            error.setContentText("You don't have enough money!");
                            error.showAndWait();
                        }
                    } catch (NumberFormatException e) {
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Invalid Input");
                        error.setContentText("Please enter a valid number!");
                        error.showAndWait();
                    }
                });
            });
        }
    }

    private static void buyAttackBoost(Player player) {
        if (player.getMoney() >= 100) {
            player.spendMoney(100);
            player.increaseAttack(10);
            creditSafeHavenOwnerOnTrade(player, 100);
            GameState.notifyUpdate();
            if (!player.isAI()) {
                showDialog(() -> {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Purchase Successful");
                    success.setContentText("You bought an attack boost for 100 money!");
                    success.showAndWait();
                });
            }
        } else if (!player.isAI()) {
            showDialog(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Insufficient Money");
                error.setContentText("You don't have enough money!");
                error.showAndWait();
            });
        }
    }

    private static void buyArmor(Player player) {
        if (player.getMoney() >= 200) {
            player.spendMoney(200);
            player.increaseMaxHealth(20);
            creditSafeHavenOwnerOnTrade(player, 200);
            GameState.notifyUpdate();
            if (!player.isAI()) {
                showDialog(() -> {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Purchase Successful");
                    success.setContentText("You bought armor for 200 money! Your max health increased by 20!");
                    success.showAndWait();
                });
            }
        } else if (!player.isAI()) {
            showDialog(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Insufficient Money");
                error.setContentText("You don't have enough money!");
                error.showAndWait();
            });
        }
    }

    private static void buySafeHaven(Player player, Tile tile) {
        int cost = 1000, influenceRequired = 180;
        if (player.getMoney() >= cost && player.getInfluence() >= influenceRequired) {
            player.spendMoney(cost);
            tile.setOwner(player);
            
            // Add to player history
            player.buyTile(tile, cost);
            
            GameState.notifyUpdate();
            if (!player.isAI()) {
                showDialog(() -> {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Safe Haven Purchased");
                    success.setContentText("You now own this Safe Haven! You'll receive benefits when other players trade here.");
                    success.showAndWait();
                });
            }
        } else if (!player.isAI()) {
            showDialog(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Requirements Not Met");
                error.setContentText("You need " + cost + " money and " + influenceRequired + " influence to buy this Safe Haven!");
                error.showAndWait();
            });
        }
    }

    private static void upgradeSafeHaven(Player player, Tile tile) {
        int upgradeCost = 500 * (tile.getUpgradeLevel() + 1);
        if (player.getMoney() >= upgradeCost) {
            player.spendMoney(upgradeCost);
            tile.setUpgradeLevel(tile.getUpgradeLevel() + 1);
            if (!player.isAI()) {
                showDialog(() -> {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Safe Haven Upgraded");
                    success.setContentText("Your Safe Haven has been upgraded! Trade percentage: " + tile.getTradePercentage() +
                            "%, Medicine reward: " + tile.getMedicineReward() + ", Attack reward: " + tile.getAttackReward());
                    success.showAndWait();
                });
            }
        } else if (!player.isAI()) {
            showDialog(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Insufficient Money");
                error.setContentText("You need " + upgradeCost + " money to upgrade!");
                error.showAndWait();
            });
        }
    }

    private static void setSafeHavenPrices(Tile tile) {
        if (!tile.getOwner().isAI() && tile.canSetPrices()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Set Trade Prices");
                alert.setHeaderText("Radio Tower Control Enabled");
                alert.setContentText("Price adjustments for trades are now active (owner advantage).");
                alert.showAndWait();
            });
        }
    }

    // The remaining methods (handleTerritoryTile, handleZombieTile, handleResourceTile, handleScenarioTile, handleSpecialTile, considerResearchCure)
    // must follow the same showDialog wrapping pattern for all Alerts, ChoiceDialogs, and TextInputDialogs.
    private static void handleTerritoryTile(Player player, Tile tile) {
        TerritorySpec spec = TerritoryCatalog.getSpecForPosition(tile.getPosition());
        System.out.println("🏠 handleTerritoryTile for " + player.getName() + " at position " + tile.getPosition() + ", spec: " + (spec != null ? spec.displayName : "null"));
        if (spec == null) {
            System.out.println("⚠️ No territory spec found for position " + tile.getPosition());
            if (!player.isAI()) {
                showDialog(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Territory");
                    alert.setHeaderText("You've entered a territory.");
                    alert.setContentText("This area seems safe for now.");
                    alert.showAndWait();
                });
            }
            return;
        }

        if (!tile.isOwned()) {
            System.out.println("🏠 Tile not owned, showing buy dialog for " + player.getName());
            boolean decideBuy = false;

            if (player.isAI()) {
                // Use AIPlayer static methods for AI decisions
                if (player instanceof AIPlayer) {
                    AIPlayer ai = (AIPlayer) player;
                    decideBuy = AIPlayer.shouldAIBuyTile(ai, tile, spec.buyCost, spec.influenceRequirement, spec.attackRequirement);
                } else {
                    // Fallback for non-AIPlayer instances
                    decideBuy = player.getInfluence() >= spec.influenceRequirement &&
                            player.getAttack() >= spec.attackRequirement &&
                            player.getMoney() >= spec.buyCost;
                }
            } else {
                System.out.println("💬 Showing buy dialog for human player " + player.getName());
                // For human players, we need to handle the dialog synchronously
                // Use a different approach that works with the existing dialog system
                showTerritoryBuyDialog(player, tile, spec);
                return; // Exit early as the dialog will handle the purchase
            }

            if (decideBuy) {
                if (player.getInfluence() < spec.influenceRequirement) {
                    if (!player.isAI()) {
                        showDialog(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Insufficient Influence");
                            alert.setHeaderText("You need at least " + spec.influenceRequirement + " influence to buy this tile.");
                            alert.showAndWait();
                        });
                    }
                    return;
                }
                if (player.getAttack() < spec.attackRequirement) {
                    player.takeDamage(spec.failHealthLoss);
                    if (!player.isAI()) {
                        showDialog(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Failed to Clear");
                            alert.setHeaderText("Insufficient attack to secure the area");
                            alert.setContentText("You lost " + spec.failHealthLoss + " health.");
                            alert.showAndWait();
                        });
                    }
                    return;
                }
                if (player.getMoney() < spec.buyCost) {
                    if (!player.isAI()) {
                        showDialog(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Not Enough Money");
                            alert.setContentText("You need " + spec.buyCost + "M to restore this tile.");
                            alert.showAndWait();
                        });
                    }
                    return;
                }
                player.spendMoney(spec.buyCost);
                tile.setOwner(player);
                tile.setUpgradeLevel(0);
                player.increaseInfluence(spec.influenceOnClaim);
                player.increaseAttack(spec.attackBonusOnClaim);
                if (spec.medicineBonusOnClaim > 0) player.addMedicine(spec.medicineBonusOnClaim);
                
                // Add to player history
                player.buyTile(tile, spec.buyCost);

                if ("Radio Tower".equals(tile.getName())) {
                    BoardRegistry.tiles().values().forEach(t -> {
                        if (t.getType() == TileType.SAFEHAVEN && player.equals(t.getOwner())) {
                            t.setCanSetPrices(true);
                        }
                    });
                }

                if (!player.isAI()) {
                    showDialog(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Base Established");
                        alert.setHeaderText(spec.displayName + " restored!");
                        alert.setContentText("Opponents will pay rent when landing here.");
                        alert.showAndWait();
                    });
                }
            }
            return;
        }

        if (tile.getOwner() == player) {
            int currentLevel = tile.getUpgradeLevel();
            int nextLevel = currentLevel + 1;

            if ("Hospital".equals(tile.getName())) {
                if (!player.isAI()) {
                    showDialog(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Hospital");
                        alert.setHeaderText("Hospital cannot be upgraded.");
                        alert.showAndWait();
                    });
                }
                return;
            }

            if (nextLevel > 3) {
                if (!player.isAI()) {
                    showDialog(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Max Level");
                        alert.setHeaderText("This base is fully upgraded.");
                        alert.showAndWait();
                    });
                }
                return;
            }

            int upgradeCost = spec.upgradeCosts[nextLevel - 1];
            boolean decideUpgrade = player.isAI() && player.getMoney() >= upgradeCost;

            if (!player.isAI()) {
                final boolean[] decision = {false};
                showDialog(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Upgrade Base");
                    alert.setHeaderText(spec.displayName + " → Level " + nextLevel);
                    alert.setContentText("Upgrade cost: " + upgradeCost + "M\nNew rent: " + spec.rentDescriptionForLevel(nextLevel));

                    ButtonType upgradeBtn = new ButtonType("Upgrade");
                    ButtonType leaveBtn = new ButtonType("Leave");
                    ButtonType backBtn = new ButtonType("Back", ButtonType.CANCEL.getButtonData());
                    alert.getButtonTypes().setAll(upgradeBtn, leaveBtn, backBtn);

                    Optional<ButtonType> res = alert.showAndWait();
                    decision[0] = res.isPresent() && res.get() == upgradeBtn;
                });
                decideUpgrade = decision[0];
            }

            if (decideUpgrade) {
                if (player.getMoney() < upgradeCost) {
                    if (!player.isAI()) {
                        showDialog(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Not Enough Money");
                            alert.setContentText("You need " + upgradeCost + "M to upgrade.");
                            alert.showAndWait();
                        });
                    }
                    return;
                }
                player.spendMoney(upgradeCost);
                tile.setUpgradeLevel(nextLevel);
                player.increaseAttack(spec.perRoundAttack[nextLevel]);
                if (spec.perRoundMedicine[nextLevel] > 0) player.addMedicine(spec.perRoundMedicine[nextLevel]);
                GameState.notifyUpdate();

                if (!player.isAI()) {
                    showDialog(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Upgraded");
                        alert.setHeaderText(spec.displayName + " is now Level " + nextLevel);
                        alert.showAndWait();
                    });
                }
            }
            return;
        }

        // Opponent base: pay rent
        int level = tile.getUpgradeLevel();
        int rentMoney = spec.rentMoney[level];
        int rentMedicine = spec.rentMedicine[level];
        player.spendMoney(rentMoney);
        if (rentMedicine > 0) player.addMedicine(-rentMedicine);
        tile.getOwner().addMoney(rentMoney);

        if ("Hospital".equals(tile.getName())) tile.getOwner().addMedicine(5);

        if (!player.isAI()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Rent Paid");
                alert.setHeaderText("You landed on " + tile.getOwner().getName() + "'s " + spec.displayName);
                String medPart = rentMedicine > 0 ? (" and " + rentMedicine + " medicine") : "";
                alert.setContentText("Paid " + rentMoney + "M" + medPart + ".");
                alert.showAndWait();
            });
        }
    }

    private static void handleZombieTile(Player player, Tile tile) {
        int pos = tile.getPosition();
        int damage, attackLoss;
        String zone;

        if (pos >= 21 && pos <= 29) { zone = "Residential"; damage = player.getAttack() >= 50 ? 50 : 100; attackLoss = 10; }
        else if (pos >= 31 && pos <= 39) { zone = "Commercial"; damage = player.getAttack() >= 120 ? 75 : 150; attackLoss = 12; }
        else if (pos >= 1 && pos <= 9) { zone = "Industrial"; damage = player.getAttack() >= 200 ? 100 : 200; attackLoss = 16; }
        else { zone = "Military"; damage = player.getAttack() >= 300 ? 120 : 250; attackLoss = 20; }

        // Play zombie sound when landing on zombie tile
        if (AudioManager.get().isEnabled() && AudioManager.get().isMediaAvailable()) {
            try {
                var zombieSoundUrl = TileEventManager.class.getResource("/sound/Zombie.mp3");
                if (zombieSoundUrl != null) {
                    javafx.scene.media.AudioClip zombieSound = new javafx.scene.media.AudioClip(zombieSoundUrl.toExternalForm());
                    zombieSound.setVolume(0.7);
                    zombieSound.play();
                }
            } catch (Exception e) {
                // Sound not available, continue silently
            }
        }

        if (!player.isAI()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Zombie Attack!");
                alert.setHeaderText(zone + " zombie threat!");
                alert.setContentText("Lose " + damage + " health and -" + attackLoss + " attack.");
                alert.showAndWait();
            });
        }

        player.takeDamage(damage);
        player.increaseAttack(-attackLoss);
        GameState.notifyUpdate();
    }

    private static void handleResourceTile(Player player, Tile tile) {
        int moneyReward = 100;
        int medicineReward = 1;

        player.addMoney(moneyReward);
        player.addMedicine(medicineReward);
        GameState.notifyUpdate(); // Sync player data changes

        if (!player.isAI()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Resource Found");
                alert.setHeaderText("You found valuable resources!");
                alert.setContentText("You received " + moneyReward + " money and " + medicineReward + " medicine!");
                alert.showAndWait();
            });
        }
    }

    private static void handleScenarioTile(Player player, Tile tile) {
        ScenarioManager.triggerRandomScenario(player);
    }

    private static void handleSpecialTile(Player player, Tile tile) {
        // Delegate special tile behavior to EventManager and ensure turn advances after dialog
        showDialog(() -> {
            EventManager.triggerRandomEvent(player);
        });
    }

    private static void showSafeHavenDialog(Player player, Tile tile) {
        showDialog(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Safe Haven");
            alert.setHeaderText("Welcome to a Safe Haven! Fancy any Items?");
            alert.setContentText("This is a trading outpost where you can buy supplies and equipment.");

            ButtonType tradeButton = new ButtonType("Trade");
            ButtonType buyTileButton = new ButtonType("Buy This Safe Haven");
            ButtonType leaveButton = new ButtonType("Leave");
            ButtonType backButton = new ButtonType("Back", ButtonType.CANCEL.getButtonData());

            alert.getButtonTypes().setAll(tradeButton, buyTileButton, leaveButton, backButton);
            Optional<ButtonType> result = alert.showAndWait();
            
            if (result.isPresent()) {
                if (result.get() == buyTileButton) {
                    buySafeHaven(player, tile);
                } else if (result.get() == tradeButton) {
                    showTradeOptions(player);
                }
            }
        });
    }

    private static void showTerritoryBuyDialog(Player player, Tile tile, TerritorySpec spec) {
        showDialog(() -> {
            System.out.println("🎭 Territory buy dialog executing for " + player.getName());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(spec.displayName);
            alert.setHeaderText("Claim this location as a base?");
            alert.setContentText(
                    "Cost: " + spec.buyCost + "M | Attack required: " + spec.attackRequirement + "\n" +
                            "Fail to clear: -" + spec.failHealthLoss + " health\n" +
                            "Base rent: " + spec.baseRentDescription());

            ButtonType restoreBtn = new ButtonType("Restore (Buy)");
            ButtonType leaveBtn = new ButtonType("Leave");
            ButtonType backBtn = new ButtonType("Back", ButtonType.CANCEL.getButtonData());

            alert.getButtonTypes().setAll(restoreBtn, leaveBtn, backBtn);
            Optional<ButtonType> result = alert.showAndWait();
            
            if (result.isPresent() && result.get() == restoreBtn) {
                // Player wants to buy
                if (player.getInfluence() < spec.influenceRequirement) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Insufficient Influence");
                    errorAlert.setHeaderText("You need at least " + spec.influenceRequirement + " influence to buy this tile.");
                    errorAlert.showAndWait();
                    return;
                }
                
                if (player.getAttack() < spec.attackRequirement) {
                    player.takeDamage(spec.failHealthLoss);
                    Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                    warningAlert.setTitle("Failed to Clear");
                    warningAlert.setHeaderText("Insufficient attack to secure the area");
                    warningAlert.setContentText("You lost " + spec.failHealthLoss + " health.");
                    warningAlert.showAndWait();
                    return;
                }
                
                if (player.getMoney() < spec.buyCost) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Not Enough Money");
                    errorAlert.setContentText("You need " + spec.buyCost + "M to restore this tile.");
                    errorAlert.showAndWait();
                    return;
                }
                
                // All checks passed, buy the tile
                buyTerritoryTile(player, tile, spec);
            }
        });
    }
    
    private static void buyTerritoryTile(Player player, Tile tile, TerritorySpec spec) {
        // Spend money and update player stats
        player.spendMoney(spec.buyCost);
        tile.setOwner(player);
        tile.setUpgradeLevel(0);
        player.increaseInfluence(spec.influenceOnClaim);
        player.increaseAttack(spec.attackBonusOnClaim);
        if (spec.medicineBonusOnClaim > 0) player.addMedicine(spec.medicineBonusOnClaim);

        // Special handling for Radio Tower
        if ("Radio Tower".equals(tile.getName())) {
            BoardRegistry.tiles().values().forEach(t -> {
                if (t.getType() == TileType.SAFEHAVEN && player.equals(t.getOwner())) {
                    t.setCanSetPrices(true);
                }
            });
        }

        // Add to player history
        player.buyTile(tile, spec.buyCost);
        
        // Update game state
        GameState.notifyUpdate();

        // Show success dialog
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Base Established");
        successAlert.setHeaderText(spec.displayName + " restored!");
        successAlert.setContentText("Opponents will pay rent when landing here.");
        successAlert.showAndWait();
    }

    private static void creditSafeHavenOwnerOnTrade(Player buyer, int spend) {
        // Determine if buyer is on a Safe Haven owned by someone
        Tile current = BoardRegistry.getOrCreate(buyer.getPos(), TileType.SAFEHAVEN, TileType.SAFEHAVEN.getDisplayName());
        if (current == null || current.getType() != TileType.SAFEHAVEN) return;
        if (!current.isOwned()) return;
        Player owner = current.getOwner();
        if (owner == null || owner.getName().equals(buyer.getName())) return;
        int pct = Math.max(0, Math.min(100, current.getTradePercentage()));
        int credit = (spend * pct) / 100;
        if (credit > 0) owner.addMoney(credit);
        GameState.notifyUpdate();
    }
    private static void considerResearchCure(Player player) {
        if (!player.isAI()) {
            showDialog(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Research Cure");
                alert.setHeaderText("Do you want to spend resources to research a cure?");
                alert.setContentText("Costs: 200 money + 2 medicine");

                ButtonType researchBtn = new ButtonType("Research");
                ButtonType skipBtn = new ButtonType("Skip", ButtonType.CANCEL.getButtonData());
                alert.getButtonTypes().setAll(researchBtn, skipBtn);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == researchBtn) {
                    if (player.getMoney() >= 200 && player.getMedicine() >= 2) {
                        player.spendMoney(200);
                        player.addMedicine(-2);
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Cure Researched");
                        success.setHeaderText("You successfully researched a cure!");
                        success.showAndWait();
                    } else {
                        Alert fail = new Alert(Alert.AlertType.ERROR);
                        fail.setTitle("Insufficient Resources");
                        fail.setHeaderText("You don't have enough resources to research a cure.");
                        fail.showAndWait();
                    }
                }
            });
        }
    }

}
