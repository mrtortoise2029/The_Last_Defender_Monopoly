package Ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.ServerSocket;

public class main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Disable auto-coop to show main menu only
        GameState.get().setAutoCoop(false);
        GameState.get().setHostRole(false);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Ui/Lobby.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("The Last Defenders");
        stage.setScene(scene);

        // Auto-save on window close
        stage.setOnCloseRequest(event -> {
            System.out.println("Auto-saving game before exit...");
            GameState.get().saveToFile();
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
