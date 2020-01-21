package com.rewe.digital.kafka

import com.rewe.digital.utils.kafka.SecuredKafkaContainer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG

trait KafkaTestSetup {
    static PollingConditions conditions = new PollingConditions()

    private static KafkaProducer kafkaProducer

    static SecuredKafkaContainer firstKafkaContainer = new SecuredKafkaContainer()
    static SecuredKafkaContainer secondKafkaContainer = new SecuredKafkaContainer(9192,
            2281,
            9193,
            9293,
            'c2_test_kafka_topic_1:1:1,c2_test_kafka_topic_2:1:1,c2_test_kafka_topic_3:1:1')

    static {
        def firstZookeeper = setupZookeeper(firstKafkaContainer, "2181")
        firstZookeeper.start()
        firstKafkaContainer.start()

        def secondZookeeper = setupZookeeper(secondKafkaContainer, "2281")
        secondZookeeper.start()
        secondKafkaContainer.start()

        conditions.within(10) {
            firstKafkaContainer.isReady() &&
            secondKafkaContainer.isReady()
        }
        System.out.println("Kafka container started")
    }

    private static GenericContainer setupZookeeper(SecuredKafkaContainer kafkaContainer, String zooKeeperPort) {
        GenericContainer zookeeper = new GenericContainer("confluentinc/cp-zookeeper:4.0.0")
                .withNetwork(kafkaContainer.getNetwork())
                .withNetworkAliases("zookeeper")
                .withEnv('JMX_OPTS', "-Djava.security.auth.login.config=/etc/kafka/server-jaas.conf")
                .withClasspathResourceMapping('kafka_server_conf/server-jaas.conf', '/etc/kafka/server-jaas.conf', BindMode.READ_ONLY)
                .withEnv("ZOOKEEPER_CLIENT_PORT", zooKeeperPort);
        zookeeper
    }

    static org.apache.kafka.clients.consumer.KafkaConsumer<String, String> getConsumer() {
        Properties properties = getKafkaProperties()
        return new org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(properties)
    }

    static KafkaProducer<String, String> getProducer() {
        if (!kafkaProducer) {
            kafkaProducer = new KafkaProducer<String, String>(getKafkaProperties())
        }

        kafkaProducer
    }

    static void withKafkaMessages(String topic, int numberOfMessages) {
        def producer = getProducer()
        println producer.properties
        (1..numberOfMessages).each { int i ->
            producer.send(new ProducerRecord<String, String>(topic, "${i}_key" as String, "${i}_value" as String))
        }
    }

    static List<LinkedHashMap<String, String>> withKafkaMessages(String topic, List<LinkedHashMap<String, String>> messages) {
        def producer = getProducer()
        messages.each {
            producer.send(new ProducerRecord<String, String>(topic, it.key, it.value))
        }
    }

    private static Properties getKafkaProperties() {
        Properties properties = new Properties();
        properties.setProperty(BOOTSTRAP_SERVERS_CONFIG, firstKafkaContainer.getPlaintextBootstrapServers());

        properties.setProperty(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.setProperty(ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties
    }
}
