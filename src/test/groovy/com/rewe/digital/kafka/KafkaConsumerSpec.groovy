package com.rewe.digital.kafka


import com.rewe.digital.kafka.consumer.ConsumerRecord
import com.rewe.digital.kafka.consumer.KafkaConsumptionState
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class KafkaConsumerSpec extends Specification implements KafkaTestSetup {
    def consumerFactory = Mock(KafkaConsumerFactory) {
        get() >> getConsumer()
    }
    def kafkaConsumer = new KafkaConsumer(consumerFactory)

    @Shared
    def topic = 'test_kafka_topic_1'

    def setup() {
        this.kafkaConsumptionStateEvent = new KafkaConsumptionState(0, 0)
        this.isConsumerFinished = false
        this.isConsumptionAborted = false
    }

    def setupSpec() {
        withKafkaMessages(topic, 10000)
    }

    @Unroll
    def "Consume #wantedMessages starting with #offsetSetting"() {
        when:
        kafkaConsumer.startConsumer(topic,
                offsetSetting,
                wantedMessages,
                callbackFn)

        then:
        conditions.within(5, {
            assert kafkaConsumptionStateEvent?.totalWantedMessages == wantedMessages
            assert kafkaConsumptionStateEvent?.totalConsumedMessages >= wantedMessages
            assert isConsumerFinished
            assert containsMessages(expectedKafkaMessages, kafkaConsumptionStateEvent?.currentBatchOfMessages) == true
        })

        where:
        wantedMessages | offsetSetting         | expectedKafkaMessages
        1              | OffsetConfig.LATEST   | [[key: '10000_key', value: '10000_value']]
        4              | OffsetConfig.LATEST   | [[key: '9997_key', value: '9997_value'],
                                                  [key: '9998_key', value: '9998_value'],
                                                  [key: '9999_key', value: '9999_value'],
                                                  [key: '10000_key', value: '10000_value']]
        1              | OffsetConfig.EARLIEST | [[key: '1_key', value: '1_value']]
    }

    def "The consumer stops consumption if there are no more messages coming in and stream processing is not wanted"() {
        given:
        def wantedMessages = 100000

        and:
        withKafkaMessages('test_kafka_topic_2', 1)

        when:
        kafkaConsumer.startConsumer('test_kafka_topic_2', OffsetConfig.EARLIEST, wantedMessages, callbackFn)

        then:
        conditions.within(10, {
            assert !isConsumerFinished
            assert isConsumptionAborted
            assert kafkaConsumptionStateEvent?.totalWantedMessages == wantedMessages
            assert kafkaConsumptionStateEvent?.totalConsumedMessages < wantedMessages
        })
    }

    def "Stop a running consumer"() {
        given:
        def wantedMessages = 100000

        when:
        def stopCallback = kafkaConsumer.startConsumer(topic, OffsetConfig.EARLIEST, wantedMessages, callbackFn)

        then:
        conditions.within(2, {
            assert isConsumerFinished == false
            assert isConsumptionAborted == false
        })

        when:
        stopCallback.run();

        then:
        conditions.within(2, {
            assert isConsumptionAborted == true
        })
    }

    @Unroll
    def "Try to consume #topicName topic"() {
        given:
        def wantedMessages = 10

        when:
        kafkaConsumer.startConsumer(topicName, OffsetConfig.EARLIEST, wantedMessages, callbackFn)

        then:
        conditions.within(2, {
            assert isConsumerFinished == false
            assert isConsumptionAborted == true
        })

        where:
        topicName      | _
        'empty'        | _
        'not_existent' | _
    }

    boolean containsMessages(List<Map> expectedMessages, List<ConsumerRecord> actualMessages) {
        expectedMessages.every { m ->
            if (actualMessages) {
                def contains = actualMessages.contains(new ConsumerRecord(m.key, m.value))
                if (!contains) System.out.println("Message is not part of received messages: " + m)
                return contains
            } else {
                false
            }
        }
    }

    KafkaConsumptionState kafkaConsumptionStateEvent
    boolean isConsumerFinished = false
    boolean isConsumptionAborted = false

    def callbackFn = new KafkaConsumptionStateCallback() {
        @Override
        void messagesReceived(KafkaConsumptionState consumptionState) {
            def previousMessages = kafkaConsumptionStateEvent ? kafkaConsumptionStateEvent.currentBatchOfMessages : []
            kafkaConsumptionStateEvent = consumptionState
            kafkaConsumptionStateEvent.currentBatchOfMessages?.addAll(previousMessages)
        }

        @Override
        void consumptionFinished() {
            isConsumerFinished = true
        }

        @Override
        void consumptionAborted() {
            isConsumptionAborted = true
        }
    }
}
