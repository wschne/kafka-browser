package com.rewe.digital.gui.controller.querying

import com.rewe.digital.AbstractApplicationSpec
import com.rewe.digital.messaging.events.querying.ShowQueryResultEvent
import javafx.scene.control.TableView
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit
import spock.lang.Unroll

import static com.rewe.digital.messaging.events.querying.ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW

class QueryResultControllerSpec extends AbstractApplicationSpec {
    @Override
    String getSceneFileToTest() {
        return "scenes/query/query_result.fxml"
    }

    def "Show query result in a table pane"() {
        given:
        def entries = [[a: 'row_1_a',
                        b: 'row_1_b',
                        c: 'row_1_c'],
                       [a: 'row_2_a',
                        b: 'row_2_b',
                        c: 'row_2_c']]
        def queryResult = new ShowQueryResultEvent(CURRENT_WINDOW, 'my_topic', entries)

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        and:
        eventBus.post(queryResult)

        then:
        conditions.eventually {
            FxAssert.verifyThat('#currentSearchResult', { TableView resultTable ->
                resultTable.items == entries
            })
        }
    }

    @Unroll
    def "Search in table using a simple string #queryString match"() {
        given:
        def entries = [[a: 'row_1_a',
                        b: 'row_1_b',
                        c: 'row_1_c'],
                       [a: 'row_2_a',
                        b: 'row_2_b',
                        c: 'row_2_c']]
        def queryResult = new ShowQueryResultEvent(CURRENT_WINDOW, 'my_topic', entries)

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        and:
        eventBus.post(queryResult)

        and:
        lookup("#filterSearchResultInput").queryTextInputControl().clear()
        clickOn("#filterSearchResultInput").write(queryString);

        then:
        conditions.eventually {
            FxAssert.verifyThat('#currentSearchResult', { TableView resultTable ->
                resultTable.items.size() == foundItemsSize
            })
        }

        where:
        queryString | foundItemsSize
        '1_a'       | 1
        'B'         | 2
        'row_'      | 2
        '2'         | 1
    }


}
