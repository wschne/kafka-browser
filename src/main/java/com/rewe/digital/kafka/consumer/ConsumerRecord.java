package com.rewe.digital.kafka.consumer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ConsumerRecord<K,V> {
    private final K key;
    private final V value;
    private Map<String, byte[]> header;
}
