package com.rewe.digital.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Named
public class StageFactory {
    @Inject
    private FXMLLoader fxmlLoader;

    private Map<String, Stage> previouslyCreatedStages = new ConcurrentHashMap<>();

    public void openOverallConnectionsStage(final String applicationVersion) {
        val stage = this.createStage("scenes/connections/overall_connections.fxml",
                "styles.css",
                "Kafka-Browser [Connections] (" + applicationVersion + ")");
        stage.setResizable(false);
        stage.show();
    }

    public Stage createStage(final String sceneFile,
                             final String sceneCssFile,
                             final String title) {
        if (previouslyCreatedStages.containsKey(sceneFile)) {
            return previouslyCreatedStages.get(sceneFile);
        } else {
            val root = getParent(sceneFile);

            val scene = new Scene(root);
            scene.getStylesheets().add(getClass().getClassLoader().getResource(sceneCssFile).toExternalForm());
            scene.setUserData(fxmlLoader);

            val newWindow = new Stage();
            newWindow.setScene(scene);
            newWindow.setTitle(title);

            previouslyCreatedStages.put(sceneFile, newWindow);
            return newWindow;
        }
    }

    public Parent getParent(String sceneFile) {
        final URL location = Objects.requireNonNull(getClass().getClassLoader().getResource(sceneFile));
        fxmlLoader.setRoot(null);
        fxmlLoader.setController(null);
        fxmlLoader.setLocation(location);
        Parent root = null;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }
}
