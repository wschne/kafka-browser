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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
        Map<String, List<PartitionInfo>> topics = kafkaConsumer.listTopics();
        LOG.info("Fetching topics took: {}", watch);

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

    public Topic byName(final String name) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        KafkaConsumer<String, String> kafkaConsumer = kafkaConsumerFactory.get();
        final List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(name);
        LOG.info("Fetching topics took: {}", stopwatch);
        return new Topic(name, offsets(kafkaConsumer, name, partitionInfos));
    }

    private Map<Integer, Long> offsets(
            KafkaConsumer<String, String> kafkaConsumer,
            String topicName, List<PartitionInfo> infos) {
        List<TopicPartition> partitions =
                infos.stream()
                        .map(partitionInfo -> new TopicPartition(topicName, partitionInfo.partition()))
                        .collect(Collectors.toList());

        Stopwatch watch = Stopwatch.createStarted();
        Map<TopicPartition, Long> offsets = kafkaConsumer.endOffsets(partitions);
        LOG.info("Fetching offset for topic {} took: {}", topicName, watch);
        return offsets.entrySet().stream()
                .collect(Collectors.toMap(o -> o.getKey().partition(), Entry::getValue));
    }
}
