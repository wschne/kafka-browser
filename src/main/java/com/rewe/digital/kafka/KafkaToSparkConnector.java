package com.rewe.digital.kafka;

import com.rewe.digital.kafka.consumer.ConsumerRecord;
import com.rewe.digital.kafka.consumer.KafkaConsumptionState;
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback;
import com.rewe.digital.kafka.model.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
@Slf4j
public class KafkaToSparkConnector {
    private final SparkSession spark;
    private final com.rewe.digital.kafka.KafkaConsumer kafkaConsumer;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public KafkaToSparkConnector(final SparkSession spark,
                                 final com.rewe.digital.kafka.KafkaConsumer kafkaConsumer) {
        this.spark = spark;
        this.kafkaConsumer = kafkaConsumer;
    }

    public void initKafkaConsumer(final String topic,
                                  final OffsetConfig offsetConfig,
                                  final Integer totalMessagesWanted,
                                  final KafkaConsumptionStateCallback consumptionStateCallback) {
        cleanupTopicDir(topic);

        kafkaConsumer.startConsumer(topic,
                offsetConfig,
                totalMessagesWanted,
                getConsumptionStateCallbackHandler(topic, consumptionStateCallback));
    }

    public void initKafkaConsumer(final String topic,
                                  final int timeUntilNow,
                                  final TemporalUnit timeUnit,
                                  final KafkaConsumptionStateCallback consumptionStateCallback) {
        cleanupTopicDir(topic);

        kafkaConsumer.startConsumer(topic,
                timeUntilNow,
                timeUnit,
                getConsumptionStateCallbackHandler(topic, consumptionStateCallback));
    }

    private List<String> putIntoTable(final List<com.rewe.digital.kafka.consumer.ConsumerRecord> newRecords,
                                      final String topicName) {
        final String viewName = getTopicUnparsedViewName(topicName);

        Stream<ConsumerRecord> consumerRecordStream = newRecords.stream();

        final JavaSparkContext javaSparkContext = new JavaSparkContext(spark.sparkContext());

        final List<String> recordsAsRow = consumerRecordStream.map(stringStringConsumerRecord -> {
            final String key = (String) stringStringConsumerRecord.getKey();
            final String value = (String) stringStringConsumerRecord.getValue();
            final long timestamp = stringStringConsumerRecord.getTimestamp();
            final long offset = stringStringConsumerRecord.getOffset();
            final int partition = stringStringConsumerRecord.getPartition();

            KafkaMessage message = getAsJsonObject(value)
                    .map(jsonObject -> new KafkaMessage(key, partition, offset, timestamp, jsonObject))
                    .orElseGet(() -> new KafkaMessage(key, partition, offset, timestamp, value));

            try {
                return objectMapper.writeValueAsString(message);
            } catch (IOException e) {
                return "";
            }
        }).collect(Collectors.toList());

        JavaRDD<String> messagesAsJson = javaSparkContext.parallelize(recordsAsRow);
        final Dataset<Row> dataFrame = spark.read().json(messagesAsJson);
        if (Arrays.asList(spark.sqlContext().tableNames()).contains(viewName)) {
            val unionFrame = spark.sqlContext().table(viewName).union(dataFrame);
            unionFrame.createOrReplaceTempView(viewName);
        } else {
            dataFrame.createOrReplaceTempView(viewName);
        }


        return recordsAsRow;
    }

    private Optional<java.util.Map> getAsJsonObject(String json) {
        if (json != null) {
            try {
                return Optional.of(objectMapper.readValue(json, java.util.Map.class));
            } catch (IOException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
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

    private void cleanupTopicDir(String topic) {
        final File parentDir = new File("kafka_messages/" + getTopicUnparsedViewName(topic));
        try {
            FileUtils.deleteDirectory(parentDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private KafkaConsumptionStateCallback getConsumptionStateCallbackHandler(String topic, KafkaConsumptionStateCallback consumptionStateCallback) {
        return new KafkaConsumptionStateCallback() {
            @Override
            public void messagesReceived(KafkaConsumptionState consumptionState) {
                putIntoTable(consumptionState.getCurrentBatchOfMessages(), topic);
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
