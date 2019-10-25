package com.rewe.digital.gui.controls.helper.autocomplete


import org.apache.spark.sql.types.Metadata
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StructType
import spock.lang.Specification
import spock.lang.Unroll

class SparkSchemaTraverseUtilSpec extends Specification {
    def sparkSchemaTraverseUtil = new SparkSchemaTraverseUtil()

    @Unroll
    def "Get #expectedChildren children behind #selectedColumns"() {
        given:
        def thirdLevelFields = [new StructField('col_3_col_1', new StringType(), false, Metadata.empty()),
                                new StructField('col_3_col_2', new StringType(), false, Metadata.empty())] as StructField[]
        def secondLevelFields = [new StructField('col_2_col_1', new StringType(), false, Metadata.empty()),
                                 new StructField('col_2_col_2', new StructType(thirdLevelFields), false, Metadata.empty())] as StructField[]
        def firstLevelFields = [new StructField('col_1', new StringType(), false, Metadata.empty()),
                                new StructField('col_2', new StructType(secondLevelFields), false, Metadata.empty())] as StructField[]
        def rootFields = new StructType(firstLevelFields)

        when:
        def result = sparkSchemaTraverseUtil.getChildrenOfField(selectedColumns as List<String>, rootFields)

        then:
        result.collect { it.name } == expectedChildren

        where:
        selectedColumns                         | expectedChildren
        []                                      | ['col_1', 'col_2']
        ['co']                                  | ['col_1', 'col_2']
        ['col_2']                               | ['col_2_col_1', 'col_2_col_2']
        ['col_2', 'col_2_']                     | ['col_2_col_1', 'col_2_col_2']
        ['col_2', 'col_2_col_2']                | ['col_3_col_1', 'col_3_col_2']
        ['col_2', 'col_2_col_2', 'col_3_col_1'] | []
    }
}
