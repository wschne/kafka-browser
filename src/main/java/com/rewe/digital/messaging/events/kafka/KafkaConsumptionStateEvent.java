package com.rewe.digital.messaging.events.kafka;

import com.rewe.digital.kafka.consumer.ConsumerRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class KafkaConsumptionStateEvent {
    private final String topicName;
    private final int totalWantedMessages;
    private final int totalConsumedMessages;
    private final boolean consumptionFinished;
    private List<ConsumerRecord> currentBatchOfMessages = new ArrayList<>();
}
