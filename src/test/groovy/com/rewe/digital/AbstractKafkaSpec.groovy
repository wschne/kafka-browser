package com.rewe.digital

import com.rewe.digital.kafka.KafkaConsumerFactory
import com.rewe.digital.utils.kafka.SecuredKafkaContainer
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import spock.lang.Shared

abstract class AbstractKafkaSpec extends AbstractApplicationSpec {
    @Shared
    static SecuredKafkaContainer kafkaContainer = new SecuredKafkaContainer()
    static KafkaConsumerFactory kafkaConsumerFactory

    def setupSpec() {
        kafkaConsumerFactory = context.getInstance(KafkaConsumerFactory)
    }

    def setup() {
        kafkaConsumerFactory?.connectedConsumer?.close(20, true)
    }

    static {
        GenericContainer zookeeper = new GenericContainer("confluentinc/cp-zookeeper:4.0.0")
                .withNetwork(kafkaContainer.getNetwork())
                .withNetworkAliases("zookeeper")
                .withEnv('JMX_OPTS', "-Djava.security.auth.login.config=/etc/kafka/server-jaas.conf")
                .withClasspathResourceMapping('kafka_server_conf/server-jaas.conf', '/etc/kafka/server-jaas.conf', BindMode.READ_ONLY)
                .withEnv("ZOOKEEPER_CLIENT_PORT", "2181");

        zookeeper.start()
        kafkaContainer.start()
        conditions.within(10) {
            kafkaContainer.isRunning()
        }
    }
}
