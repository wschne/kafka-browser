package com.rewe.digital.kafka.topics

import com.rewe.digital.kafka.KafkaConsumerFactory
import com.rewe.digital.kafka.KafkaTestSetup
import spock.lang.Specification
import spock.lang.Unroll

class TopicsServiceSpec extends Specification implements KafkaTestSetup {
    def consumerFactory = Mock(KafkaConsumerFactory) {
        get() >> getConsumer()
    }

    def topicsService = new TopicsService(consumerFactory)

    def setupSpec() {
        withKafkaMessages('non_empty_topic', 10)
    }

    def "Get a list of topics"() {
        when:
        def topics = topicsService.topics()

        then:
        topics.size() > 0
    }

    @Unroll
    def "Check if topic #topicName contains data: #expectToContainData"() {
        when:
        def isTopicConainsData = topicsService.isTopicContainsData(topicName)

        then:
        isTopicConainsData == expectToContainData

        where:
        topicName         | expectToContainData
        'non_empty_topic' | true
        'empty'           | false
    }
}
