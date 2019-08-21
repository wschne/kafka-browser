package com.rewe.digital.gui.topiclist;

import com.rewe.digital.kafka.topics.Topic;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.function.Function;

public class TopicListItem extends ListCell<Topic> {
    ButtonState buttonState = ButtonState.started;
    HBox hbox = new HBox();
    Label label = new Label("(empty)");
    Pane pane = new Pane();
    Button button = new Button();

    public TopicListItem(EventHandler<ActionEvent> buttonClickEvent) {
        super();
        hbox.getChildren().addAll(button, label, pane);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(5);

        HBox.setHgrow(pane, Priority.ALWAYS);
        button.setOnAction(event -> {
            Function<ButtonState, Boolean> topicConsumerStateChangedEvent = newState -> {
                buttonState = newState;
                Platform.runLater(() -> button.setGraphic(new ImageView(getImage(buttonState))));
                return true;
            };
            buttonClickEvent.handle(new ActionEvent(new TopicListItemClickedEvent(buttonState, label.getText(), topicConsumerStateChangedEvent), event.getTarget()));
        });
    }

    @Override
    protected void updateItem(Topic item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        if (empty) {
            setGraphic(null);
        } else {
            final String topicName = item.getId();
            label.setText(topicName != null ? topicName : "<null>");
            buttonState = ButtonState.stopped;
            button.setGraphic(new ImageView(getImage(buttonState)));

            setGraphic(hbox);
        }
    }

    private Image getImage(ButtonState buttonState) {
        if (buttonState == ButtonState.stopped) {
            return new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("images/start.png")));
        }
        if (buttonState == ButtonState.loading) {
            return new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("images/loading.gif")), 15, 15, false, false);
        } else {
            return new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("images/stop.png")));
        }
    }

    public static class TopicListItemClickedEvent {
        public ButtonState buttonState;
        public String topicName;
        public Function<ButtonState, Boolean> topicConsumerStateChangedEvent;

        public TopicListItemClickedEvent(ButtonState buttonState,
                                         String topicName,
                                         Function<ButtonState, Boolean> topicConsumerStateChangedEvent) {
            this.buttonState = buttonState;
            this.topicName = topicName;
            this.topicConsumerStateChangedEvent = topicConsumerStateChangedEvent;
        }
    }

    public enum ButtonState {
        started,
        loading,
        stopped;

        ButtonState followState() {
            switch (this) {
                case started:
                    return loading;
                case loading:
                    return stopped;
                case stopped:
                    return started;
                default:
                    return stopped;
            }
        }
    }
}

