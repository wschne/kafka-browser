package com.rewe.digital.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import javax.inject.Named;
import java.util.Properties;

@Named
public class KafkaConsumerWrapper {
    public <K,V> KafkaConsumer<K,V> getInstance(final Properties properties) {
        return new KafkaConsumer<K, V>(properties);
    }
}
