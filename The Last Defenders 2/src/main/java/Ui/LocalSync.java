package Ui;

import java.util.ArrayList;
import java.util.List;

// Local in-memory sync (single-JVM). Swap this later with real networking.
public class LocalSync implements GameSync {
    private final List<GameSyncListener> listeners = new ArrayList<>();

    @Override
    public void broadcast(GameState state) {
        // push updates to all listeners
        for (var l : listeners) l.onUpdate(state);
    }

    @Override
    public void broadcastPlayerJoined(String playerName) {
        for (var l : listeners) l.onPlayerJoined(playerName);
    }

    @Override
    public void broadcastStartGame() {
        for (var l : listeners) l.onStartGame();
    }

    @Override
    public void subscribe(GameSyncListener listener) {
        listeners.add(listener);
        // initial snapshot
        listener.onUpdate(GameState.get());
    }
}

