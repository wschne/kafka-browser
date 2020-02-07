package com.rewe.digital.gui.controls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.gui.controls.helper.QueryResultTableColumnBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Named
public class QueryResultDetails extends AnchorPane {
    private MessageDetails messageDetailsPane;

    ObjectMapper objectMapper = new ObjectMapper();

    private TableView currentSearchResult;
    private TextField filterSearchResultInput;

    private final QueryResultTableColumnBuilder tableColumnBuilder;
    private final Injector injector;
    private VBox vBoxContainer;

    @Inject
    public QueryResultDetails(final QueryResultTableColumnBuilder tableColumnBuilder,
                              final StageFactory stageFactory,
                              final Injector injector) {
        this.tableColumnBuilder = tableColumnBuilder;
        this.injector = injector;

        val pane = stageFactory.getParent("scenes/controls/query_result_details.fxml");
        this.getChildren().addAll(pane.getChildrenUnmodifiable());

        currentSearchResult = (TableView) this.lookup("#currentSearchResult");
        currentSearchResult.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filterSearchResultInput = (TextField) this.lookup("#filterSearchResultInput");
    }

    public void showSearchResult(final List<Map> result,
                                 final String topic,
                                 final boolean append) {

        val observableResult = FXCollections.observableArrayList(result);
        if (append) {
            final Stream stream = currentSearchResult.getItems().stream();
            final Stream<Map> stream1 = stream.map(Map.class::cast);
            List<Map> collect = stream1.collect(Collectors.toList());
            observableResult.addAll(collect);
        }

        addTableColumns(currentSearchResult, result);

        currentSearchResult.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedMessage) -> {
            getMessageDetails().showMessageDetails(topic, selectedMessage);
        });

        val filteredData = new FilteredList<>(observableResult, p -> true);

        filterSearchResultInput.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(row -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                val lowerCaseFilter = newValue.toLowerCase();

                return row.values()
                        .stream()
                        .map(o -> {
                            try {
                                return objectMapper.writeValueAsString(o);
                            } catch (JsonProcessingException e) {
                                return String.valueOf(0);
                            }
                        })
                        .anyMatch(i -> StringUtils.containsIgnoreCase((String) i, lowerCaseFilter));
            });
        });

        val sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(currentSearchResult.comparatorProperty());
        currentSearchResult.setItems(sortedData);
    }

    public void resetView() {
        val emptyList = FXCollections.observableArrayList();
        currentSearchResult.setItems(emptyList);

        filterSearchResultInput.clear();
        getVboxContainer().getChildren().remove(messageDetailsPane);
        messageDetailsPane = null;
    }

    private void addTableColumns(final TableView tableToAddColumnsto, final List<Map> tableData) {
        Optional<Set<String>> columns = tableData.stream()
                .findFirst()
                .map(Map::keySet);

        columns.ifPresent(cols -> {
            List<TableColumn<Map<String, Object>, String>> tableColumns = cols.stream()
                    .map(tableColumnBuilder::buildTableColumn)
                    .collect(Collectors.toList());

            Platform.runLater(
                    () -> {
                        tableToAddColumnsto.getColumns().clear();
                        tableToAddColumnsto.getColumns().addAll(tableColumns);
                    }
            );
        });
    }

    private MessageDetails getMessageDetails() {
        if (this.messageDetailsPane == null) {
            this.messageDetailsPane = injector.getInstance(MessageDetails.class);

            getVboxContainer().getChildren().add(messageDetailsPane);
        }

        return this.messageDetailsPane;
    }

    private VBox getVboxContainer() {
        if (this.vBoxContainer == null) {
            this.vBoxContainer = (VBox) lookup("VBox");
        }

        return vBoxContainer;
    }
}
