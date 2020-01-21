package com.rewe.digital.gui.controller.querying;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;
import com.rewe.digital.gui.controls.QueryResultDetails;
import com.rewe.digital.messaging.events.querying.ExecuteQueryEvent;
import com.rewe.digital.messaging.events.querying.ShowQueryResultEvent;
import com.rewe.digital.messaging.events.querying.ShowQueryingErrorEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Named
public class QueryResultsController implements Initializable {
    @FXML
    private TabPane searchResultTabPane;

    private final EventBus eventBus;
    private final Injector injector;

    @Inject
    public QueryResultsController(final EventBus eventBus,
                                  final Injector injector) {
        this.eventBus = eventBus;
        this.injector = injector;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        eventBus.register(this);
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
        val resultTarget = showQueryResultEvent.getTarget();
        val topicName = showQueryResultEvent.getTopicName();
        val result = showQueryResultEvent.getResult();
        val tabTitle = topicName + " (" + result.size() + ")";

        if (resultTarget == ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW) {
            Platform.runLater( () -> updateOrCreateCurrentSearchResultTab(tabTitle, topicName, result) );
        } else {
            Platform.runLater( () -> createAndAppendNewTab(tabTitle, topicName, result) );
        }
    }

    private void updateOrCreateCurrentSearchResultTab(final String title,
                                                      final String topicName,
                                                      final List<Map> messageDetails) {
        val currentTab = getCurrentSearchResultTab();

        if(!currentTab.isPresent()) {
            createAndAppendNewTab(title, topicName, messageDetails);
        } else {
            QueryResultDetails resultDetails = (QueryResultDetails) currentTab.get().getContent();
            currentTab.get().setText(title);
            resultDetails.showSearchResult(messageDetails, topicName);
        }
    }

    private Tab createAndAppendNewTab(final String title,
                                      final String topicName,
                                      final List<Map> messageDetails) {
        val content = injector.getInstance(QueryResultDetails.class);
        content.showSearchResult(messageDetails, topicName);
        final Tab tab = new Tab(title, content);
        tab.setClosable(true);
        searchResultTabPane.getTabs().add(tab);
        searchResultTabPane.getSelectionModel().select(tab);
        return tab;
    }

    private Optional<Tab> getCurrentSearchResultTab() {
        val selectedTab = Optional.ofNullable(searchResultTabPane.getSelectionModel().getSelectedItem());
        return selectedTab.filter(this::isSelectedTabASearchResultTab);
    }

    private boolean isSelectedTabASearchResultTab(final Tab newTab) {
        return !newTab.getText().contains("Error") && newTab.getContent() instanceof QueryResultDetails;
    }
}