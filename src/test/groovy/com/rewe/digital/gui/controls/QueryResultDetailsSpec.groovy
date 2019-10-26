package com.rewe.digital.gui.controls

import com.google.inject.Injector
import com.rewe.digital.AbstractControlSpec
import com.rewe.digital.gui.StageFactory
import com.rewe.digital.gui.controls.helper.QueryResultTableColumnBuilder
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.testfx.api.FxToolkit
import org.testfx.service.adapter.impl.JavafxRobotAdapter
import spock.lang.Unroll

class QueryResultDetailsSpec extends AbstractControlSpec {
    JavafxRobotAdapter robotAdapter = new JavafxRobotAdapter()
    QueryResultDetails queryResultDetails

    Scene scene
    Stage stage

    Injector injector = Mock()

    @Override
    void start(Stage stage) {
        this.stage = stage;
        queryResultDetails = new QueryResultDetails(new QueryResultTableColumnBuilder(),
                new StageFactory(fxmlLoader: new FXMLLoader()),
                injector)
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
            queryResultDetails.showSearchResult(entries, 'topic')
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
            queryResultDetails.showSearchResult(entries, 'topic')
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

    def "Show message details only after a click on the table entry"() {
        given:
        def entries = [[a: 'row_1_a',
                        b: 'row_1_b',
                        c: 'row_1_c'],
                       [a: 'row_2_a',
                        b: 'row_2_b',
                        c: 'row_2_c']]
        and:
        MessageDetails messageDetails = Mock(MessageDetails)
        injector.getInstance(MessageDetails) >> messageDetails

        and:
        def vBox = Mock(VBox)
        def vBoxChildren = Mock(ObservableList)
        vBox.getChildren() >> vBoxChildren
        queryResultDetails.vBoxContainer = vBox

        when:
        FxToolkit.setupScene({
            queryResultDetails.showSearchResult(entries, 'topic')
        })

        then:
        pollingConditions.eventually {
            assert queryResultDetails.messageDetailsPane == null
            assert queryResultDetails.currentSearchResult.selectionModel.selectedItem == null
        }

        when:
        queryResultDetails.currentSearchResult.selectionModel.selectFirst()

        then:
        pollingConditions.eventually {
            1 * vBoxChildren.add(messageDetails)
            1 * messageDetails.showMessageDetails('topic', _)
        }
    }
}
