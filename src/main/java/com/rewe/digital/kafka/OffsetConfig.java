package com.rewe.digital.kafka;

public enum OffsetConfig {
    LATEST("latest"),
    EARLIEST("earliest"),
    TIME_OFFSET("timeOffset");

    private final String kafkaOffsetType;

    OffsetConfig(String kafkaOffsetType) {
        this.kafkaOffsetType = kafkaOffsetType;
    }

    public String getKafkaOffsetType() {
        return kafkaOffsetType;
    }
}
