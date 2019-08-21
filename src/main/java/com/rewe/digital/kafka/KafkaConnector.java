package com.rewe.digital.kafka;

import com.rewe.digital.kafka.model.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Named
@Slf4j
public class KafkaConnector {
    private final SparkSession spark;
    private final KafkaConsumerFactory kafkaConsumerFactory;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public KafkaConnector(final SparkSession spark,
                          final KafkaConsumerFactory kafkaConsumerFactory) {
        this.spark = spark;
        this.kafkaConsumerFactory = kafkaConsumerFactory;
    }

    public void initKafkaConsumer(final String topic,
                                  final OffsetConfigType offsetConfigType,
                                  final Integer totalMessagesWanted,
                                  final Runnable consumptionFinishedCallback) {
        cleanupTopicDir(topic);
        final KafkaConsumer<String, String> kafkaConsumer = kafkaConsumerFactory.get();
        List<TopicPartition> partitions = assignTopic(topic, kafkaConsumer);

        if (offsetConfigType == OffsetConfigType.LATEST) {
            rewindConsumerOffset(totalMessagesWanted, kafkaConsumer, partitions);
        }

        System.out.println("Started consuming " + topic);

        Runnable task = () -> {
            List<String> foundRecords = new ArrayList<>();
            int retryCountOnEmptyPoll = 2;
            while (foundRecords.size() < totalMessagesWanted && retryCountOnEmptyPoll > 0) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
                System.out.println("Received " + records.count());
                if (records.isEmpty()) {
                    retryCountOnEmptyPoll -= 1;
                } else {
                    foundRecords = putIntoTable(records, foundRecords, topic);
                }
            }
            consumptionFinishedCallback.run();
            System.out.println("Stop consuming " + topic + ". Total messages consumed: " + foundRecords.size());
        };

        Thread thread = new Thread(task);
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void initKafkaConsumer(final String topic,
                                  final int timeUntilNow,
                                  final TemporalUnit timeUnit,
                                  final Runnable consumptionFinishedCallback) {
        cleanupTopicDir(topic);

        final KafkaConsumer<String, String> kafkaConsumer = kafkaConsumerFactory.get();
        val partitions = assignTopic(topic, kafkaConsumer);
        setOffsetByTime(kafkaConsumer, partitions, timeUntilNow, timeUnit);

        System.out.println("Started consuming " + topic);

        Runnable task = () -> {
            int retryCountOnEmptyPoll = 2;
            List<String> foundRecords = new ArrayList<>();
            while (retryCountOnEmptyPoll > 0) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));
                System.out.println("Received " + records.count());
                if (records.isEmpty()) {
                    retryCountOnEmptyPoll -= 1;
                } else {
                    foundRecords = putIntoTable(records, foundRecords, topic);
                }
            }
            consumptionFinishedCallback.run();
            System.out.println("Stop consuming " + topic + ". Total messages consumed: " + foundRecords.size());
        };

        Thread thread = new Thread(task);
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public Long getTopicSize(final String topicName) {
        val consumer = kafkaConsumerFactory.get();
        val partitions = assignTopic(topicName, consumer);
        val partitionOffsets = consumer.endOffsets(partitions);
        return partitionOffsets.values().stream().reduce(0l, Long::sum);
    }

    private void setOffsetByTime(final KafkaConsumer<String, String> topicConsumer,
                                 final List<TopicPartition> partitions,
                                 final int timeUntilNow,
                                 final TemporalUnit timeUnit) {
        Map<TopicPartition, OffsetAndTimestamp> result = getOffsetsByTime(topicConsumer, partitions, timeUntilNow, timeUnit);

        result.entrySet().forEach(entry -> {
            final List<TopicPartition> ts = Collections.singletonList(entry.getKey());
            final Long fallbackOffset = topicConsumer.endOffsets(ts).get(entry.getKey());
            topicConsumer.seek(entry.getKey(), entry.getValue() != null ? entry.getValue().offset() : fallbackOffset);
        });
    }

    private Map<TopicPartition, OffsetAndTimestamp> getOffsetsByTime(final KafkaConsumer<String, String> topicConsumer,
                                                                     final List<TopicPartition> partitions,
                                                                     final int timeUntilNow,
                                                                     final TemporalUnit timeUnit) {
        Map<TopicPartition, Long> offsetReset = new HashMap<>();

        partitions.forEach((TopicPartition partition) -> {
            val timePeriod = Instant.now().minus(timeUntilNow, timeUnit).toEpochMilli();

            offsetReset.put(partition, timePeriod);
        });

        return topicConsumer.offsetsForTimes(offsetReset);
    }

    private List<TopicPartition> assignTopic(String topic, KafkaConsumer<String, String> kafkaConsumer) {
        List<TopicPartition> partitions = kafkaConsumer.listTopics().get(topic).stream()
                .map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()))
                .collect(Collectors.toList());

        kafkaConsumer.assign(partitions);
        return partitions;
    }

    private void rewindConsumerOffset(final Integer totalMessagesWanted,
                                      final KafkaConsumer<String, String> kafkaConsumer,
                                      final List<TopicPartition> partitions) {
        int finalNumberOfWantedMessagesPerPartition = getNumberOfWantedMessagesPerPartition(totalMessagesWanted, partitions);
        kafkaConsumer.endOffsets(partitions)
                .forEach((topicPartition, partitionOffset) -> {
                    final long offset = partitionOffset - finalNumberOfWantedMessagesPerPartition;
                    if (offset > 0) {
                        kafkaConsumer.seek(topicPartition, offset);
                    }
                });
    }

    private int getNumberOfWantedMessagesPerPartition(Integer totalMessagesWanted, List<TopicPartition> partitions) {
        return (totalMessagesWanted / partitions.size()) + 1;
    }

    private List<String> putIntoTable(ConsumerRecords<String, String> newRecords, List<String> previousRecords, String topicName) {
        final String viewName = getTopicUnparsedViewName(topicName);

        Stream<ConsumerRecord<String, String>> consumerRecordStream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(newRecords.iterator(), Spliterator.ORDERED),
                false);

        final JavaSparkContext javaSparkContext = new JavaSparkContext(spark.sparkContext());

        final List<String> recordsAsRow = consumerRecordStream.map(stringStringConsumerRecord -> {
            final String value = stringStringConsumerRecord.value();
            final long timestamp = stringStringConsumerRecord.timestamp();
            final long offset = stringStringConsumerRecord.offset();
            final int partition = stringStringConsumerRecord.partition();
            final String key = stringStringConsumerRecord.key();

            KafkaMessage message = getAsJsonObject(value)
                    .map(jsonObject -> new KafkaMessage(key, partition, offset, timestamp, jsonObject))
                    .orElseGet(() -> new KafkaMessage(key, partition, offset, timestamp, value));

            try {
                return objectMapper.writeValueAsString(message);
            } catch (IOException e) {
                return "";
            }
        }).collect(Collectors.toList());

        recordsAsRow.addAll(previousRecords);

        JavaRDD<String> messagesAsJson = javaSparkContext.parallelize(recordsAsRow);
        final Dataset<Row> dataFrame = spark.read().json(messagesAsJson);

        dataFrame.createOrReplaceTempView(viewName);

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

    public Optional<StreamingQuery> getTopicStream(final String topic) {
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
}
