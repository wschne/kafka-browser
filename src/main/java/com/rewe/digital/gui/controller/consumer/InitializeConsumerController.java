package com.rewe.digital.gui.controller.consumer;

import com.rewe.digital.gui.handler.StartKafkaConsumerEventHandler;
import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.kafka.OffsetConfig;
import com.rewe.digital.kafka.topics.TopicsService;
import com.rewe.digital.messaging.events.kafka.StartKafkaConsumerEvent;
import com.victorlaerte.asynctask.AsyncTask;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ResourceBundle;

@Named
public class InitializeConsumerController implements Initializable {
    @FXML
    Label consumeMessagesInfo;
    @FXML
    ComboBox timeUnitSelection;
    @FXML
    TextField timeUntilNow;
    @FXML
    TitledPane consumerSettingsPane;
    @FXML
    TextField topicContainsDataField;
    @FXML
    Button buttonCloseWindow;
    @FXML
    Button buttonStartConsumer;
    @FXML
    ComboBox offsetSelection;
    @FXML
    TextField textBoxAmountOfMessagesToConsume;

    @Inject
    private TopicsService topicsService;

    @Inject
    private StartKafkaConsumerEventHandler startKafkaConsumerEventHandler;

    private TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void initConfigUi(TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent) {
        this.topicListItemClickedEvent = topicListItemClickedEvent;
        val topicName = topicListItemClickedEvent.topicName;
        Platform.runLater(() -> {
            consumerSettingsPane.setText("Consumer config [" + topicName + "]");
        });

        val fetchTopicSize = new AsyncTask<Object, Object, Boolean>() {
            @Override
            public void onPreExecute() {
                Platform.runLater(() -> { topicContainsDataField.setText("calculating..."); });
            }

            @Override
            public Boolean doInBackground(Object[] params) {
                return topicsService.isTopicContainsData(topicName);
            }

            @Override
            public void onPostExecute(Boolean topicContainsData) {
                Platform.runLater(() -> {
                    topicContainsDataField.setText(Boolean.toString(topicContainsData));
                    if (topicContainsData) {
                        buttonStartConsumer.setDisable(false);
                        buttonStartConsumer.setText("Start consumer");
                    } else {
                        buttonStartConsumer.setDisable(true);
                        buttonStartConsumer.setText("Topic is empty");
                    }
                });
            }

            @Override
            public void progressCallback(Object[] params) { }
        };

        fetchTopicSize.execute(topicName);
    }

    public void closeWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) buttonStartConsumer.getScene().getWindow();
        stage.close();
    }

    public void startConsumer(ActionEvent actionEvent) {
        final StartKafkaConsumerEvent event = getStartKafkaConsumerEvent();
        startKafkaConsumerEventHandler.handle(event);
        closeWindow(actionEvent);
    }

    private StartKafkaConsumerEvent getStartKafkaConsumerEvent() {
        final OffsetConfig topicOffset = getOffsetType();
        final String topicName = topicListItemClickedEvent.topicName;
        if (topicOffset == OffsetConfig.TIME_OFFSET) {
            final Integer time = Integer.valueOf(timeUntilNow.getText());
            final TemporalUnit timeUnit = getSelectedTemporalUnit();
            return new StartKafkaConsumerEvent(EventType.ROOT,
                    topicName,
                    time,
                    timeUnit,
                    topicListItemClickedEvent);
        } else {
            final int numberOfMessages = Integer.parseInt(textBoxAmountOfMessagesToConsume.getText());
            return new StartKafkaConsumerEvent(EventType.ROOT,
                    topicName,
                    topicOffset,
                    numberOfMessages,
                    topicListItemClickedEvent);
        }
    }

    private TemporalUnit getSelectedTemporalUnit() {
        final String selectedItem = (String) timeUnitSelection.getSelectionModel().getSelectedItem();
        if ("MINUTES".equals(selectedItem)) {
            return ChronoUnit.MINUTES;
        } else if ("HOURS".equals(selectedItem)) {
            return ChronoUnit.HOURS;
        } else if ("DAYS".equals(selectedItem)) {
            return ChronoUnit.DAYS;
        } else {
            return ChronoUnit.HOURS;
        }
    }

    private OffsetConfig getOffsetType() {
        final String selectedOffset = (String) offsetSelection.getSelectionModel().getSelectedItem();
        return OffsetConfig.valueOf(selectedOffset);
    }

    public void setVisibleInput(ActionEvent actionEvent) {
        if (getOffsetType() == OffsetConfig.TIME_OFFSET) {
            timeUnitSelection.setVisible(true);
            timeUntilNow.setVisible(true);
            consumeMessagesInfo.setVisible(false);
            textBoxAmountOfMessagesToConsume.setVisible(false);
        } else {
            consumeMessagesInfo.setVisible(true);
            textBoxAmountOfMessagesToConsume.setVisible(true);
            timeUnitSelection.setVisible(false);
            timeUntilNow.setVisible(false);
        }
    }
}
