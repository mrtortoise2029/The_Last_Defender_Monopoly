package Ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Central audio controller for menu and board music.
 * Looks for resources at /sound/Lobby.mp3 for menu and /sound/board.mp3 for board.
 * If files are missing, playback is skipped gracefully.
 */
public class AudioManager {

    private static final AudioManager INSTANCE = new AudioManager();
    public static AudioManager get() { return INSTANCE; }

    private MediaPlayer menuPlayer;
    private MediaPlayer boardPlayer;
    private boolean enabled = true;
    private boolean mediaAvailable = true;

    private AudioManager() { }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) stopAll();
    }

    public boolean isEnabled() { return enabled; }
    
    public boolean isMediaAvailable() { return mediaAvailable; }

    public void playMenuLoop() {
        if (!enabled) return;
        try {
            // Only try to initialize media if we haven't already failed
            if (mediaAvailable) {
                ensureMenuPlayer();
            }
            if (boardPlayer != null) boardPlayer.stop();
            if (menuPlayer != null) {
                menuPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                menuPlayer.play();
            }
        } catch (Exception e) {
            System.out.println("Failed to play menu loop: " + e.getMessage());
            mediaAvailable = false; // Disable audio for future calls
        }
    }

    public void playBoardLoop() {
        if (!enabled) return;
        try {
            // Only try to initialize media if we haven't already failed
            if (mediaAvailable) {
                ensureBoardPlayer();
            }
            if (menuPlayer != null) menuPlayer.stop();
            if (boardPlayer != null) {
                boardPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                boardPlayer.play();
            }
        } catch (Exception e) {
            System.out.println("Failed to play board loop: " + e.getMessage());
            mediaAvailable = false; // Disable audio for future calls
        }
    }

    public void stopAll() {
        try {
            if (menuPlayer != null) menuPlayer.stop();
            if (boardPlayer != null) boardPlayer.stop();
        } catch (Exception e) {
            System.out.println("Failed to stop audio: " + e.getMessage());
            // Continue - this is not critical
        }
    }

    private void ensureMenuPlayer() {
        if (menuPlayer != null) return;
        try {
            var url = getClass().getResource("/sound/Lobby.mp3");
            if (url == null) {
                System.out.println("Lobby.mp3 not found, skipping audio");
                mediaAvailable = false;
                return; // No audio bundled; skip gracefully
            }
            
            // Check if JavaFX media is available
            try {
                Class.forName("javafx.scene.media.MediaPlayer");
                Class.forName("javafx.scene.media.Media");
            } catch (ClassNotFoundException e) {
                System.out.println("JavaFX media not available, skipping audio");
                mediaAvailable = false;
                return;
            }
            
            // Try to initialize media with better error handling
            try {
                Media media = new Media(url.toExternalForm());
                menuPlayer = new MediaPlayer(media);
                System.out.println("Menu audio initialized successfully");
            } catch (Exception mediaException) {
                System.out.println("Failed to create media player: " + mediaException.getMessage());
                mediaAvailable = false;
            }
        } catch (Exception e) {
            System.out.println("Failed to initialize menu audio: " + e.getMessage());
            mediaAvailable = false; // Disable audio for future calls
            // Continue without audio - don't crash the application
        }
    }

    private void ensureBoardPlayer() {
        if (boardPlayer != null) return;
        try {
            // Check if JavaFX media is available
            try {
                Class.forName("javafx.scene.media.MediaPlayer");
                Class.forName("javafx.scene.media.Media");
            } catch (ClassNotFoundException e) {
                System.out.println("JavaFX media not available, skipping audio");
                mediaAvailable = false;
                return;
            }
            
            // Try to find board music in sound directory, fallback to silence
            var url = getClass().getResource("/sound/board.mp3");
            if (url == null) {
                // If no board music found, we'll just not play anything
                System.out.println("No board background music found, playing in silence");
                return;
            }
            
            // Try to initialize media with better error handling
            try {
                Media media = new Media(url.toExternalForm());
                boardPlayer = new MediaPlayer(media);
                System.out.println("Board audio initialized successfully");
            } catch (Exception mediaException) {
                System.out.println("Failed to create board media player: " + mediaException.getMessage());
                mediaAvailable = false;
            }
        } catch (Exception e) {
            System.out.println("Failed to initialize board audio: " + e.getMessage());
            mediaAvailable = false; // Disable audio for future calls
            // Continue without audio - don't crash the application
        }
    }
}


