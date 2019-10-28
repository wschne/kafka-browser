package com.rewe.digital.kafka.topics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class Topic {
    private final String id;
    private final Map<Integer, Long> partitionToOffset;
    private boolean isCurrentlyConsuming;
}
