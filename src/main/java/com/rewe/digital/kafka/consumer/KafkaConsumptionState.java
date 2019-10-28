package com.rewe.digital.kafka.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class KafkaConsumptionState {
    private final int totalWantedMessages;
    private final int totalConsumedMessages;
    private List<ConsumerRecord> currentBatchOfMessages = new ArrayList<>();
}
