package com.rewe.digital.gui.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.KafkaBrowserMain;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.gui.topiclist.ConsumerStartStopEventHandler;
import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.kafka.topics.Topic;
import com.rewe.digital.kafka.topics.TopicsService;
import com.rewe.digital.messaging.events.KafkaConnectionSelectedEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Named
public class KafkaBrowserController implements Initializable {
    private TopicsService topicsService;
    private final ConsumerStartStopEventHandler consumerStartStopEventHandler;

    @FXML
    private ListView availableTopicsList;
    @FXML
    private TextField filterTopicsInput;

    private final StageFactory stageFactory;
    private final EventBus eventBus;

    @Inject
    public KafkaBrowserController(TopicsService topicsService,
                                  ConsumerStartStopEventHandler consumerStartStopEventHandler,
                                  StageFactory stageFactory,
                                  EventBus eventBus) {
        this.topicsService = topicsService;
        this.consumerStartStopEventHandler = consumerStartStopEventHandler;
        this.stageFactory = stageFactory;
        this.eventBus = eventBus;
    }

    @Override
    public void initialize(final URL url,
                           final ResourceBundle rb) {
        this.eventBus.register(this);

        availableTopicsList.setCellFactory(param -> new TopicListItem(consumerStartStopEventHandler));
    }

    @Subscribe
    public void onKafkaConnectionSelected(KafkaConnectionSelectedEvent event) {
        final List<Topic> topics = new ArrayList<>(topicsService.topics());
        ObservableList<Topic> availableTopcs = FXCollections.observableArrayList(topics);

        Platform.runLater(() -> {
            availableTopicsList.getItems().removeAll();

            val filteredData = new FilteredList<>(availableTopcs, p -> true);
            filterTopicsInput.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(row -> {
                    // If filter text is empty, display all items.
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }

                    val lowerCaseFilter = newValue.toLowerCase();

                    return StringUtils.containsIgnoreCase(row.getId(), lowerCaseFilter);
                });
            });

            val sortedData = new SortedList<>(filteredData);

            availableTopicsList.setItems(sortedData);

            availableTopicsList.refresh();
        });
    }

    public void onSwitchConnection(ActionEvent actionEvent) {
        stageFactory.openOverallConnectionsStage(KafkaBrowserMain.APPLICATION_VERSION);

        Stage stage = (Stage) availableTopicsList.getScene().getWindow();
        stage.close();
    }
}
