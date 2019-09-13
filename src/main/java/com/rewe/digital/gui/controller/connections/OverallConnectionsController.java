package com.rewe.digital.gui.controller.connections;

import com.google.common.eventbus.EventBus;
import com.rewe.digital.KafkaBrowserMain;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.gui.utils.Sleeper;
import com.rewe.digital.kafka.KafkaConnectionRepository;
import com.rewe.digital.kafka.KafkaConsumerFactory;
import com.rewe.digital.messaging.events.KafkaConnectionSelectedEvent;
import com.rewe.digital.model.connection.ConnectionSettings;
import com.victorlaerte.asynctask.AsyncTask;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

@Slf4j
public class OverallConnectionsController implements Initializable {
    public ListView configuredConnectionsList;
    public TextField clusterNameField;
    public TextField bootstrapServersField;
    public Button removeConnectionButton;
    public Label connectionCheckLabel;
    public HBox connectionCheckBox;
    public Button connectButton;
    public VBox connectionDetails;

    @Inject
    SaslSslDetailsController saslSslDetailsController;

    @Inject
    KafkaConsumerFactory consumerFactory;

    @Inject
    KafkaConnectionRepository kafkaConnectionRepository;

    @Inject
    StageFactory stageFactory;

    @Inject
    EventBus eventBus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resetSettingsForm();
        configuredConnectionsList.setCellFactory(param -> new ConnectionItemFactory());
        configuredConnectionsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        configuredConnectionsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                ConnectionSettings selectedSettings = (ConnectionSettings) newValue;
                Platform.runLater(() -> {
                    connectionDetails.setDisable(false);
                    clusterNameField.setText(selectedSettings.getName());
                    bootstrapServersField.setText(selectedSettings.getBootstrapServer());
                    connectionCheckBox.setVisible(false);
                    removeConnectionButton.setDisable(false);
                    if (selectedSettings.getSecuritySettings() != null) {
                        saslSslDetailsController.applySecuritySettings(selectedSettings.getSecuritySettings());
                    }
                });
            }
        });
        Platform.runLater(() -> {
            configuredConnectionsList.setItems(FXCollections.observableArrayList(kafkaConnectionRepository.getAll()));
        });
    }

    public void addConnection(MouseEvent mouseEvent) {
        final String connectionName = "Unnamed connection";
        clusterNameField.setText(connectionName);
        configuredConnectionsList.getItems().add(0, new ConnectionSettings(connectionName, ""));
        configuredConnectionsList.getSelectionModel().select(0);
        removeConnectionButton.setDisable(false);
    }

    public void removeConnection(MouseEvent mouseEvent) {
        Platform.runLater(() -> {
            ConnectionSettings selectedConnection = getSelectedConnectionSettings();
            int selectedConnectionIndex = configuredConnectionsList.getSelectionModel().getSelectedIndex();
            configuredConnectionsList.getItems().remove(selectedConnectionIndex);
            configuredConnectionsList.getSelectionModel().selectFirst();
            resetSettingsForm();
            kafkaConnectionRepository.delete(selectedConnection);
        });
    }

    public void connectByDoubleClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1) {
            connectUsingCurrentSettings(null);
        }
    }

    public void saveCurrentSettings(ActionEvent actionEvent) {
        ConnectionSettings connectionSettings = getConnectionSettings();

        Platform.runLater(() -> {
            int itemIndex = configuredConnectionsList.getSelectionModel().getSelectedIndex();
            configuredConnectionsList.getItems().remove(itemIndex);
            configuredConnectionsList.getItems().add(itemIndex, connectionSettings);
            configuredConnectionsList.getSelectionModel().select(connectionSettings);
        });
        kafkaConnectionRepository.save(connectionSettings);
        kafkaConnectionRepository.setCurrentConnectionSettings(connectionSettings);
    }

    public void connectUsingCurrentSettings(ActionEvent actionEvent) {
        this.saveCurrentSettings(actionEvent);
        AsyncTask checkConnectionTask = new AsyncTask() {

            @Override
            public void onPreExecute() {
                Platform.runLater(() -> {
                    setConnectButtonImage("images/loading.gif");
                    connectionCheckBox.setVisible(true);
                    connectionCheckLabel.setText("checking...");
                    connectionCheckLabel.setTextFill(Paint.valueOf("orange"));
                });
            }

            @Override
            public Object doInBackground(Object[] params) {
                return consumerFactory.checkConnection((ConnectionSettings) params[0]);
            }

            @Override
            public void onPostExecute(Object connectionSuccessful) {
                Platform.runLater(() -> {
                    if (connectionSuccessful instanceof Boolean && (Boolean) connectionSuccessful) {
                        connectionCheckLabel.setText("success");
                        connectionCheckLabel.setTextFill(Paint.valueOf("green"));

                        Sleeper.executeAfterSeconds(1, () -> {
                            Stage browser = stageFactory.createStage("scenes/kafka_browser.fxml", "styles.css", "Kafka browser (" + KafkaBrowserMain.APPLICATION_VERSION + ")");

                            browser.setWidth(1724);
                            browser.setHeight(800);

                            eventBus.post(new KafkaConnectionSelectedEvent());

                            browser.show();

                            Stage stage = (Stage) connectionCheckLabel.getScene().getWindow();
                            stage.close();
                        });
                    } else {
                        connectionCheckLabel.setText("failed");
                        connectionCheckLabel.setTextFill(Paint.valueOf("red"));
                    }
                });
            }

            @Override
            public void progressCallback(Object[] params) {
            }
        };
        ConnectionSettings connectionSettings = getConnectionSettings();
        checkConnectionTask.execute(connectionSettings);
    }

    private void resetSettingsForm() {
        Platform.runLater(() -> {
            clusterNameField.setText("");
            bootstrapServersField.setText("");
            connectionCheckBox.setVisible(false);
            connectionCheckLabel.setText("");
            connectButton.setGraphic(null);
            saslSslDetailsController.resetSettings();
            configuredConnectionsList.getSelectionModel().clearSelection();
            configuredConnectionsList.getItems().clear();
            connectionDetails.setDisable(true);
        });
    }

    public void checkCurrentSettings(ActionEvent actionEvent) {
        AsyncTask checkConnectionTask = new AsyncTask() {

            @Override
            public void onPreExecute() {
                Platform.runLater(() -> {
                    connectionCheckBox.setVisible(true);
                    connectionCheckLabel.setText("checking...");
                    connectionCheckLabel.setTextFill(Paint.valueOf("orange"));
                });
            }

            @Override
            public Object doInBackground(Object[] params) {
                return consumerFactory.checkConnection((ConnectionSettings) params[0]);
            }

            @Override
            public void onPostExecute(Object connectionSuccessful) {
                log.info("Connection check successful: {}", connectionSuccessful);
                if (connectionSuccessful instanceof Boolean && (Boolean) connectionSuccessful) {
                    connectionCheckLabel.setText("success");
                    connectionCheckLabel.setTextFill(Paint.valueOf("green"));
                } else {
                    connectionCheckLabel.setText("failed");
                    connectionCheckLabel.setTextFill(Paint.valueOf("red"));
                }
            }

            @Override
            public void progressCallback(Object[] params) {
            }
        };
        ConnectionSettings connectionSettings = getConnectionSettings();
        log.info("Check connection settings {}", connectionSettings);
        checkConnectionTask.execute(connectionSettings);
    }

    private static class ConnectionItemFactory extends ListCell<ConnectionSettings> {
        @Override
        protected void updateItem(ConnectionSettings item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty) {
                setText(item.getName());
            } else {
                setText(null);
            }
        }
    }

    private ConnectionSettings getSelectedConnectionSettings() {
        return (ConnectionSettings) configuredConnectionsList.getSelectionModel().getSelectedItem();
    }

    private void setConnectButtonImage(String imageFilePath) {
        Image loadingImage = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imageFilePath)), 15, 15, false, false);
        connectButton.setGraphic(new ImageView(loadingImage));
    }

    private ConnectionSettings getConnectionSettings() {
        ConnectionSettings connectionSettings = getSelectedConnectionSettings();
        connectionSettings.setName(clusterNameField.getText());
        connectionSettings.setBootstrapServer(bootstrapServersField.getText());
        connectionSettings.setSecuritySettings(saslSslDetailsController.getSecuritySettings());
        return connectionSettings;
    }
}
