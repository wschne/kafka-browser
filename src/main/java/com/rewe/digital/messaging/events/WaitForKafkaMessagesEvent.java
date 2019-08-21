package com.rewe.digital.messaging.events;

import com.rewe.digital.model.Query;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WaitForKafkaMessagesEvent {
    private Query query;
}
