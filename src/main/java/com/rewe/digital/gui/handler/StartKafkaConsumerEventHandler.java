package com.rewe.digital.gui.handler;

import com.google.common.eventbus.EventBus;
import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.kafka.KafkaToSparkConnector;
import com.rewe.digital.kafka.KafkaQueryExecutor;
import com.rewe.digital.kafka.OffsetConfig;
import com.rewe.digital.kafka.consumer.KafkaConsumptionState;
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback;
import com.rewe.digital.messaging.events.TopicEmptyEvent;
import com.rewe.digital.messaging.events.WaitForKafkaMessagesEvent;
import com.rewe.digital.messaging.events.kafka.StartKafkaConsumerEvent;
import com.rewe.digital.messaging.events.querying.ExecuteQueryEvent;
import com.rewe.digital.messaging.events.querying.QueryExecutionFinishedEvent;
import com.rewe.digital.model.Query;
import com.victorlaerte.asynctask.AsyncTask;
import javafx.event.EventHandler;
import org.awaitility.core.ConditionTimeoutException;

import javax.inject.Inject;
import javax.inject.Named;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_MINUTE;

@Named
public class StartKafkaConsumerEventHandler implements EventHandler<StartKafkaConsumerEvent> {
    @Inject
    private KafkaToSparkConnector kafkaToSparkConnector;

    @Inject
    KafkaQueryExecutor kafkaQueryExecutor;

    @Inject
    private EventBus eventBus;

    @Override
    public void handle(StartKafkaConsumerEvent event) {
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
                        try {
                            await().atMost(ONE_MINUTE)
                                    .until(() -> kafkaQueryExecutor.getTopicTableStatus(topicName).getRecordCountInStore() > 0);
                            eventBus.post(new ExecuteQueryEvent(query, ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW));
                        } catch (ConditionTimeoutException e) {
                            eventBus.post(new TopicEmptyEvent(topicName));
                        }
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
