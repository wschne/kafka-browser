package com.rewe.digital.kafka.topics;

import com.google.common.base.Stopwatch;
import com.rewe.digital.kafka.KafkaConsumerFactory;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.time.Duration.ofSeconds;

@Named
public class TopicsService {

    private static final Logger LOG = LoggerFactory.getLogger(TopicsService.class);

    private KafkaConsumerFactory kafkaConsumerFactory;
    private SparkSession spark;

    @Inject
    public TopicsService(KafkaConsumerFactory kafkaConsumerFactory,
                         SparkSession spark) {
        this.kafkaConsumerFactory = kafkaConsumerFactory;
        this.spark = spark;
    }

    public List<Topic> topics() {
        Stopwatch watch = Stopwatch.createStarted();
        KafkaConsumer<String, String> kafkaConsumer = kafkaConsumerFactory.get();
        Map<String, List<PartitionInfo>> topics = kafkaConsumer.listTopics(ofSeconds(1));
        LOG.info("Fetching {} topics took: {}", topics.size(), watch);

        final List<Topic> availableTopics = topics.keySet().stream()
                .map(partitionInfos -> new Topic(partitionInfos, Collections.emptyMap()))
                .collect(Collectors.toList());

        Arrays.stream(spark.streams().active())
                .map(StreamingQuery::name)
                .forEach(s -> availableTopics.stream().
                        filter(topic -> s.startsWith(topic.getId().replace("-", "_")))
                        .forEach(topic -> topic.setCurrentlyConsuming(true)));

        availableTopics.sort(Comparator.comparing(Topic::getId));

        return availableTopics;
    }
}
