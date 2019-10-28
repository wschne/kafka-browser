package com.rewe.digital

import com.rewe.digital.kafka.KafkaConsumerFactory
import com.rewe.digital.kafka.KafkaTestSetup
import com.rewe.digital.utils.kafka.SecuredKafkaContainer
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import spock.lang.Shared

abstract class AbstractApplicationWithKafkaSpec extends AbstractApplicationSpec implements KafkaTestSetup {
    static KafkaConsumerFactory kafkaConsumerFactory

    def setupSpec() {
        kafkaConsumerFactory = context.getInstance(KafkaConsumerFactory)
    }

    def setup() {
        kafkaConsumerFactory?.connectedConsumer.values().each {it?.close(20, true)}
    }
}
