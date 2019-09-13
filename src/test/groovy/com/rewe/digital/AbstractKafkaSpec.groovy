package com.rewe.digital

import com.rewe.digital.kafka.KafkaConsumerFactory
import com.rewe.digital.utils.kafka.SecuredKafkaContainer
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import spock.lang.Shared

abstract class AbstractKafkaSpec extends AbstractApplicationSpec {
    @Shared
    static SecuredKafkaContainer firstKafkaContainer = new SecuredKafkaContainer()
    @Shared
    static SecuredKafkaContainer secondKafkaContainer = new SecuredKafkaContainer(9192,
            2281,
            9193,
            9293,
    'second_kafka_topic_1:1:1,second_kafka_topic_2:1:1,second_kafka_topic_3:1:1')
    static KafkaConsumerFactory kafkaConsumerFactory

    def setupSpec() {
        kafkaConsumerFactory = context.getInstance(KafkaConsumerFactory)
    }

    def setup() {
        kafkaConsumerFactory?.connectedConsumer.values().each {it?.close(20, true)}
    }

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
}
