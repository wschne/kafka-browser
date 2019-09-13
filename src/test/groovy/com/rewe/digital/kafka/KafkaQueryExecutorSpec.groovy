package com.rewe.digital.kafka

import com.rewe.digital.model.Query
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.SparkSession
import spock.lang.Specification

class KafkaQueryExecutorSpec extends Specification {
    def spark = Mock(SparkSession)

    KafkaQueryExecutor kafkaQueryExecutor = new KafkaQueryExecutor(spark)

    def "Execute a query and map the result as json"() {
        given:
        def query = new Query('select * from bla')

        and:
        def rawData = Mock(Dataset)
        def jsonData = Mock(Dataset)
        rawData.toJSON() >> jsonData
        jsonData.collectAsList() >> ['{"abc":"def"}']

        when:
        def result = kafkaQueryExecutor.executeQuery(query)

        then:
        result.size() == 1
        result.first().abc == 'def'

        and:
        1 * spark.sql(query.query) >> rawData
    }

    def "Make sure to use a normalized topic name within the query"() {
        given:
        def query = new Query('select * from topic-name-one where value.id = \'some-identifier\'')

        and:
        def rawData = Mock(Dataset)
        def jsonData = Mock(Dataset)
        rawData.toJSON() >> jsonData
        jsonData.collectAsList() >> ['{"abc":"def"}']

        when:
        kafkaQueryExecutor.executeQuery(query)

        then:
        1 * spark.sql('select * from topic_name_one where value.id = \'some-identifier\'') >> rawData
    }
}
