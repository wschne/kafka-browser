package com.rewe.digital.messaging.events.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class KafkaConsumptionStateEvent {
    private final int totalWantedMessages;
    private final int totalConsumedMessages;
    private final boolean consumptionFinished;
    private List<ConsumerRecord> currentBatchOfMessages = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class ConsumerRecord<K,V> {
        private final K key;
        private final K value;
        private Map<String, String> header;
    }
}
