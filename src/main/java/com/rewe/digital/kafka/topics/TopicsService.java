package com.rewe.digital.kafka.topics;

import com.google.common.base.Stopwatch;
import com.rewe.digital.kafka.KafkaConsumerFactory;
import lombok.val;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.time.Duration.of;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

@Named
public class TopicsService {

    private static final Logger LOG = LoggerFactory.getLogger(TopicsService.class);

    private KafkaConsumerFactory kafkaConsumerFactory;

    @Inject
    public TopicsService(KafkaConsumerFactory kafkaConsumerFactory) {
        this.kafkaConsumerFactory = kafkaConsumerFactory;
    }

    public List<Topic> topics() {
        Stopwatch watch = Stopwatch.createStarted();
        val kafkaConsumer = kafkaConsumerFactory.get();
        val topics = kafkaConsumer.listTopics(ofSeconds(2));
        LOG.info("Fetching {} topics took: {}", topics.size(), watch);

        return topics.keySet().stream()
                .map(Topic::new)
                .sorted(Comparator.comparing(Topic::getId))
                .collect(Collectors.toList());
    }

    public boolean isTopicContainsData(final String topicName) {
        KafkaConsumer<String, String> kafkaConsumer = kafkaConsumerFactory.get();
        TopicPartition topicPartition = new TopicPartition(topicName, 0);
        List<TopicPartition> topics = Arrays.asList(topicPartition);
        kafkaConsumer.assign(topics);
        kafkaConsumer.seekToEnd(topics);
        try {
            long current = kafkaConsumer.position(topicPartition, of(2, ChronoUnit.SECONDS));
            kafkaConsumer.seek(topicPartition, getOffset(current));
            return !kafkaConsumer.poll(ofMillis(500)).isEmpty();
        } catch (final KafkaException e) {
            return true;
        }
    }

    private long getOffset(long current) {
        if (current > 1000) {
            return current-1000;
        } else {
            return current;
        }
    }
}
