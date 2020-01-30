package com.rewe.digital.kafka;

import com.rewe.digital.kafka.consumer.ConsumerRecord;
import com.rewe.digital.kafka.consumer.KafkaConsumptionState;
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.EMPTY_LIST;

@Named
@Slf4j
public class KafkaToSparkConnector {
    private final SparkSession spark;
    private final com.rewe.digital.kafka.KafkaConsumer kafkaConsumer;
    private final ConsumerRecordTransformer consumerRecordTransformer;

    @Inject
    public KafkaToSparkConnector(final SparkSession spark,
                                 final KafkaConsumer kafkaConsumer,
                                 final ConsumerRecordTransformer consumerRecordTransformer) {
        this.spark = spark;
        this.kafkaConsumer = kafkaConsumer;
        this.consumerRecordTransformer = consumerRecordTransformer;
    }

    public void initKafkaConsumer(final String topic,
                                  final OffsetConfig offsetConfig,
                                  final Integer totalMessagesWanted,
                                  final KafkaConsumptionStateCallback consumptionStateCallback) {
        kafkaConsumer.startConsumer(topic,
                offsetConfig,
                totalMessagesWanted,
                getConsumptionStateCallbackHandler(topic, consumptionStateCallback));
    }

    public void initKafkaConsumer(final String topic,
                                  final int timeUntilNow,
                                  final TemporalUnit timeUnit,
                                  final KafkaConsumptionStateCallback consumptionStateCallback) {
        kafkaConsumer.startConsumer(topic,
                timeUntilNow,
                timeUnit,
                getConsumptionStateCallbackHandler(topic, consumptionStateCallback));
    }

    private void putIntoTable(final List<ConsumerRecord> newRecords,
                              final String topicName) {
        final String viewName = getTopicUnparsedViewName(topicName);

        final JavaSparkContext javaSparkContext = new JavaSparkContext(spark.sparkContext());

        val recordsAsRow = consumerRecordTransformer.toJson(newRecords);
        mergeWithExistingTable(viewName, javaSparkContext, recordsAsRow);
    }

    @Synchronized
    private void mergeWithExistingTable(final String viewName,
                                        final JavaSparkContext javaSparkContext,
                                        final List<String> recordsAsRow) {
        List<String> previousRecords = getPreviousRecords(viewName);
        recordsAsRow.addAll(previousRecords);

        JavaRDD<String> messagesAsJson = javaSparkContext.parallelize(recordsAsRow);
        final Dataset<Row> dataFrame = spark.read().json(messagesAsJson);

        dataFrame.createOrReplaceTempView(viewName);
    }

    private List<String> getPreviousRecords(String viewName) {
        if (Arrays.stream(spark.sqlContext().tableNames()).anyMatch(t -> t.equalsIgnoreCase(viewName))) {
            return spark.sqlContext().table(viewName).toJSON().collectAsList();
        } else {
            return EMPTY_LIST;
        }
    }

    public void closeConsumer(String topic) {
        getTopicStream(topic).ifPresent(StreamingQuery::stop);
    }

    private Optional<StreamingQuery> getTopicStream(final String topic) {
        String viewName = getTopicUnparsedViewName(topic);
        return Arrays.stream(spark.streams().active())
                .filter(streamingQuery -> streamingQuery.name().equalsIgnoreCase(viewName))
                .findFirst();
    }

    private String getTopicUnparsedViewName(String topic) {
        return topic.replace("-", "_");
    }

    private KafkaConsumptionStateCallback getConsumptionStateCallbackHandler(final String topic,
                                                                             final KafkaConsumptionStateCallback consumptionStateCallback) {
        return new KafkaConsumptionStateCallback() {
            ExecutorService executor = Executors.newFixedThreadPool(5);
            List<ConsumerRecord> consumedRecords = new ArrayList<>();

            @Override
            public void messagesReceived(KafkaConsumptionState consumptionState) {
                consumptionStateCallback.messagesReceived(consumptionState);
                consumedRecords.addAll(consumptionState.getCurrentBatchOfMessages());
                Runnable task = () -> putIntoTable(consumptionState.getCurrentBatchOfMessages(), topic);
                executor.submit(task);
            }

            @Override
            public void consumptionFinished() {
                consumptionStateCallback.consumptionFinished();
            }

            @Override
            public void consumptionAborted() {
                consumptionStateCallback.consumptionAborted();
            }
        };
    }
}
