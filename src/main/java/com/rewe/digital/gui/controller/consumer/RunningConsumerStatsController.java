package com.rewe.digital.gui.controller.consumer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.messaging.events.kafka.KafkaConsumptionStateEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.ResourceBundle;

@Named
public class RunningConsumerStatsController implements Initializable {
    @FXML
    ProgressBar consumptionProcessBar;
    @FXML
    TitledPane consumerSettingsPane;
    @FXML
    TextField consumedMessagesText;
    @FXML
    Button buttonCloseWindow;
    @FXML
    Button buttonStopConsumer;

    @Inject
    private EventBus eventBus;

    private String topicName;

    private TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        eventBus.register(this);
    }

    public void initConfigUi(TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent) {
        this.topicListItemClickedEvent = topicListItemClickedEvent;
        this.topicName = topicListItemClickedEvent.topicName;
        Platform.runLater(() -> {
            consumerSettingsPane.setText("Consumer stats [" + topicName + "]");
        });
    }

    @Subscribe
    public void setReceiveConsumerData(final KafkaConsumptionStateEvent consumptionStateEvent) {
        if (this.topicName.equalsIgnoreCase(consumptionStateEvent.getTopicName())) {
            Platform.runLater(() -> {
                consumptionProcessBar.setProgress(consumptionStateEvent.getTotalConsumedMessages() / consumptionStateEvent.getTotalWantedMessages());
                consumedMessagesText.setText(consumptionStateEvent.getTotalConsumedMessages() + "/" + consumptionStateEvent.getTotalWantedMessages());
            });
        }
    }

    public void closeWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) buttonStopConsumer.getScene().getWindow();
        stage.close();
    }

    public void stopConsumer(ActionEvent actionEvent) {
    }
}
