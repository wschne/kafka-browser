package com.rewe.digital.gui.controller.connections

import com.rewe.digital.AbstractKafkaSpec
import com.rewe.digital.model.connection.ConnectionSettings
import groovy.util.logging.Slf4j
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit

@Slf4j
class OverallConnectionsControllerSpec extends AbstractKafkaSpec {
    @Override
    String getSceneFileToTest() {
        return "scenes/connections/overall_connections.fxml"
    }

    def "Connect to kafka using minimal settings"() {
        given:
        def connectionSettings = new ConnectionSettings('a_simple_test_config', kafkaContainer.getPlaintextBootstrapServers())
        fileStorageRepository.writeDataToFile('connections', connectionSettings.fileName, connectionSettings)

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
        def connectionSettings = new ConnectionSettings('a_simple_test_config', 'localhost:9092')
        fileStorageRepository.writeDataToFile('connections', connectionSettings.fileName, connectionSettings)

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

    private void selectFirstConnection() {
        ListView connections = scene.lookup('#configuredConnectionsList')
        connections.selectionModel.selectFirst()
    }
}
