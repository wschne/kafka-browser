package com.rewe.digital.gui.controls.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Named;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

@Named
@Slf4j
public class QueryResultTableColumnBuilder {
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public QueryResultTableColumnBuilder() {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public TableColumn<Map<String, Object>, String> buildTableColumn(String column) {
        val mapper = new ObjectMapper();
        val tableColumn = new TableColumn<Map<String, Object>, String>(column);

        tableColumn.setCellValueFactory(p -> {
            final Object columnValue = p.getValue().get(column);
            if (columnValue instanceof String) {
                return new SimpleStringProperty((String) columnValue);
            } else if (columnValue instanceof Map) {
                try {
                    String json = mapper.writeValueAsString(columnValue);
                    int maxSize = 120;
                    if (json.length() > maxSize) {
                        json = json.substring(0, maxSize) + " ...";
                    }
                    return new SimpleStringProperty(json);
                } catch (JsonProcessingException e) {
                    log.error("Error while parsing json string", e);
                    return new SimpleStringProperty("Error while reading value");
                }
            } else if (columnValue instanceof Long) {
                val time = new Timestamp((Long)columnValue);
                return new SimpleStringProperty(formatter.format(time));
            } else {
                return new SimpleStringProperty(String.valueOf(columnValue));
            }
        });
        return tableColumn;
    }
}
