package com.rewe.digital.messaging;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.kafka.KafkaQueryExecutor;
import com.rewe.digital.messaging.events.querying.ExecuteQueryEvent;
import com.rewe.digital.messaging.events.querying.QueryExecutionFinishedEvent;
import com.rewe.digital.messaging.events.querying.ShowQueryResultEvent;
import com.rewe.digital.messaging.events.querying.ShowQueryingErrorEvent;
import com.victorlaerte.asynctask.AsyncTask;
import lombok.val;
import org.apache.spark.sql.AnalysisException;

import javax.inject.Inject;

public class KafkaMessageHandler implements MessageHandler {
    private final EventBus eventBus;
    private final KafkaQueryExecutor kafkaQueryExecutor;

    @Inject
    public KafkaMessageHandler(final EventBus eventBus,
                               final KafkaQueryExecutor kafkaQueryExecutor) {
        this.eventBus = eventBus;
        this.kafkaQueryExecutor = kafkaQueryExecutor;
        this.eventBus.register(this);
    }

    @Subscribe
    private void executeQueryAndShowResult(final ExecuteQueryEvent executeQueryEvent) {
        AsyncTask executeQueryTask = new AsyncTask() {

            @Override
            public void onPreExecute() {

            }

            @Override
            public Object doInBackground(Object[] params) {
                try {
                    val result = kafkaQueryExecutor.executeQuery(executeQueryEvent.getQuery());
                    val topicName = executeQueryEvent.getQuery().getTopic();
                    val resultEvent = new ShowQueryResultEvent(executeQueryEvent.getTarget(), topicName, result);
                    eventBus.post(resultEvent);
                } catch (AnalysisException e) {
                    val errorMessage = e.getSimpleMessage();
                    eventBus.post(new ShowQueryingErrorEvent(errorMessage));
                }
                return null;
            }

            @Override
            public void onPostExecute(Object resultEvent) {
                eventBus.post(new QueryExecutionFinishedEvent(executeQueryEvent.getQuery().getTopic()));
            }

            @Override
            public void progressCallback(Object[] params) {

            }
        };
        executeQueryTask.execute();
    }
}
