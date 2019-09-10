package com.rewe.digital.gui.controller

import com.rewe.digital.AbstractApplicationSpec
import com.rewe.digital.kafka.topics.Topic
import com.rewe.digital.kafka.topics.TopicsService
import com.rewe.digital.messaging.events.KafkaConnectionSelectedEvent
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit
import spock.lang.Unroll

class KafkaBrowserControllerSpec extends AbstractApplicationSpec {
    def topicsService = Mock(TopicsService)

    @Override
    String getSceneFileToTest() {
        return "scenes/kafka_browser.fxml"
    }

    @Unroll
    def "Filter topics list based on user input #searchInput"() {
        given:
        topicsService.topics() >> [new Topic('topic_a', [:]),
                                   new Topic('topic_b', [:]),
                                   new Topic('topic_ac', [:])]

        and:
        FxToolkit.setupStage({
            loader.getController().topicsService = topicsService
            loader.getController().initialize(null, null)
        })

        and:
        eventBus.post(new KafkaConnectionSelectedEvent())

        when:

        clickOn("#filterTopicsInput").eraseText(10). write(searchInput)

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#availableTopicsList', { it.items.size() == expectedEntries.size() })
        }

        where:
        searchInput | expectedEntries
        '_a'        | ['topic_a', 'topic_ac']
        '_ac'       | ['topic_ac']
        '_a_none'   | []
        ''          | ['topic_a', 'topic_b', 'topic_ac']
    }
}
