package com.rewe.digital.gui.controls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.kafka.KafkaQueryExecutor;
import com.rewe.digital.messaging.events.ShowMessageDetailsEvent;
import com.rewe.digital.messaging.events.querying.ShowQueryResultEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Map;

@Named
public class MessageDetails extends AnchorPane {

    private TextArea messageViewAsText;
    private TreeView messageViewAsTree;
    private TextArea schemaText;

    private KafkaQueryExecutor kafkaQueryExecutor;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public MessageDetails(final KafkaQueryExecutor kafkaQueryExecutor,
                          final StageFactory stageFactory) {
        this.kafkaQueryExecutor = kafkaQueryExecutor;

        val pane = stageFactory.getParent("scenes/controls/message_details.fxml");
        this.getChildren().addAll(pane.getChildrenUnmodifiable());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        messageViewAsText = (TextArea) this.lookup("#messageViewAsText");
        schemaText = (TextArea) this.lookup("#schemaText");
        messageViewAsTree = (TreeView) this.lookup("#messageViewAsTree");
    }

    public void showMessageDetails(final String topic, final Object message) {
        Platform.runLater(
                () -> {
                    if (message instanceof Map) {
                        final Map messageAsMap = (Map) message;
                        messageViewAsTree.setRoot(createTree(messageAsMap));
                        messageViewAsText.setText(getMessageAsJsonString(messageAsMap));
                        messageViewAsTree.setShowRoot(false);

                        messageViewAsTree.setCellFactory(t -> {
                            TextFieldTreeCell<MapItem> cell = new TextFieldTreeCell<>();
                            cell.setConverter(new Converter(cell));
                            return cell;
                        });
                    } else if (message instanceof byte[]) {
                        val messageAsArray = (byte[]) message;
                        messageViewAsText.setText(Arrays.toString(messageAsArray));
                    } else if (message != null) {
                        messageViewAsText.setText(message.toString());
                    }

                    messageViewAsText.setEditable(false);
                }
        );
        showSchemaOfKnownTopics(topic);
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

        if (value != null) {
            if (value instanceof Map) {
                Map<String, Object> vMap = (Map<String, Object>) value;

                // recursive creation of subtrees for map entries
                for (Map.Entry<String, Object> e : vMap.entrySet()) {
                    result.getChildren().add(createTree(vMap, e));
                }
            } else {
                result.getChildren().add(new TreeItem<>(new MapItem(null, value.toString())));
            }
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

    private void showSchemaOfKnownTopics(final String topic) {
        schemaText.clear();
        val schema = kafkaQueryExecutor.getTopicSchema(topic);
        schema.ifPresent(structType -> schemaText.setText(structType.treeString()));
    }
}
