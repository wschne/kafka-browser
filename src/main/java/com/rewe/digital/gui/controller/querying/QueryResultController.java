package com.rewe.digital.gui.controller.querying;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.gui.controls.helper.QueryResultTableColumnBuilder;
import com.rewe.digital.kafka.KafkaQueryExecutor;
import com.rewe.digital.messaging.events.querying.ExecuteQueryEvent;
import com.rewe.digital.messaging.events.querying.QueryExecutionFinishedEvent;
import com.rewe.digital.messaging.events.ShowMessageDetailsEvent;
import com.rewe.digital.messaging.events.querying.ShowQueryResultEvent;
import com.rewe.digital.messaging.events.querying.ShowQueryingErrorEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.types.StructType;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

@Named
public class QueryResultController implements Initializable {
    private final Set<String> subscribedTopics = new HashSet<>();

    ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    private TextArea schemaText;

    @FXML
    private Tab schemaTab;

    @FXML
    private Tab currentSearchResultTab;
    @FXML
    private TableView currentSearchResult;

    @FXML
    private TabPane searchResultTabPane;

    @FXML
    public TextField filterSearchResultInput;

    private final EventBus eventBus;
    private final KafkaQueryExecutor kafkaQueryExecutor;
    private final QueryResultTableColumnBuilder tableColumnBuilder;

    @Inject
    public QueryResultController(final EventBus eventBus,
                                 final KafkaQueryExecutor kafkaQueryExecutor,
                                 final QueryResultTableColumnBuilder tableColumnBuilder) {
        this.eventBus = eventBus;
        this.kafkaQueryExecutor = kafkaQueryExecutor;
        this.tableColumnBuilder = tableColumnBuilder;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        eventBus.register(this);

        searchResultTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab == schemaTab) {
                showSchemaOfKnownTopics();
            } else {
                if(isSelectedTabASearchResultTab(newTab)) {
                    currentSearchResultTab = newTab;
                    currentSearchResult = (TableView) newTab.getContent();
                }
            }
        });
    }

    @Subscribe
    public void topicClickHadlingFinished(final QueryExecutionFinishedEvent queryExecutionFinishedEvent) {
        final String topicName = queryExecutionFinishedEvent.getTopicName();
        subscribedTopics.add(topicName);
    }

    @Subscribe
    private void showQueryErrorResult(final ShowQueryingErrorEvent queryingErrorEvent) {
        val searchError = new TextArea(queryingErrorEvent.getErrorMessage());
        val errorTab = new Tab("Error", searchError);
        Platform.runLater(
                () -> {
                    searchResultTabPane.getTabs().add(errorTab);
                    searchResultTabPane.getSelectionModel().select(errorTab);
                }
        );
    }

    @Subscribe
    private void showSearchResult(final ShowQueryResultEvent showQueryResultEvent) {
        val result = showQueryResultEvent.getResult();

        setResultTargetTab(showQueryResultEvent, result);

        currentSearchResult.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedMessage) -> {
            eventBus.post(new ShowMessageDetailsEvent(selectedMessage));
        });

        addTableColumns(currentSearchResult, result);

        val observableResult = FXCollections.observableArrayList(result);
        val filteredData = new FilteredList<>(observableResult, p -> true);

        filterSearchResultInput.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(row -> {
                // If filter text is empty, display all items.
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

    private void setResultTargetTab(final ShowQueryResultEvent showQueryResultEvent,
                                    final List<Map> result) {
        val resultTarget = showQueryResultEvent.getTarget();
        val topicName = showQueryResultEvent.getTopicName();
        if (resultTarget == ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW) {
            Platform.runLater(
                    () -> {
                        currentSearchResultTab.setText(topicName + " (" + result.size() + ")");
                        searchResultTabPane.getSelectionModel().select(currentSearchResultTab);
                    }
            );
        } else {
            currentSearchResult = new TableView<>();
            currentSearchResult.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            currentSearchResultTab = new Tab(topicName + " (" + result.size() + ")", currentSearchResult);
            currentSearchResultTab.setClosable(true);

            Platform.runLater(
                    () -> {
                        searchResultTabPane.getTabs().add(currentSearchResultTab);
                        searchResultTabPane.getSelectionModel().select(currentSearchResultTab);
                    }
            );
        }
    }

    private boolean isSelectedTabASearchResultTab(final Tab newTab) {
        return !newTab.getText().contains("Error") && newTab.getContent() instanceof TableView;
    }

    private void showSchemaOfKnownTopics() {
        StringBuilder topicSchemas = new StringBuilder();
        schemaText.clear();
        subscribedTopics.forEach(s -> {
            topicSchemas.append("------------------ ").append(s).append(" --------------------\n");
            Optional<StructType> schema = kafkaQueryExecutor.getTopicSchema(s);
            schema.ifPresent(structType -> topicSchemas.append(structType.treeString()).append('\n'));
        });
        schemaText.setText(topicSchemas.toString());
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
