package com.rewe.digital.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rewe.digital.model.Query;
import org.apache.commons.lang.StringUtils;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Named
public class KafkaQueryExecutor {

    @Inject
    SparkSession spark;

    @Inject
    KafkaConnector kafkaConnector;

    public List<Map> executeQuery(Query query) throws AnalysisException {
        return spark.sql(query.getQuery())
                .toJSON()
                .collectAsList()
                .stream()
                .map(s -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        final JsonNode jsonNode = mapper.readTree(s);
                        return mapper.convertValue(jsonNode, Map.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    } catch (IOException e) {
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    public Optional<StructType> getTopicSchema(String topic) {
        Optional<Dataset<Row>> table = getTable(topic);
        return table.map(Dataset::schema);
    }

    public TopicTableStatus getTopicTableStatus(String topicName) {
        final Optional<Dataset<Row>> table = getTable(topicName);
        return new TopicTableStatus(table.map(Dataset::count).orElse(0l));
    }

    private String getTopicUnparsedViewName(String topic) {
        return StringUtils.replace(topic, "-", "_");
    }

    private Optional<Dataset<Row>> getTable(String tableName) {
        final String topicUnparsedViewName = getTopicUnparsedViewName(tableName);
        if (Arrays.asList(spark.sqlContext().tableNames()).contains(topicUnparsedViewName))
            return Optional.of(spark.sqlContext().table(topicUnparsedViewName));
        else
            return Optional.empty();
    }

    public static class TopicTableStatus {
        long recordCountInStore;

        public TopicTableStatus(long recordCountInStore) {
            this.recordCountInStore = recordCountInStore;
        }

        public long getRecordCountInStore() {
            return recordCountInStore;
        }
    }
}
