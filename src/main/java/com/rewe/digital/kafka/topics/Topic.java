package com.rewe.digital.kafka.topics;

import java.util.Map;

public class Topic {

    private final String id;
    private final Map<Integer, Long> partitionToOffset;
    private boolean isCurrentlyConsuming;

    public Topic(String name, Map<Integer, Long> partitionToOffset) {
        this.id = name;
        this.partitionToOffset = partitionToOffset;
    }

    public String getId() {
        return id;
    }

    public Map<Integer, Long> getPartitionToOffset() {
        return partitionToOffset;
    }

    public void setCurrentlyConsuming(boolean currentlyConsuming) {
        isCurrentlyConsuming = currentlyConsuming;
    }

    public boolean isCurrentlyConsuming() {
        return isCurrentlyConsuming;
    }
}
