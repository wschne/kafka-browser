package com.rewe.digital.gui.controller;

import com.rewe.digital.gui.topiclist.ConsumerStartStopEventHandler;
import com.rewe.digital.gui.topiclist.TopicListItem;
import com.rewe.digital.kafka.topics.Topic;
import com.rewe.digital.kafka.topics.TopicsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Named
public class KafkaBrowserController implements Initializable {
    private final TopicsService topicsService;
    private final ConsumerStartStopEventHandler consumerStartStopEventHandler;

    @FXML
    private ListView availableTopicsList;

    @Inject
    public KafkaBrowserController(TopicsService topicsService,
                                  ConsumerStartStopEventHandler consumerStartStopEventHandler) {
        this.topicsService = topicsService;
        this.consumerStartStopEventHandler = consumerStartStopEventHandler;
    }

    @Override
    public void initialize(final URL url,
                           final ResourceBundle rb) {
        final List<Topic> topics = new ArrayList<>(topicsService.topics());
        ObservableList<Topic> availableTopcs = FXCollections.observableArrayList(topics);
        availableTopicsList.setItems(availableTopcs);
        availableTopicsList.setCellFactory(param -> new TopicListItem(consumerStartStopEventHandler));
    }
}
