package com.rewe.digital.gui.controller.connections

import com.rewe.digital.AbstractKafkaSpec
import com.rewe.digital.kafka.topics.Topic
import com.rewe.digital.model.connection.ConnectionSettings
import groovy.util.logging.Slf4j
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit

@Slf4j
class OverallConnectionsControllerComponentSpec extends AbstractKafkaSpec {
    @Override
    String getSceneFileToTest() {
        return "scenes/connections/overall_connections.fxml"
    }

    def "Connect to kafka using minimal settings"() {
        given:
        def connectionSettings = new ConnectionSettings('first_cluster', firstKafkaContainer.getPlaintextBootstrapServers())
        connectionRepository.save(connectionSettings)

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        and:
        FxToolkit.setupStage({
            selectFirstConnection()
            clickOn('#checkButton')
        })

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#connectionCheckLabel', { Label connectionCheckLabel ->
                connectionCheckLabel.text == "success"
            })
        }
    }

    def "Predefined kafka settings are displayed correctly"() {
        given:
        def connectionSettings = new ConnectionSettings('first_cluster', 'localhost:9092')
        connectionRepository.save(connectionSettings)

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#configuredConnectionsList', { ListView li ->
                log.info("Number of connections ${li.items.size()}")
                li.items.size() == 1
            })
        }

        and:
        conditions.within(3) {
            FxAssert.verifyThat('#configuredConnectionsList', { ListView li ->
                log.info("Selected connection ${li.items.first()}")
                li.items.first() == connectionSettings
            })
        }

        when:
        FxToolkit.setupStage({
            selectFirstConnection()
        })

        then:
        FxAssert.verifyThat('#clusterNameField', { TextField it ->
            it.text == connectionSettings.name
        })

        and:
        FxAssert.verifyThat('#bootstrapServersField', { TextField it ->
            it.text == connectionSettings.bootstrapServer
        })
    }

    def "Connect to a cluster then try to reconnect"() {
        given:
        def firstClusterConnectionSettings = new ConnectionSettings('first_cluster', firstKafkaContainer.getPlaintextBootstrapServers())
        def secondClusterConnectionSettings = new ConnectionSettings('second_cluster', secondKafkaContainer.getPlaintextBootstrapServers())
        connectionRepository.save(firstClusterConnectionSettings)
        connectionRepository.save(secondClusterConnectionSettings)

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        and:
        FxToolkit.setupStage({
            selectConnection('first_cluster')
            clickOn('#connectButton')
        })

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#kafkaBrowserPane', { it.visible == true })
        }

        when:
        clickOn('#menuConnections')

        and:
        clickOn('#menuSwitchConnection')

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#overallConnectionsView', { it.visible == true })
        }
    }

    private void selectFirstConnection() {
        ListView connections = scene.lookup('#configuredConnectionsList')
        connections.selectionModel.selectFirst()
    }

    private void selectConnection(connectionName = 'first_cluster') {
        def scene = factory.previouslyCreatedStages["scenes/connections/overall_connections.fxml"].scene
        ListView connections = scene.lookup('#configuredConnectionsList')
        def wantedConnection = connections.items.find { settings -> settings.name == connectionName}
        connections.selectionModel.select(wantedConnection)
    }
}
