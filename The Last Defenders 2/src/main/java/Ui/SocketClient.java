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
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles client-side networking: connects to server, receives updates.
 */
public class SocketClient implements GameSync {
    private final String host;
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

    private Socket clientSocket;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
        start();
    }

    private void start() {
        executor.execute(() -> {
            try {
                clientSocket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
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
        });
    }

    public void broadcast(GameState state) {
        // Clients don't broadcast; host is authoritative
        sendToHost(GameState.createSnapshot());
    }

    public void broadcastPlayerJoined(String playerName) {
        // Clients can send player joined messages to host
        sendToHost("PLAYER_JOINED:" + playerName);
    }

    public void broadcastStartGame() {
        // Clients can send start game to host
        sendToHost("START_GAME");
    }

    public void subscribe(GameSync.GameSyncListener listener) {
        listeners.add(listener);
        // If a snapshot was already received, deliver immediately
        GameState.GameSnapshot snapshot = lastSnapshot;
        if (snapshot != null) {
            try {
                GameState.applySnapshot(snapshot);
                listener.onUpdate(GameState.get());
            } catch (Throwable ignored) {}
        }
    }

    private void sendToHost(GameState.GameSnapshot snapshot) {
        executor.execute(() -> {
            try {
                if (clientSocket == null) return;
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                out.println(gson.toJson(snapshot));
            } catch (IOException ignored) { }
        });
    }

    private void sendToHost(String message) {
        executor.execute(() -> {
            try {
                if (clientSocket == null) return;
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                out.println(message);
            } catch (IOException ignored) { }
        });
    }
}
