package com.rewe.digital.gui.controller.querying;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.configuration.FileStorageRepository;
import com.rewe.digital.messaging.events.querying.ExecuteQueryEvent;
import com.rewe.digital.model.Query;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Named
public class QueryHistoryController implements Initializable {

    private static final String QUERY_HISTORY_FILE_NAME = "query_history.json";

    @FXML
    Button executeButton;
    @FXML
    Button cancelButton;

    @FXML
    ListView historyList;

    private EventBus eventBus;
    private FileStorageRepository fileStorageRepository;

    @Inject
    public QueryHistoryController(final FileStorageRepository fileStorageRepository,
                                  final EventBus eventBus) {
        this.fileStorageRepository = fileStorageRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        eventBus.register(this);

        val queryHistory = this.fileStorageRepository.getDataFromFile(QUERY_HISTORY_FILE_NAME, List.class);

        queryHistory.ifPresent(list -> {
            historyList.setItems(FXCollections.observableList(list));
        });

        historyList.getSelectionModel()
                .getSelectedItems()
                .addListener((ListChangeListener) c -> executeButton.setDisable(false));
    }

    @Subscribe
    private void logQuery(final ExecuteQueryEvent executeQueryEvent) {
        if (!alreadyKnownQuery(executeQueryEvent)) {
            historyList.getItems().add(0, executeQueryEvent.getQuery().getQuery());
            fileStorageRepository.writeDataToFile(QUERY_HISTORY_FILE_NAME, historyList.getItems());
        }
    }

    private boolean alreadyKnownQuery(ExecuteQueryEvent executeQueryEvent) {
        return historyList.getItems().stream().anyMatch(o -> executeQueryEvent.getQuery().getQuery().equals(o));
    }

    public void closeWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void executeQuery(ActionEvent actionEvent) {
        executeSelectedQuery();
        closeWindow(actionEvent);
    }

    public void execcuteQueryByDoubleClick(MouseEvent mouseEvent) {
        if(mouseEvent.getClickCount() > 1) {
            executeSelectedQuery();
            closeWindow(null);
        }
    }

    private void executeSelectedQuery() {
        final String query = (String) historyList.getSelectionModel().getSelectedItem();
        eventBus.post(new ExecuteQueryEvent(new Query(query), ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW));
    }
}
