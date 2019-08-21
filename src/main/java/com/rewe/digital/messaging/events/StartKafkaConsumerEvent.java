package com.rewe.digital.messaging.events;

import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.kafka.OffsetConfigType;
import javafx.event.Event;
import javafx.event.EventType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.temporal.TemporalUnit;

@Data
@EqualsAndHashCode(callSuper = true)
public class StartKafkaConsumerEvent extends Event {
    private final String topicName;
    private final OffsetConfigType topicOffset;
    private final Integer numberOfMessages;
    private final Integer timeUntilNow;
    private final TemporalUnit timeUnit;
    private final TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent;

    public StartKafkaConsumerEvent(EventType<? extends Event> eventType,
                                   final String topicName,
                                   final OffsetConfigType topicOffset,
                                   final int numberOfMessages,
                                   final TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent) {
        super(eventType);
        this.topicName = topicName;
        this.topicOffset = topicOffset;
        this.numberOfMessages = numberOfMessages;
        this.topicListItemClickedEvent = topicListItemClickedEvent;
        this.timeUnit = null;
        this.timeUntilNow = null;
    }

    public StartKafkaConsumerEvent(EventType<? extends Event> eventType,
                                   final String topicName,
                                   final Integer timeUntilNow,
                                   final TemporalUnit timeUnit,
                                   final TopicListItem.TopicListItemClickedEvent topicListItemClickedEvent) {
        super(eventType);
        this.topicName = topicName;
        this.timeUntilNow = timeUntilNow;
        this.timeUnit = timeUnit;
        this.topicOffset = null;
        this.numberOfMessages = null;
        this.topicListItemClickedEvent = topicListItemClickedEvent;
    }
}
