package com.rewe.digital.gui.controls

import com.rewe.digital.AbstractControlSpec
import com.rewe.digital.gui.controls.helper.autocomplete.AutocompletePopUp
import com.rewe.digital.gui.controls.helper.autocomplete.SparkSchemaTraverseUtil
import com.rewe.digital.gui.controls.helper.autocomplete.SqlQueryAnalyzer
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.Metadata
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StructType
import org.testfx.api.FxToolkit
import org.testfx.service.adapter.impl.JavafxRobotAdapter
import org.testfx.util.WaitForAsyncUtils
import spock.lang.Unroll

class AutoCompleteTextFieldSpec extends AbstractControlSpec {
    JavafxRobotAdapter robotAdapter = new JavafxRobotAdapter()
    SparkSchemaTraverseUtil sparkSchemaTraverseUtil = Mock()
    AutocompletePopUp autocompletePopUp = Mock()
    SqlQueryAnalyzer sqlQueryAnalyzer = Mock()

    AutoCompleteTextField autoCompleteTextField

    @Override
    void start(Stage stage) {
        autoCompleteTextField = new AutoCompleteTextField(sparkSession: setupSpark(),
                sparkSchemaTraverseUtil: sparkSchemaTraverseUtil,
                autocompletePopUp: autocompletePopUp,
                sqlQueryAnalyzer: sqlQueryAnalyzer)
        autoCompleteTextField.setId('autoCompleteTextField')
        stage.setScene(new Scene(new StackPane(autoCompleteTextField), 100, 100))
        stage.show()

        robotAdapter.robotCreate(stage.getScene());
    }

    def setup() {
        sparkSchemaTraverseUtil.getChildrenOfField(_, _) >> []
        sqlQueryAnalyzer.getWordAtPosition(_, _) >> []
    }

    @Unroll
    def "Apply selected field #selectedEntry to the previously entered query '#initialQuery'"() {
        given:
        FxToolkit.setupScene({
            autoCompleteTextField.setText(initialQuery)
            autoCompleteTextField.positionCaret(caretPosition)
        })

        when:
        autoCompleteTextField.newEntrySelected().accept(selectedEntry)

        then:
        autoCompleteTextField.getText() == expectedResult

        and:
        _ * sqlQueryAnalyzer.isLastTypedCharacterADot(initialQuery, caretPosition) >> isCaretAtDot
        _ * sqlQueryAnalyzer.getWordAtPosition(initialQuery, caretPosition) >> selectedWords

        where:
        initialQuery                           | selectedWords               | selectedEntry | caretPosition | isCaretAtDot | expectedResult
        'select value. from topic'             | ['value']                   | 'payload'     | 13            | true         | 'select value.payload from topic'
        'select val from topic'                | ['val']                     | 'value'       | 9             | false        | 'select value from topic'
        'select  from topic'                   | []                          | 'value'       | 7             | false        | 'select value from topic'
        'select value.payload.tim from topic'  | ['value', 'payload', 'tim'] | 'timestamp'   | 25            | false        | 'select value.payload.timestamp from topic'
        'select value from topic where value.' | ['value']                   | 'payload'     | 36            | true         | 'select value from topic where value.payload'
    }

    def "Show popup of schema entries if user presses 'control-space'"() {
        given:
        clickOn('#autoCompleteTextField')

        and:
        FxToolkit.setupScene({
            autoCompleteTextField.setText('select  from topic')
            autoCompleteTextField.positionCaret(8)
        })

        when:
        robotAdapter.keyPress(KeyCode.CONTROL)
        robotAdapter.keyPress(KeyCode.SPACE)

        and:
        WaitForAsyncUtils.waitForFxEvents();

        then:
        pollingConditions.eventually {
            1 * autocompletePopUp.showEntriesPopUp(autoCompleteTextField, { List<StructField> fields ->
                assert fields[0].name() == 'col_1'
                assert fields[1].name() == 'col_2'
            })
        }

        and:
        1 * sparkSchemaTraverseUtil.getChildrenOfField(_, _) >> Arrays.asList(getTableSchema().fields())
        1 * sqlQueryAnalyzer.isLastTypedCharacterADot(_, _) >> false
        1 * sqlQueryAnalyzer.getWordAtPosition(_, _) >> []
    }

    def setupSpark() {
        SparkSession sparkSession = Mock()
        SQLContext sqlContext = Mock()
        Dataset<Row> table = Mock()

        sparkSession.sqlContext() >> sqlContext
        sqlContext.table(_) >> table
        table.schema() >> getTableSchema()
        sparkSession
    }

    def getTableSchema() {
        def thirdLevelFields = [new StructField('col_3_col_1', new StringType(), false, Metadata.empty()),
                                new StructField('col_3_col_2', new StringType(), false, Metadata.empty())] as StructField[]
        def secondLevelFields = [new StructField('col_2_col_1', new StringType(), false, Metadata.empty()),
                                 new StructField('col_2_col_2', new StructType(thirdLevelFields), false, Metadata.empty())] as StructField[]
        def firstLevelFields = [new StructField('col_1', new StringType(), false, Metadata.empty()),
                                new StructField('col_2', new StructType(secondLevelFields), false, Metadata.empty())] as StructField[]
        new StructType(firstLevelFields)
    }
}
