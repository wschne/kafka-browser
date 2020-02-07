package com.rewe.digital.kafka

import com.rewe.digital.kafka.consumer.ConsumerRecord
import com.rewe.digital.kafka.consumer.KafkaConsumptionState
import com.rewe.digital.kafka.consumer.KafkaConsumptionStateCallback
import groovy.json.JsonOutput
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.sql.SparkSession
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant

class KafkaToSparkConnectorSpec extends Specification {
    def conditions = new PollingConditions()

    @Shared
    def sparkSession = SparkSession
            .builder()
            .master("local[*]")
            .appName("Kafka-Browser-Test")
            .config("spark.driver.host", "localhost")
            .config("spark.driver.bindAddress", "127.0.0.1")
            .config("spark.sql.codegen.wholeStage", "false")
            .getOrCreate();

    def kafkaConsumer = Mock(KafkaConsumer)
    def consumerRecordTransformer = Mock(ConsumerRecordTransformer)

    def kafkaConnector = new KafkaToSparkConnector(sparkSession, kafkaConsumer, consumerRecordTransformer)

    def setup() {
        consumerRecordTransformer.toJson(_) >> ['msg']
    }

    def cleanupSpec() {
        sparkSession.stop()
    }

    def "Consume some messages and add them into spark-context"() {
        given:
        def topic = 'sample_topic'
        def offsetType = OffsetConfig.EARLIEST
        def totalMessagesWanted = 200
        def totalMessagesReceived = 150

        and:
        def consumedMessages = consumptionStateEvent(totalMessagesWanted, totalMessagesReceived)

        when:
        kafkaConnector.initKafkaConsumer(topic, offsetType, totalMessagesWanted, consumptionStateCallback)

        then:
        conditions.eventually {
            1 * kafkaConsumer.startConsumer(topic,
                    offsetType,
                    totalMessagesWanted,
                    { KafkaConsumptionStateCallback c ->
                        c.messagesReceived(consumedMessages); true })
        }

        and:
        conditions.within(10, {
            sparkSession.sql("select * from $topic").count() > 0
        })
    }

    def "Consume some messages and append them onto an existing dataset"() {
        given:
        def topic = 'existing_topic'
        def offsetType = OffsetConfig.EARLIEST
        def totalPreExistingMessages = 2000
        def totalMessagesWanted = 200
        def totalMessagesReceived = 150

        and:
        def event = consumptionStateEvent(totalMessagesWanted, totalMessagesReceived)

        and:
        prepareAnExistingDataFrame(topic, totalPreExistingMessages)

        and:
        consumerRecordTransformer.toJson(_) >> JsonOutput.toJson(event.currentBatchOfMessages)

        when:
        kafkaConnector.initKafkaConsumer(topic, offsetType, totalMessagesWanted, consumptionStateCallback)

        then:
        1 * kafkaConsumer.startConsumer(topic,
                offsetType,
                totalMessagesWanted,
                { KafkaConsumptionStateCallback c -> c.messagesReceived(event); true })

        and:
        conditions.within(1, {
            sparkSession.sql("select * from $topic").count() == totalMessagesReceived + totalPreExistingMessages
        })
    }

    private void prepareAnExistingDataFrame(String topic, int totalPreExistingMessages) {
        def existingMessages = generateJsonMessages(totalPreExistingMessages)
        final JavaSparkContext javaSparkContext = new JavaSparkContext(sparkSession.sparkContext());
        def messagesAsJson = javaSparkContext.parallelize(existingMessages);
        def dataFrame = sparkSession.read().json(messagesAsJson)
        dataFrame.createTempView(topic)
    }

    private List<String> generateJsonMessages(int totalMessages) {
        (1..totalMessages).collect {
            JsonOutput.toJson(sampleMessage(it))
        }
    }

    private KafkaConsumptionState consumptionStateEvent(int totalMessages, int consumedMessages) {
        def messages = (1..consumedMessages).collect {
            sampleMessage(it)
        }
        new KafkaConsumptionState(totalMessages, 1, messages)
    }

    private ConsumerRecord sampleMessage(index = 1) {
        def key = UUID.randomUUID()
        new ConsumerRecord(key as String,
                """{"index": $index, "a_property": "some value"}""" as String,
                Instant.now().toEpochMilli(),
                1,
                1,
                [:])
    }

    def consumptionStateCallback = new KafkaConsumptionStateCallback() {

        @Override
        void messagesReceived(KafkaConsumptionState consumptionState) {

        }

        @Override
        void consumptionFinished() {

        }

        @Override
        void consumptionAborted() {

        }
    }
}
