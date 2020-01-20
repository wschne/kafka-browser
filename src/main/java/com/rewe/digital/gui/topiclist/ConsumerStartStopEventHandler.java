package com.rewe.digital.gui.topiclist;

import com.rewe.digital.gui.controller.consumer.InitializeConsumerController;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.gui.controller.consumer.RunningConsumerStatsController;
import com.rewe.digital.kafka.KafkaToSparkConnector;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ConsumerStartStopEventHandler implements EventHandler<ActionEvent> {

    private final StageFactory stageFactory;
    private final KafkaToSparkConnector kafkaToSparkConnector;

    @Inject
    public ConsumerStartStopEventHandler(StageFactory stageFactory,
                                         KafkaToSparkConnector kafkaToSparkConnector) {
        this.stageFactory = stageFactory;
        this.kafkaToSparkConnector = kafkaToSparkConnector;
    }

    @Override
    public void handle(ActionEvent actionEvent) {
        final TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent = (TopicListItem.TopicListItemClickedEvent) actionEvent.getSource();
        final String topicName = topicListItemClickedEvent.topicName;

        if (topicListItemClickedEvent.buttonState == TopicListItem.ButtonState.stopped) {
            Stage consumerConfigWindow = stageFactory.createStage("scenes/consumer/init_consumer.fxml",
                    "styles.css",
                    "Consumer configuration");
            consumerConfigWindow.setResizable(false);
            consumerConfigWindow.setAlwaysOnTop(true);

            FXMLLoader loader = (FXMLLoader) consumerConfigWindow.getScene().getUserData();
            InitializeConsumerController initializeConsumerController = loader.getController();
            initializeConsumerController.initConfigUi(topicListItemClickedEvent);

            consumerConfigWindow.show();
        } else if (topicListItemClickedEvent.buttonState == TopicListItem.ButtonState.started ||
                   topicListItemClickedEvent.buttonState == TopicListItem.ButtonState.loading) {
            Stage consumerConfigWindow = stageFactory.createStage("scenes/consumer/running_consumer_stats.fxml",
                    "styles.css",
                    "Consumer stats");
            consumerConfigWindow.setResizable(false);
            consumerConfigWindow.setAlwaysOnTop(true);

            FXMLLoader loader = (FXMLLoader) consumerConfigWindow.getScene().getUserData();
            RunningConsumerStatsController initializeConsumerController = loader.getController();
            initializeConsumerController.initConfigUi(topicListItemClickedEvent);

            consumerConfigWindow.show();
        }
    }
}
