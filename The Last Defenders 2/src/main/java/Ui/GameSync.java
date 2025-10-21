package Ui;

// A simple interface you can later back with sockets/WebSocket/db.
public interface GameSync {
    void broadcast(GameState state);
    void broadcastPlayerJoined(String playerName);
    void broadcastStartGame();
    void subscribe(GameSyncListener listener);

    interface GameSyncListener {
        void onUpdate(GameState state);
        void onPlayerJoined(String playerName);
        void onStartGame();
    }
}

