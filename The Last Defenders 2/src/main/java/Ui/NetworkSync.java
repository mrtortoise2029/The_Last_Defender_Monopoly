package Ui;

/**
 * Minimal TCP-based sync: host accepts one or more clients; all state updates are
 * serialized via Gson and broadcast to all peers. Joining peer gets a full snapshot
 * and then live updates.
 */
public class NetworkSync implements GameSync {
    private final boolean isHost;
    private final String host;
    private final int port;
    private final GameSync delegate;

    public NetworkSync(boolean isHost, String host, int port) {
        this.isHost = isHost;
        this.host = host;
        this.port = port;
        if (isHost) {
            delegate = new SocketServer(port);
        } else {
            delegate = new SocketClient(host, port);
        }
    }

    @Override
    public void broadcast(GameState state) {
        delegate.broadcast(state);
    }

    @Override
    public void broadcastPlayerJoined(String playerName) {
        delegate.broadcastPlayerJoined(playerName);
    }

    @Override
    public void broadcastStartGame() {
        delegate.broadcastStartGame();
    }

    @Override
    public void subscribe(GameSyncListener listener) {
        delegate.subscribe(listener);
    }
}


