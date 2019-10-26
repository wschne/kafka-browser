package com.rewe.digital.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ShowMessageDetailsEvent<T> {
    private String topic;
    private T message;
}
