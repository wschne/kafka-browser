package com.rewe.digital;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SplashScreenLoader extends Preloader {

    private Stage splashScreen;

    @Override
    public void start(Stage stage) throws Exception {
        splashScreen = stage;
        splashScreen.setScene(createScene());
        splashScreen.show();
    }

    public Scene createScene() {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 300, 200);
        return scene;
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification notification) {
        if (notification instanceof StateChangeNotification) {
            splashScreen.hide();
        }
    }

}
