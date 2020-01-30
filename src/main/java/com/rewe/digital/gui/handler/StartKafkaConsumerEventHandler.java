package com.rewe.digital.gui.handler;

import com.google.common.eventbus.EventBus;
import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.kafka.KafkaToSparkConnector;
import com.rewe.digital.kafka.OffsetConfig;
import com.rewe.digital.kafka.consumer.KafkaConsumptionState;
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback;
import com.rewe.digital.messaging.events.WaitForKafkaMessagesEvent;
import com.rewe.digital.messaging.events.kafka.KafkaConsumptionStateEvent;
import com.rewe.digital.messaging.events.kafka.StartKafkaConsumerEvent;
import com.rewe.digital.messaging.events.querying.QueryExecutionFinishedEvent;
import com.rewe.digital.model.Query;
import com.victorlaerte.asynctask.AsyncTask;
import javafx.event.EventHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StartKafkaConsumerEventHandler implements EventHandler<StartKafkaConsumerEvent> {
    @Inject
    private KafkaToSparkConnector kafkaToSparkConnector;

    @Inject
    private EventBus eventBus;

    @Override
    public void handle(StartKafkaConsumerEvent event) {
        eventBus.post(event);

        final TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent = event.getTopicListItemClickedEvent();
        final String topicName = topicListItemClickedEvent.topicName;
        AsyncTask initConsumerTask = new AsyncTask() {
            @Override
            public void onPreExecute() {
                topicListItemClickedEvent.topicConsumerStateChangedEvent.apply(TopicListItem.ButtonState.loading);
            }

            @Override
            public Object doInBackground(Object[] params) {
                AsyncTask executeInitialQueryTask = new AsyncTask() {

                    @Override
                    public void onPreExecute() {
                    }

                    @Override
                    public Object doInBackground(Object[] params) {
                        final Query query = new Query("select * from " + topicName);
                        eventBus.post(new WaitForKafkaMessagesEvent(query));
                        return null;
                    }

                    @Override
                    public void onPostExecute(Object params) {

                    }

                    @Override
                    public void progressCallback(Object[] params) {

                    }
                };

                if (event.getTopicOffset() == OffsetConfig.EARLIEST || event.getTopicOffset() == OffsetConfig.LATEST) {
                    kafkaToSparkConnector.initKafkaConsumer(topicName,
                            event.getTopicOffset(),
                            event.getNumberOfMessages(),
                            getConsumptionStateCallbackHandler(topicName, topicListItemClickedEvent));
                } else {
                    kafkaToSparkConnector.initKafkaConsumer(topicName,
                            event.getTimeUntilNow(),
                            event.getTimeUnit(),
                            getConsumptionStateCallbackHandler(topicName, topicListItemClickedEvent));
                }
                executeInitialQueryTask.execute();
                return null;
            }

            @Override
            public void onPostExecute(Object params) {
            }

            @Override
            public void progressCallback(Object[] params) {

            }
        };
        initConsumerTask.execute();
    }


    private KafkaConsumptionStateCallback getConsumptionStateCallbackHandler(final String topicName,
                                                                             final TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent) {
        return new KafkaConsumptionStateCallback() {
            @Override
            public void messagesReceived(KafkaConsumptionState consumptionState) {
                eventBus.post(new KafkaConsumptionStateEvent(topicName,
                        consumptionState.getTotalWantedMessages(),
                        consumptionState.getTotalConsumedMessages(),
                        false,
                        consumptionState.getCurrentBatchOfMessages()));
            }

            @Override
            public void consumptionFinished() {
                topicListItemClickedEvent.topicConsumerStateChangedEvent.apply(TopicListItem.ButtonState.stopped);
                eventBus.post(new QueryExecutionFinishedEvent(topicName));
            }

            @Override
            public void consumptionAborted() {
                topicListItemClickedEvent.topicConsumerStateChangedEvent.apply(TopicListItem.ButtonState.stopped);
                eventBus.post(new QueryExecutionFinishedEvent(topicName));
            }
        };
    }

}
