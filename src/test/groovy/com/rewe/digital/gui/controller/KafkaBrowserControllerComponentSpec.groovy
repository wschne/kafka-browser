package com.rewe.digital.gui.controller

import com.rewe.digital.AbstractApplicationSpec
import com.rewe.digital.kafka.topics.Topic
import com.rewe.digital.kafka.topics.TopicsService
import com.rewe.digital.messaging.events.kafka.KafkaConnectionSelectedEvent
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit

class KafkaBrowserControllerComponentSpec extends AbstractApplicationSpec {
    @Override
    String getSceneFileToTest() {
        return "scenes/kafka_browser.fxml"
    }

    def "Connect to a cluster and show the list of topics"() {
        given:
        def topicsService = Mock(TopicsService)
        def firstTopicList = [new Topic('a', [:]),new Topic('b', [:]),new Topic('c', [:])]
        def secondTopicList = [new Topic('d', [:]), new Topic('e', [:]), new Topic('f', [:]), new Topic('g', [:])]

        and:
        topicsService.topics() >>> [firstTopicList, secondTopicList]

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
            loader.getController().topicsService = topicsService
        })

        and:
        eventBus.post(new KafkaConnectionSelectedEvent())

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#availableTopicsList', { it.visible == true })
            FxAssert.verifyThat('#availableTopicsList', { it.items.size() == firstTopicList.size()})
        }

        when:
        eventBus.post(new KafkaConnectionSelectedEvent())

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#availableTopicsList', { it.items.size() == secondTopicList.size()})
        }
    }
}
