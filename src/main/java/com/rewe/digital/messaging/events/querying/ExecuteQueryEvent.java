package com.rewe.digital.messaging.events.querying;

import com.rewe.digital.model.Query;
import lombok.Data;

@Data
public class ExecuteQueryEvent {
    private final Query query;
    private final ResultTarget target;

    public enum ResultTarget {
        CURRENT_WINDOW,
        NEW_WINDOW
    }
}
