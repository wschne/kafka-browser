package com.rewe.digital.gui.controller

import com.rewe.digital.AbstractKafkaSpec
import com.rewe.digital.kafka.topics.Topic
import com.rewe.digital.kafka.topics.TopicsService
import com.rewe.digital.messaging.events.KafkaConnectionSelectedEvent
import com.rewe.digital.model.connection.ConnectionSettings
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit

class KafkaBrowserControllerComponentSpec extends AbstractKafkaSpec {
    @Override
    String getSceneFileToTest() {
        return "scenes/kafka_browser.fxml"
    }

    def "Connect to a cluster and show the list of topics"() {
        given:
        def firstClusterConnectionSettings = new ConnectionSettings('first_cluster', firstKafkaContainer.getPlaintextBootstrapServers())
        def secondClusterConnectionSettings = new ConnectionSettings('second_cluster', secondKafkaContainer.getPlaintextBootstrapServers())
        connectionRepository.save(firstClusterConnectionSettings)
        connectionRepository.save(secondClusterConnectionSettings)

        and:
        connectionRepository.setCurrentConnectionSettings(firstClusterConnectionSettings)

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        and:
        eventBus.post(new KafkaConnectionSelectedEvent())

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#availableTopicsList', { it.visible == true })
            FxAssert.verifyThat('#availableTopicsList', { it.items.size() == 1})
        }

        and:
        conditions.within(30) {
            secondKafkaContainer.consumer.listTopics().size() == 3
        }

        when:
        connectionRepository.setCurrentConnectionSettings(secondClusterConnectionSettings)

        and:
        FxToolkit.setupStage({
            eventBus.post(new KafkaConnectionSelectedEvent())
        })

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#availableTopicsList', { it.items.size() == 3})
        }
    }
}
