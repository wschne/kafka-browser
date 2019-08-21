package com.rewe.digital.messaging.events.querying;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueryExecutionFinishedEvent {
    private String topicName;
}
