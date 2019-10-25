package com.rewe.digital.gui.controls


import com.rewe.digital.AbstractControlSpec
import com.rewe.digital.gui.StageFactory
import com.rewe.digital.kafka.KafkaQueryExecutor
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.apache.spark.sql.types.StructType
import org.testfx.service.adapter.impl.JavafxRobotAdapter
import spock.lang.Unroll

class MessageDetailsSpec extends AbstractControlSpec {
    JavafxRobotAdapter robotAdapter = new JavafxRobotAdapter()
    KafkaQueryExecutor kafkaQueryExecutor = Mock(){
        getTopicSchema(_) >> Optional.empty()
    }

    MessageDetails messageDetails

    Scene scene
    Stage stage

    @Override
    void start(Stage stage) {
        this.stage = stage;
        messageDetails = new MessageDetails(kafkaQueryExecutor,
                new StageFactory(fxmlLoader: new FXMLLoader()))
        messageDetails.setId('messageDetails')
        scene = new Scene(new StackPane(messageDetails), 100, 100)
        stage.setScene(scene)
        stage.show()

        robotAdapter.robotCreate(stage.getScene());
    }

    @Unroll
    def "Show result of message (#messageToShow) as plain text"() {
        given:
        def topic = 'any_topic'

        when:
        messageDetails.showMessageDetails(topic, messageToShow)

        then:
        pollingConditions.within(3) {
            messageDetails.messageViewAsText.text == expectedText
        }

        and:
        pollingConditions.within(3) {
            messageDetails.messageViewAsText.isEditable() == false
        }

        where:
        messageToShow               | expectedText
        [key: 'bla', value: 'blub'] | '{\n  "key" : "bla",\n  "value" : "blub"\n}'
        'a non json string'         | 'a non json string'
        [12, 13, 44] as byte[]      | '[12, 13, 44]'
    }

    def "Show schema of related topic"() {
        given:
        def topic = 'any_topic'
        def schemaString = 'schema-string'
        def schema = Mock(StructType) {
            treeString() >> schemaString
        }

        when:
        messageDetails.showMessageDetails(topic, [[key: 'bla', value: 'blub']])

        then:
        pollingConditions.within(3) {
            messageDetails.schemaText.text == schemaString
            messageDetails.messageViewAsText.isEditable() == false
        }

        and:
        1 * kafkaQueryExecutor.getTopicSchema(topic) >> Optional.of(schema)
    }
}
