package com.rewe.digital.kafka;

public enum OffsetConfigType {
    LATEST("latest"),
    EARLIEST("earliest"),
    TIME_OFFSET("timeOffset");

    private final String kafkaOffsetType;

    OffsetConfigType(String kafkaOffsetType) {
        this.kafkaOffsetType = kafkaOffsetType;
    }

    public String getKafkaOffsetType() {
        return kafkaOffsetType;
    }
}
