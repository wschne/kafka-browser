package com.rewe.digital.kafka;

import com.rewe.digital.kafka.consumer.ConsumerRecord;
import com.rewe.digital.kafka.consumer.KafkaConsumptionState;
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.time.Duration.ofSeconds;

@Named
@Slf4j
public class KafkaConsumer {
    private final KafkaConsumerFactory kafkaConsumerFactory;

    @Inject
    public KafkaConsumer(final KafkaConsumerFactory kafkaConsumerFactory) {
        this.kafkaConsumerFactory = kafkaConsumerFactory;
    }

    public Runnable startConsumer(final String topic,
                                  final int timeUntilNow,
                                  final TemporalUnit timeUnit,
                                  final KafkaConsumptionStateCallback consumptionStateCallback) {
        val consumer = kafkaConsumerFactory.get();

        val partitions = assignTopic(topic, consumer);
        setOffsetByTime(consumer, partitions, timeUntilNow, timeUnit);

        return startConsumer(topic,
                Integer.MAX_VALUE,
                consumptionStateCallback,
                consumer);
    }

    public Runnable startConsumer(final String topic,
                                  final OffsetConfig offsetConfig,
                                  final Integer totalMessagesWanted,
                                  final KafkaConsumptionStateCallback consumptionStateCallback) {
        val consumer = kafkaConsumerFactory.get();

        if (offsetConfig == OffsetConfig.LATEST) {
            val partitions = assignTopic(topic, consumer);
            rewindConsumerOffset(totalMessagesWanted, consumer, partitions);
        } else {
            val partitions = assignTopic(topic, consumer);
            rewindConsumerOffset(consumer, partitions);
        }

        return startConsumer(topic, totalMessagesWanted, consumptionStateCallback, consumer);
    }

    private Runnable startConsumer(final String topic,
                                   final Integer totalMessagesWanted,
                                   final KafkaConsumptionStateCallback consumptionStateCallback,
                                   final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer) {
        final boolean[] shouldConsume = {true};
        Runnable stopCallback = () -> shouldConsume[0] = false;

        Runnable task = () -> {
            int totalConsumedMessages = 0;
            int retryCountOnEmptyPoll = 2;
            try {
                while (totalConsumedMessages < totalMessagesWanted &&
                        retryCountOnEmptyPoll > 0 &&
                        shouldConsume[0]) {
                    val records = consumer.poll(ofSeconds(2));
                    totalConsumedMessages += records.count();
                    log.info("Received from topic {} {} messages", topic, totalConsumedMessages);
                    if (records.isEmpty()) {
                        retryCountOnEmptyPoll -= 1;
                    }
                    postCurrentBatchOfRecords(consumptionStateCallback,
                            totalMessagesWanted,
                            totalConsumedMessages,
                            records);
                }
            } catch (Exception e) {
                log.error("Error while polling for messages", e);
                consumptionStateCallback.consumptionAborted();
            }

            if (shouldConsume[0] && totalConsumedMessages >= totalMessagesWanted) {
                consumptionStateCallback.consumptionFinished();
            } else {
                consumptionStateCallback.consumptionAborted();
            }
            log.info("Stop consuming {}. Total messages consumed: {}", topic, totalConsumedMessages);
        };

        Thread thread = new Thread(task);
        thread.setUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        thread.start();

        return stopCallback;
    }

    private void postCurrentBatchOfRecords(final KafkaConsumptionStateCallback consumptionStateCallback,
                                           final Integer totalMessagesWanted,
                                           final int totalConsumedMessages,
                                           final ConsumerRecords<String, String> records) {
        val recordsCollected = StreamSupport.stream(records.spliterator(), false)
                .map(r -> new ConsumerRecord(r.key(), r.value(), r.timestamp(), r.offset(), r.partition()))
                .collect(Collectors.toList());
        consumptionStateCallback.messagesReceived(new KafkaConsumptionState(totalMessagesWanted,
                totalConsumedMessages,
                recordsCollected));
    }

    private List<TopicPartition> assignTopic(final String topic,
                                             final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> kafkaConsumer) {
        val partitionInfos = Optional.ofNullable(kafkaConsumer.listTopics().get(topic));
        val partitions = partitionInfos.map(infos -> infos.stream()
                .map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()))
                .collect(Collectors.toList())).orElseGet(ArrayList::new);

        kafkaConsumer.assign(partitions);
        return partitions;
    }

    private void rewindConsumerOffset(final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> kafkaConsumer,
                                      final List<TopicPartition> partitions) {
        kafkaConsumer.endOffsets(partitions)
                .forEach((topicPartition, partitionOffset) -> kafkaConsumer.seek(topicPartition, 0));
    }

    private void rewindConsumerOffset(final Integer totalMessagesWanted,
                                      final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> kafkaConsumer,
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

    private void setOffsetByTime(final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> topicConsumer,
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

    private Map<TopicPartition, OffsetAndTimestamp> getOffsetsByTime(final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> topicConsumer,
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

    private int getNumberOfWantedMessagesPerPartition(Integer totalMessagesWanted, List<TopicPartition> partitions) {
        return (totalMessagesWanted / partitions.size()) + 1;
    }
}
