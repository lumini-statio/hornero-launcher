package com.eluna.hornerolauncher.views;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

public class HorneroApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HorneroApplication.class.getResource("/com/eluna/hornerolauncher/views/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 500, 500);
        scene.getStylesheets().addAll(
                BootstrapFX.bootstrapFXStylesheet(),
                HorneroApplication.class.getResource("/com/eluna/hornerolauncher/views/main.css").toExternalForm()
        );
        stage.setTitle("Hornero Launcher");
        stage.setScene(scene);
        stage.show();
    }
}
