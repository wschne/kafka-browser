package com.rewe.digital.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class KafkaMessage {
    private String key;
    private int partition;
    private long offset;
    private long timestamp;
    private Object value;
}
