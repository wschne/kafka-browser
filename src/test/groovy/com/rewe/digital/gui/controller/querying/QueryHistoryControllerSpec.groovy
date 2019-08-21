package com.rewe.digital.gui.controller.querying

import com.rewe.digital.AbstractApplicationSpec
import com.rewe.digital.configuration.FileStorageRepository
import com.rewe.digital.messaging.events.querying.ExecuteQueryEvent
import com.rewe.digital.model.Query
import javafx.scene.control.ListView
import org.testfx.api.FxAssert
import org.testfx.api.FxToolkit
import spock.lang.Unroll

class QueryHistoryControllerSpec extends AbstractApplicationSpec {
    FileStorageRepository fileStorageRepository = context.getInstance(FileStorageRepository)

    @Override
    String getSceneFileToTest() {
        return "scenes/query/query_history.fxml"
    }

    def setup() {
        FxToolkit.setupStage({
            loader.getController().historyList.getItems().clear()
        })
    }

    @Unroll
    def "Show query history in list"() {
        given:
        fileStorageRepository.writeDataToFile("query_history.json", queryList)

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        then:
        FxAssert.verifyThat('#historyList', { ListView it ->
            it.items.size() > queryList.size()
            it.items == queryList
        })

        where:
        queryList        | _
        []               | _
        ['abcdefg']      | _
        ['abcd', 'efgh'] | _
    }

    def "Queries file does not exist"() {
        given:
        fileStorageRepository.deleteFile('.', "query_history.json")

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        then:
        FxAssert.verifyThat('#historyList', { ListView it ->
            it.items.size() == 0
        })
    }

    def "User enters a new query"() {
        given:
        def newQuery = 'select * from bla'
        FxToolkit.setupStage({
            eventBus.post(new ExecuteQueryEvent(new Query(newQuery), ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW))
        })

        when:
        FxToolkit.setupStage({
            loader.getController().initialize(null, null)
        })

        then:
        FxAssert.verifyThat('#historyList', { ListView it ->
            it.items.contains(newQuery)
        })
    }
}
