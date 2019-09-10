package com.rewe.digital.gui.controller.querying;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rewe.digital.gui.StageFactory;
import com.rewe.digital.messaging.events.querying.ExecuteQueryEvent;
import com.rewe.digital.messaging.events.querying.QueryExecutionFinishedEvent;
import com.rewe.digital.messaging.events.TopicEmptyEvent;
import com.rewe.digital.messaging.events.WaitForKafkaMessagesEvent;
import com.rewe.digital.model.Query;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

@Named
public class QueryCompositionController implements Initializable {

    @FXML
    private TextField queryInput;

    @FXML
    private SplitMenuButton executeButton;

    @FXML
    private Label labelWaitForData;

    @Inject
    private EventBus eventBus;

    @Inject
    private StageFactory stageFactory;

    private Stage queryHistoryWindow;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        eventBus.register(this);

        queryHistoryWindow = stageFactory.createStage("scenes/query/query_history.fxml",
                "styles.css",
                "Query history");
        queryHistoryWindow.setResizable(false);
        queryHistoryWindow.setAlwaysOnTop(true);
        queryHistoryWindow.initStyle(StageStyle.UTILITY);
    }

    @FXML
    public void onEnter(KeyEvent ae) {
        if (ae.getCode() == KeyCode.ENTER) {
            if (ae.isControlDown()) {
                executeQuery(ExecuteQueryEvent.ResultTarget.NEW_WINDOW);
            } else {
                executeQuery(ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW);
            }
        }
    }

    @FXML
    public void onExecutionClick(MouseEvent ae) {
        executeQuery(ExecuteQueryEvent.ResultTarget.CURRENT_WINDOW);
    }

    @FXML
    public void onExecuteInNewWindowClick(ActionEvent ae) {
        executeQuery(ExecuteQueryEvent.ResultTarget.NEW_WINDOW);
    }

    private void executeQuery(ExecuteQueryEvent.ResultTarget target) {
        String query = queryInput.getText();
        Platform.runLater(() -> setExecButtonImage("images/loading.gif"));
        eventBus.post(new ExecuteQueryEvent(new Query(query), target));
    }

    public void showQueryHistory(MouseEvent mouseEvent) {
        queryHistoryWindow.show();
    }

    private void setExecButtonImage(String imageFilePath) {
        Image loadingImage = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imageFilePath)), 15, 15, false, false);
        executeButton.setGraphic(new ImageView(loadingImage));
    }

    @Subscribe
    public void setUiToWaitForDataToAppear(final WaitForKafkaMessagesEvent waitForDataToAppear) {
        Platform.runLater(
                () -> {
                    labelWaitForData.setVisible(true);
                    labelWaitForData.setText("Wait for data to appear...");
                    executeButton.setDisable(true);
                    queryInput.setText(waitForDataToAppear.getQuery().getQuery());
                }
        );
    }

    @Subscribe
    public void handleExecuteQueryEvent(final ExecuteQueryEvent executeQueryEvent) {
        Platform.runLater(() -> {
            queryInput.setText(executeQueryEvent.getQuery().getQuery());
        });
    }

    @Subscribe
    public void handleTopicEmptyEvent(final TopicEmptyEvent topicEmptyEvent) {
        Platform.runLater(() -> {
            labelWaitForData.setVisible(true);
            labelWaitForData.setText("Topic " + topicEmptyEvent.getTopicName() + " seems to be empty.");
            executeButton.setDisable(false);
            PauseTransition hideCircle = new PauseTransition(Duration.seconds(5));
            hideCircle.setOnFinished(event1 -> labelWaitForData.setVisible(false));
            hideCircle.play();
        });
    }

    @Subscribe
    public void handleQueryExecutionFinishedEvent(final QueryExecutionFinishedEvent queryExecutionFinishedEvent) {
        final String topicName = queryExecutionFinishedEvent.getTopicName();
        Platform.runLater(() -> {
            labelWaitForData.setVisible(false);
            executeButton.setDisable(false);
            executeButton.setGraphic(null);
        });
    }
}
