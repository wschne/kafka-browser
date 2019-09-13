package com.rewe.digital.gui.controls.helper


import javafx.scene.control.TableColumn
import spock.lang.Specification
import spock.lang.Unroll

class QueryResultTableColumnBuilderSpec extends Specification {
    def queryResultTableColumnBuilder = new QueryResultTableColumnBuilder()

    @Unroll
    def "Build a table column for a given name #columnName and value: #columnValue"() {
        given:
        def columnData = new HashMap()
        columnData.put(columnName, columnValue)

        and:
        def cellData = new TableColumn.CellDataFeatures(null, null, columnData)

        when:
        def result = queryResultTableColumnBuilder.buildTableColumn(columnName)

        then:
        result

        when:
        def transformedCellProperty = result.cellValueFactory.call(cellData)

        then:
        transformedCellProperty.value == expectedResult

        where:
        columnName | columnValue          | expectedResult
        'col_name' | 'someValue'          | 'someValue'
        'col_name' | [abc: 'def', gh: 12] | '{"abc":"def","gh":12}'
        'col_name' | 1567937437L          | '19.01.1970 03:32:17'
        'col_name' | ['a', 'b', 3]        | '[a, b, 3]'
    }
}
