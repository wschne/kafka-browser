package com.rewe.digital.messaging.events.querying;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ShowQueryResultEvent {
    private final ExecuteQueryEvent.ResultTarget target;
    private final String topicName;
    private final List<Map> result;
}
