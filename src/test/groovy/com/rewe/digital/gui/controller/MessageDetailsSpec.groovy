package com.rewe.digital.gui.controller

import com.rewe.digital.AbstractApplicationSpec
import com.rewe.digital.kafka.KafkaQueryExecutor
import com.rewe.digital.messaging.events.ShowMessageDetailsEvent
import com.rewe.digital.messaging.events.querying.ShowQueryResultEvent
import org.apache.spark.sql.types.StructType
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit
import spock.lang.Unroll

import static com.rewe.digital.messaging.events.querying.ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW

class MessageDetailsSpec extends AbstractApplicationSpec {
    KafkaQueryExecutor kafkaQueryExecutor = Mock()

    @Override
    String getSceneFileToTest() {
        return "scenes/query/message_details.fxml"
    }

    @Unroll
    def "Show result of message (#messageToShow) as plain text"() {
        given:
        def topic = 'any_topic'

        when:
        eventBus.post(new ShowMessageDetailsEvent(topic, messageToShow))

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#messageViewAsText', {
                it.getText() == expectedText
            })
        }

        and:
        conditions.within(3) {
            FxAssert.verifyThat('#messageViewAsText', { it.isEditable() == false })
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

        and:
        kafkaQueryExecutor.getTopicSchema(topic) >> Optional.of(schema)

        and:
        FxToolkit.setupStage({
            loader.getController().kafkaQueryExecutor = kafkaQueryExecutor
            loader.getController().initialize(null, null)
        })


        when:
        eventBus.post(new ShowQueryResultEvent(CURRENT_WINDOW, topic, [[key: 'bla', value: 'blub']]))

        then:
        conditions.within(3) {
            FxAssert.verifyThat('#schemaText', { it.getText() == schemaString })
        }
    }
}
