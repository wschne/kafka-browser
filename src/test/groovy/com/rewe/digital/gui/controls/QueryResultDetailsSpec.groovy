package com.rewe.digital.gui.controls

import com.rewe.digital.AbstractControlSpec
import com.rewe.digital.gui.StageFactory
import com.rewe.digital.gui.controls.helper.QueryResultTableColumnBuilder
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.testfx.api.FxToolkit
import org.testfx.service.adapter.impl.JavafxRobotAdapter
import spock.lang.Unroll

class QueryResultDetailsSpec extends AbstractControlSpec {
    JavafxRobotAdapter robotAdapter = new JavafxRobotAdapter()
    QueryResultDetails queryResultDetails

    Scene scene
    Stage stage

    @Override
    void start(Stage stage) {
        this.stage = stage;
        queryResultDetails = new QueryResultDetails(new QueryResultTableColumnBuilder(), new StageFactory(fxmlLoader: new FXMLLoader()), injector)
        queryResultDetails.setId('queryResultDetails')
        scene = new Scene(new StackPane(queryResultDetails), 100, 100)
        stage.setScene(scene)
        stage.show()

        robotAdapter.robotCreate(stage.getScene());
    }

    def "Show query result in a table pane"() {
        given:
        def entries = [[a: 'row_1_a',
                        b: 'row_1_b',
                        c: 'row_1_c'],
                       [a: 'row_2_a',
                        b: 'row_2_b',
                        c: 'row_2_c']]

        when:
        FxToolkit.setupScene({
            queryResultDetails.showSearchResult(entries)
        })

        then:
        pollingConditions.eventually {
            queryResultDetails.currentSearchResult.items == entries
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

        when:
        FxToolkit.setupScene({
            queryResultDetails.showSearchResult(entries)
        })

        and:
        queryResultDetails.filterSearchResultInput.clear()
        queryResultDetails.filterSearchResultInput.text = queryString

        then:
        pollingConditions.eventually {
            queryResultDetails.currentSearchResult.items.size() == foundItemsSize
        }

        where:
        queryString | foundItemsSize
        '1_a'       | 1
        'B'         | 2
        'row_'      | 2
        '2'         | 1
    }
}
