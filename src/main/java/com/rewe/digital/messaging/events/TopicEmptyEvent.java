package com.rewe.digital.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopicEmptyEvent {
    private String topicName;
}
