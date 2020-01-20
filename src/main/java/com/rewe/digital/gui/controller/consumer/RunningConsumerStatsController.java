package com.rewe.digital.gui.controller.consumer;

import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.kafka.topics.TopicsService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.ResourceBundle;

@Named
public class RunningConsumerStatsController implements Initializable {
    @FXML
    TitledPane consumerSettingsPane;
    @FXML
    TextField consumedMessagesText;
    @FXML
    Button buttonCloseWindow;
    @FXML
    Button buttonStopConsumer;

    @Inject
    private TopicsService topicsService;

    private TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void initConfigUi(TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent) {
        this.topicListItemClickedEvent = topicListItemClickedEvent;
        val topicName = topicListItemClickedEvent.topicName;
        Platform.runLater(() -> {
            consumerSettingsPane.setText("Consumer stats [" + topicName + "]");
        });
    }

    public void closeWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) buttonStopConsumer.getScene().getWindow();
        stage.close();
    }

    public void stopConsumer(ActionEvent actionEvent) {
    }
}
