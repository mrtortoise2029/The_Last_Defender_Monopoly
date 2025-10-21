# TODO: Fix AI Game Issues

## 1. Prevent Dice Animation in Lobby After AI Game ✅
- **File**: BoardController.java
- **Issue**: Dice animation triggers when returning to lobby if GameState has lastRoll > 0
- **Fix**: In initialize(), set lastShownRoll to current lastRoll to prevent animation on load

## 2. Slow Down AI Dice Rolling Animation ✅
- **File**: BoardController.java
- **Issue**: AI dice animation is too fast (55ms per frame)
- **Fix**: Increase frame duration to 70ms for slower animation

## 3. Enable AI Decision on Scenario Tiles ✅
- **File**: ScenarioManager.java
- **Issue**: AI players don't make decisions on scenario tiles; they take decisions automatically
- **Fix**: In showScenarioDialog, if player.isAI(), decide based on success chance (>50% take risk, else safe) and apply outcomes without dialogs
