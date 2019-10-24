package com.rewe.digital.gui.controls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.gui.controls.helper.QueryResultTableColumnBuilder;
import com.rewe.digital.messaging.events.ShowMessageDetailsEvent;
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

@Named
public class QueryResultDetails extends AnchorPane {
    private final MessageDetails messageDetailsPane;

    ObjectMapper objectMapper = new ObjectMapper();

    private TableView currentSearchResult;
    private TextField filterSearchResultInput;

    private final QueryResultTableColumnBuilder tableColumnBuilder;

    @Inject
    public QueryResultDetails(final QueryResultTableColumnBuilder tableColumnBuilder,
                              final StageFactory stageFactory,
                              final Injector injector) {
        this.tableColumnBuilder = tableColumnBuilder;

        val pane = stageFactory.getParent("scenes/controls/query_result_details.fxml");
        this.getChildren().addAll(pane.getChildrenUnmodifiable());

        currentSearchResult = (TableView) this.lookup("#currentSearchResult");
        currentSearchResult.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filterSearchResultInput = (TextField) this.lookup("#filterSearchResultInput");

        this.messageDetailsPane = injector.getInstance(MessageDetails.class);

        VBox container = (VBox) lookup("VBox");
        container.getChildren().add(messageDetailsPane);
    }

    public void showSearchResult(final List<Map> result,
                                 final String topic) {
        addTableColumns(currentSearchResult, result);

        currentSearchResult.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedMessage) -> {
            this.messageDetailsPane.showMessageDetails(topic, selectedMessage);
        });

        val observableResult = FXCollections.observableArrayList(result);
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
}
