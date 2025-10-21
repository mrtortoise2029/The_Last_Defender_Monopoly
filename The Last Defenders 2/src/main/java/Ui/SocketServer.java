package Ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles server-side networking: accepts client connections, broadcasts updates.
 */
public class SocketServer implements GameSync {
    private final int port;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<GameSync.GameSyncListener> listeners = new ArrayList<>();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();
    private volatile GameState.GameSnapshot lastSnapshot;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();

    public SocketServer(int port) {
        this.port = port;
        start();
    }

    private void start() {
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (!serverSocket.isClosed()) {
                    Socket s = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(s);
                    synchronized (clients) { clients.add(handler); }
                    executor.execute(handler);
                    // Send initial snapshot immediately so joiners see the board
                    handler.sendSnapshot(GameState.createSnapshot());
                }
            } catch (IOException ignored) { }
        });
    }

    public void broadcast(GameState state) {
        lastSnapshot = GameState.createSnapshot();
        synchronized (clients) {
            for (ClientHandler c : clients) c.sendSnapshot(lastSnapshot);
        }
    }

    public void broadcastPlayerJoined(String playerName) {
        synchronized (clients) {
            for (ClientHandler c : clients) c.sendMessage("PLAYER_JOINED:" + playerName);
        }
    }

    public void broadcastStartGame() {
        synchronized (clients) {
            for (ClientHandler c : clients) c.sendMessage("START_GAME");
        }
    }

    public void subscribe(GameSync.GameSyncListener listener) {
        listeners.add(listener);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            } catch (IOException ignored) { }
        }

        void sendSnapshot(GameState.GameSnapshot snapshot) {
            if (out != null) {
                out.println(gson.toJson(snapshot));
            }
        }

        void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("PLAYER_JOINED:")) {
                        String playerName = line.substring("PLAYER_JOINED:".length());
                        for (var l : listeners) l.onPlayerJoined(playerName);
                    } else if ("PLAYER_JOINED".equals(line)) {
                        for (var l : listeners) l.onPlayerJoined("Unknown Player");
                    } else if ("START_GAME".equals(line)) {
                        for (var l : listeners) l.onStartGame();
                    } else {
                        try {
                            GameState.GameSnapshot snapshot = gson.fromJson(line, GameState.GameSnapshot.class);
                            lastSnapshot = snapshot;
                            GameState.applySnapshot(snapshot);
                            for (var l : listeners) l.onUpdate(GameState.get());
                        } catch (Exception ignored) { }
                    }
                }
            } catch (IOException ignored) { }
        }
    }
}
