package com.rewe.digital.gui.controls;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

public class FilePicker extends HBox {
    public FilePicker() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("scenes/controls/file_picker.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            getNode(Button.class).ifPresent(button -> {
                final InputStream openFileImageResource = getClass().getClassLoader().getResourceAsStream("images/open_file.png");
                if (openFileImageResource != null) {
                    val openFileImage = new Image(Objects.requireNonNull(openFileImageResource), 15, 15, false, false);
                    button.setGraphic(new ImageView(openFileImage));
                }
                button.setOnAction(event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Open file");
                    val chosenFile = fileChooser.showOpenDialog(((Node)event.getTarget()).getScene().getWindow());
                    this.setText(chosenFile.getAbsolutePath());
                });
            });
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

    public String getText() {
        return getNode(TextField.class).map(TextInputControl::getText).orElse("");
    }

    public void setText(String filePath) {
        getNode(TextField.class).ifPresent(textField -> textField.setText(filePath));
    }

    private <T> Optional<T> getNode(Class<T> nodeType) {
        return this.getChildren().stream().filter(node -> node.getClass().isAssignableFrom(nodeType)).map(node -> (T) node).findFirst();
    }
}
