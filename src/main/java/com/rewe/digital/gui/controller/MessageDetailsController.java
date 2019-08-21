package com.rewe.digital.gui.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.messaging.events.ShowMessageDetailsEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.util.StringConverter;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

@Named
public class MessageDetailsController implements Initializable {

    @FXML
    TextArea messageViewAsText;
    @FXML
    TreeView messageViewAsTree;

    @Inject
    private EventBus eventBus;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        eventBus.register(this);
    }

    @Subscribe
    private void showMessageDetails(ShowMessageDetailsEvent messageDetailsEvent) {
        Platform.runLater(
                () -> {
                    if (messageDetailsEvent.getMessage() instanceof Map) {
                        final Map messageAsMap = (Map) messageDetailsEvent.getMessage();
                        if (messageAsMap.containsKey("value") &&
                                messageAsMap.containsKey("key") &&
                                messageAsMap.containsKey("offset") &&
                                messageAsMap.containsKey("timestamp") &&
                                messageAsMap.get("value") instanceof Map) {
                            Map value = (Map) messageAsMap.get("value");
                            messageViewAsTree.setRoot(createTree(value));
                            messageViewAsText.setText(getMessageAsJsonString(value));
                        } else {
                            messageViewAsTree.setRoot(createTree(messageAsMap));
                            messageViewAsText.setText(getMessageAsJsonString(messageAsMap));
                        }

                        messageViewAsTree.setEditable(false);
                        messageViewAsTree.setShowRoot(false);

                        messageViewAsTree.setCellFactory(t -> {
                            TextFieldTreeCell<MapItem> cell = new TextFieldTreeCell<>();
                            cell.setConverter(new Converter(cell));
                            return cell;
                        });
                    } else {
                        messageViewAsText.setText(messageDetailsEvent.getMessage().toString());
                    }
                }
        );
    }

    private String getMessageAsJsonString(Map messageAsMap) {
        try {
            return objectMapper.writeValueAsString(messageAsMap);
        } catch (JsonProcessingException e) {
            return "";
        }
    }


    private static TreeItem<MapItem> createTree(Map<String, Object> map) {
        TreeItem<MapItem> result = new TreeItem<>();
        result.setExpanded(true);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.getChildren().add(createTree(map, entry));
        }

        return result;
    }

    private static TreeItem<MapItem> createTree(Map<String, Object> map, Map.Entry<String, Object> entry) {
        MapItem mi = new MapItem(map, entry.getKey());
        TreeItem<MapItem> result = new TreeItem<>(mi);

        Object value = entry.getValue();

        if (value instanceof Map) {
            Map<String, Object> vMap = (Map<String, Object>) value;

            // recursive creation of subtrees for map entries
            for (Map.Entry<String, Object> e : vMap.entrySet()) {
                result.getChildren().add(createTree(vMap, e));
            }
        } else {
            result.getChildren().add(new TreeItem<>(new MapItem(null, value.toString())));
        }

        return result;
    }

    private static class MapItem {

        private final Map<String, Object> map;
        private final String value;

        public MapItem(Map<String, Object> map, String value) {
            this.map = map;
            this.value = value;
        }
    }

    private static class Converter extends StringConverter<MapItem> {

        private final TreeCell<MapItem> cell;

        public Converter(TreeCell<MapItem> cell) {
            this.cell = cell;
        }

        @Override
        public String toString(MapItem object) {
            return object == null ? null : object.value;
        }

        @Override
        public MapItem fromString(String string) {
            MapItem mi = cell.getItem();

            if (mi != null) {
                TreeItem<MapItem> item = cell.getTreeItem();
                if (item.isLeaf()) {
                    MapItem parentItem = item.getParent().getValue();

                    // modify value in parent map
                    parentItem.map.put(parentItem.value, string);
                    mi = new MapItem(mi.map, string);
                } else if (!mi.map.containsKey(string)) {
                    // change key of mapping, if there is no mapping for the new key
                    mi.map.put(string, mi.map.remove(mi.value));
                    mi = new MapItem(mi.map, string);
                }
            }

            return mi;
        }

    }
}
