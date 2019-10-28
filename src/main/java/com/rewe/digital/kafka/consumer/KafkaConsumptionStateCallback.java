package com.rewe.digital.kafka.consumer;

public abstract class KafkaConsumptionStateCallback {
    public abstract void messagesReceived(final KafkaConsumptionState consumptionState);
    public abstract void consumptionFinished();
    public abstract void consumptionAborted();
}
