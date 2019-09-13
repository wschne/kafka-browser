package com.rewe.digital.kafka


import com.rewe.digital.model.connection.ConnectionSettings
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.KafkaException
import spock.lang.Specification

import java.time.Duration

class KafkaConsumerFactorySpec extends Specification {

    def kafkaConnectionRepository = Mock(KafkaConnectionRepository)
    def kafkaConsumerWrapper = Mock(KafkaConsumerWrapper)
    def kafkaPropertiesBuilder = Mock(KafkaPropertiesBuilder)
    def kafkaConsumerFactory = new KafkaConsumerFactory(kafkaConnectionRepository,
            kafkaConsumerWrapper,
            kafkaPropertiesBuilder)

    def setup() {
        kafkaConsumerFactory.connectedConsumer.clear()
    }

    def "Connect to a cluster using a valid configuration"() {
        given:
        def connectionSettings = new ConnectionSettings('simple_connection', 'localhost:2182')

        and:
        def consumer = Mock(KafkaConsumer)
        def properties = Mock(Properties)

        when:
        def result = kafkaConsumerFactory.get()

        then:
        result

        and:
        1 * kafkaConnectionRepository.getCurrentConnectionSettings() >> Optional.of(connectionSettings)
        1 * kafkaPropertiesBuilder.getKafkaProperties(connectionSettings) >> properties
        1 * kafkaConsumerWrapper.getInstance(properties) >> consumer
    }

    def "If a consumer was previously initialized, reuse the same instance"() {
        given:
        def connectionSettings = new ConnectionSettings('simple_connection', 'localhost:2182')

        and:
        def consumer = Mock(KafkaConsumer)
        kafkaConnectionRepository.getCurrentConnectionSettings() >> Optional.of(connectionSettings)

        when:
        def result = kafkaConsumerFactory.get()

        then:
        result == consumer

        and:
        1 * kafkaConsumerWrapper.getInstance(_) >> consumer

        when:
        def result2 = kafkaConsumerFactory.get()

        then:
        result2 == consumer

        and:
        0 * kafkaConsumerWrapper.getInstance(_) >> consumer
    }

    def "No connection settings are currently set, throw an exception"() {
        given:
        kafkaConnectionRepository.getCurrentConnectionSettings() >> Optional.empty()

        when:
        kafkaConsumerFactory.get()

        then:
        thrown(IllegalStateException)
    }

    def "Check connection using valid settings"() {
        given:
        def connectionSettings = new ConnectionSettings('simple_connection', 'localhost:2182')

        and:
        def consumer = Mock(KafkaConsumer)

        when:
        def result = kafkaConsumerFactory.checkConnection(connectionSettings)

        then:
        result

        and:
        1 * kafkaPropertiesBuilder.getKafkaProperties(connectionSettings) >> properties
        1 * kafkaConsumerWrapper.getInstance(properties) >> consumer
        1 * consumer.listTopics(Duration.ofSeconds(2))
        1 * consumer.close()
    }

    def "Kafka throws an exception while checking connection"() {
        given:
        def connectionSettings = new ConnectionSettings('simple_connection', 'localhost:2182')

        and:
        def consumer = Mock(KafkaConsumer)

        when:
        def result = kafkaConsumerFactory.checkConnection(connectionSettings)

        then:
        result == false

        and:
        1 * kafkaPropertiesBuilder.getKafkaProperties(connectionSettings) >> properties
        1 * kafkaConsumerWrapper.getInstance(properties) >> consumer
        1 * consumer.listTopics(_) >> {throw new KafkaException()}
    }
}
