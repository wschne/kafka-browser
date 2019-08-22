package com.rewe.digital.gui.controller.connections

import com.rewe.digital.AbstractKafkaSpec
import com.rewe.digital.model.connection.BrokerSecuritySettings
import com.rewe.digital.model.connection.BrokerSecurityType
import com.rewe.digital.model.connection.ConnectionSettings
import javafx.scene.control.Label
import javafx.scene.control.ListView
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit
import spock.lang.Unroll

class SaslSslDetailsControllerSpec extends AbstractKafkaSpec {
    @Override
    String getSceneFileToTest() {
        return "scenes/connections/overall_connections.fxml"
    }

    @Unroll
    def "Connect to kafka using a secured ssl_sasl connection should be #expectedResult"() {
        given:
        def truststoreFilePath = getSecurityFile(trustStoreFileName)
        def securityConfig = new BrokerSecuritySettings(BrokerSecurityType.SASL_SSL,
                saslMechanism,
                truststoreFilePath,
                trustStorePassword,
                "",
                "",
                "",
                loginUser,
                loginPassword)
        def connectionSettings = new ConnectionSettings(UUID.randomUUID(),
                'sasl_ssl_config',
                kafkaContainer.getSaslSslBootstrapServers(),
                securityConfig)
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
                connectionCheckLabel.text == expectedResult
            })
        }

        where:
        expectedResult | loginUser    | loginPassword | saslMechanism | trustStoreFileName                   | trustStorePassword
        'failed'       | 'wrong_user' | 'nodesinek'   | 'PLAIN'       | 'docker.kafka.server.truststore.jks' | 'nodesinek'
        'failed'       | 'admin'      | 'wrong_pw'    | 'PLAIN'       | 'docker.kafka.server.truststore.jks' | 'nodesinek'
        'success'      | 'admin'      | 'nodesinek'   | 'PLAIN'       | 'docker.kafka.server.truststore.jks' | 'nodesinek'
    }

    private String getSecurityFile(String fileName) {
        this.getClass().getResource("/kafka_server_conf/certs/${fileName}").file
    }

    private void selectFirstConnection() {
        ListView connections = scene.lookup('#configuredConnectionsList')
        connections.selectionModel.selectFirst()
    }
}