package com.rewe.digital.gui.topiclist;

import com.rewe.digital.gui.controller.ConsumerConfigController;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.kafka.KafkaConnector;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ConsumerStartStopEventHandler implements EventHandler<ActionEvent> {

    private final StageFactory stageFactory;
    private final KafkaConnector kafkaConnector;

    @Inject
    public ConsumerStartStopEventHandler(StageFactory stageFactory,
                                         KafkaConnector kafkaConnector) {
        this.stageFactory = stageFactory;
        this.kafkaConnector = kafkaConnector;
    }

    @Override
    public void handle(ActionEvent actionEvent) {
        final TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent = (TopicListItem.TopicListItemClickedEvent) actionEvent.getSource();
        final String topicName = topicListItemClickedEvent.topicName;

        if (topicListItemClickedEvent.buttonState == TopicListItem.ButtonState.stopped) {
            Stage consumerConfigWindow = stageFactory.createStage("scenes/consumer_config.fxml",
                    "styles.css",
                    "Consumer configuration");
            consumerConfigWindow.setResizable(false);
            consumerConfigWindow.setAlwaysOnTop(true);

            FXMLLoader loader = (FXMLLoader) consumerConfigWindow.getScene().getUserData();
            ConsumerConfigController consumerConfigController = loader.getController();
            consumerConfigController.initConfigUi(topicListItemClickedEvent);

            consumerConfigWindow.show();
        } else if (topicListItemClickedEvent.buttonState == TopicListItem.ButtonState.started) {
            kafkaConnector.closeConsumer(topicName);
            topicListItemClickedEvent.topicConsumerStateChangedEvent.apply(TopicListItem.ButtonState.stopped);
        }
    }
}
