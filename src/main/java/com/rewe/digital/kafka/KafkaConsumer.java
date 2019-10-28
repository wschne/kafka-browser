package com.rewe.digital.kafka;

import com.rewe.digital.kafka.consumer.ConsumerRecord;
import com.rewe.digital.kafka.consumer.KafkaConsumptionState;
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
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
                                  final OffsetConfig offsetConfig,
                                  final Integer totalMessagesWanted,
                                  final KafkaConsumptionStateCallback consumptionStateCallback) {
        final boolean[] shouldConsume = {true};
        val consumer = kafkaConsumerFactory.get();

        if(!topicExistsAndContainsData(topic, consumer)) {
            log.warn("Topic {} either not exists, or is empty.", topic);
            consumptionStateCallback.consumptionAborted();
            return () -> {};
        }

        if (offsetConfig == OffsetConfig.LATEST) {
            val partitions = assignTopic(topic, consumer);
            rewindConsumerOffset(totalMessagesWanted, consumer, partitions);
        } else {
            val partitions = assignTopic(topic, consumer);
            rewindConsumerOffset(consumer, partitions);
        }

        Runnable stopCallback = () -> shouldConsume[0] = false;

        Runnable task = () -> {
            int totalConsumedMessages = 0;
            int retryCountOnEmptyPoll = 2;
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

            if(shouldConsume[0] && totalConsumedMessages >= totalMessagesWanted) {
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
                .map(r -> new ConsumerRecord(r.key(), r.value()))
                .collect(Collectors.toList());
        consumptionStateCallback.messagesReceived(new KafkaConsumptionState(totalMessagesWanted,
                totalConsumedMessages,
                recordsCollected));
    }

    private boolean topicExistsAndContainsData(final String topic,
                                               final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> kafkaConsumer) {
        val topics = kafkaConsumer.listTopics();
        if(topics.containsKey(topic)) {
            try {
                val partitionInfos = topics.get(topic)
                        .stream()
                        .map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()))
                        .collect(Collectors.toList());
                return kafkaConsumer.endOffsets(partitionInfos, ofSeconds(2))
                        .values()
                        .stream()
                        .anyMatch(offset -> offset > 0);
            } catch (org.apache.kafka.common.errors.TimeoutException e) {
                log.error("Timeout while fetching offsets of topic {}", topic);
                return false;
            }
        } else {
            return false;
        }
    }

    private List<TopicPartition> assignTopic(final String topic,
                                             final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> kafkaConsumer) {
        List<TopicPartition> partitions = kafkaConsumer.listTopics().get(topic).stream()
                .map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()))
                .collect(Collectors.toList());

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

    private int getNumberOfWantedMessagesPerPartition(Integer totalMessagesWanted, List<TopicPartition> partitions) {
        return (totalMessagesWanted / partitions.size()) + 1;
    }
}
