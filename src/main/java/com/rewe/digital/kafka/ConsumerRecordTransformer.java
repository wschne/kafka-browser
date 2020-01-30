package com.rewe.digital.kafka;

import com.rewe.digital.kafka.consumer.ConsumerRecord;
import lombok.val;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class ConsumerRecordTransformer {

    private ObjectMapper objectMapper = new ObjectMapper();

    public List<Map> toMap(final List<ConsumerRecord> records) {
        return getRecordsAsMap(records).collect(Collectors.toList());
    }

    public List<String> toJson(final List<ConsumerRecord> records) {
        return getRecordsAsMap(records)
                .map(message -> {
                    try {
                        return objectMapper.writeValueAsString(message);
                    } catch (IOException e) {
                        return "";
                    }
                })
                .collect(Collectors.toList());
    }

    private Stream<HashMap<String, Object>> getRecordsAsMap(List<ConsumerRecord> records) {
        return records.stream()
                .map(stringStringConsumerRecord -> {
                    val value = stringStringConsumerRecord.getValue();
                    val timestamp = stringStringConsumerRecord.getTimestamp();
                    val offset = stringStringConsumerRecord.getTimestamp();
                    val partition = stringStringConsumerRecord.getPartition();
                    val key = stringStringConsumerRecord.getKey().toString();

                    val messageAsMap = new HashMap<String, Object>();
                    messageAsMap.put("key", key);
                    messageAsMap.put("partition", partition);
                    messageAsMap.put("timestamp", timestamp);
                    messageAsMap.put("offset", offset);

                    val jsonAsObject = getAsJsonObject(value);
                    if (jsonAsObject.isPresent()) {
                        messageAsMap.put("value", jsonAsObject.get());
                    } else {
                        messageAsMap.put("value", value);
                    }
                    return messageAsMap;

                });
    }

    private Optional<Map> getAsJsonObject(final Object value) {
        if (value instanceof String) {
            try {
                return Optional.of(objectMapper.readValue(value.toString(), java.util.Map.class));
            } catch (IOException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

}
