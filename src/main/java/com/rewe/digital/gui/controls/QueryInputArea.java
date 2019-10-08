package com.rewe.digital.gui.controls;

import com.rewe.digital.gui.controls.helper.autocomplete.ApplySelectedEntry;
import com.rewe.digital.gui.controls.helper.autocomplete.AutocompletePopUp;
import com.rewe.digital.gui.controls.helper.autocomplete.CalculateCaretPosition;
import com.rewe.digital.gui.controls.helper.autocomplete.SparkSchemaTraverseUtil;
import com.rewe.digital.gui.controls.helper.autocomplete.SqlQueryAnalyzer;
import com.rewe.digital.model.Query;
import javafx.geometry.Bounds;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.fxmisc.richtext.InlineCssTextArea;
import org.reactfx.EventStream;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.reactfx.EventStreams.nonNullValuesOf;

@Named
public class QueryInputArea extends InlineCssTextArea {
    private SqlQueryAnalyzer sqlQueryAnalyzer = new SqlQueryAnalyzer();
    private SparkSchemaTraverseUtil sparkSchemaTraverseUtil = new SparkSchemaTraverseUtil();
    private AutocompletePopUp autocompletePopUp = new AutocompletePopUp();
    private ApplySelectedEntry applySelectedEntry = new ApplySelectedEntry(sqlQueryAnalyzer);
    private CalculateCaretPosition calculateCaretPosition = new CalculateCaretPosition();
    private List<StructField> entries;
    public SparkSession sparkSession;
    private Consumer<KeyEvent> onEnterKeyPressed;

    private Stage stage;

    public QueryInputArea() {
        super();
        entries = new ArrayList<>();

        focusedProperty().addListener((observableValue, aBoolean, aBoolean2) -> autocompletePopUp.hide());

        this.autocompletePopUp.setEntrySelectedEvent(newEntrySelected());

        setOnKeyReleased(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
                val popupOpened = calculateAutocompletePupup(getText());
                if (!popupOpened) {
                    this.autocompletePopUp.showEntriesPopUp(this.getStage(), entries);
                }
            } else if (!this.autocompletePopUp.isPopupOpen() && event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                onEnterKeyPressed.accept(event);
            } else if (!event.getCode().isArrowKey() &&
                    !event.getCode().isFunctionKey() &&
                    !event.getCode().isKeypadKey() &&
                    !event.getCode().isMediaKey() &&
                    !event.getCode().isModifierKey() &&
                    !event.getCode().isNavigationKey()) {
                calculateAutocompletePupup(getText());
            } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT){
                autocompletePopUp.hide();
            }
        });

        EventStream<Optional<Bounds>> caretBounds = nonNullValuesOf(this.caretBoundsProperty());

        // set up event streams to update popups every time bounds change
        double caretXOffset = -10;
        double caretYOffset = 0;

        caretBounds
                .subscribe(opt -> {
                    if (opt.isPresent()) {
                        Bounds b = opt.get();
                        autocompletePopUp.setX(b.getMaxX() + caretXOffset);
                        autocompletePopUp.setY(b.getMaxY() + caretYOffset);
                    }
                });
    }

    private boolean calculateAutocompletePupup(String newText) {
        if (getText().length() == 0) {
            autocompletePopUp.hide();
            return false;
        } else {
            Query query = new Query(newText);
            val position = getCaretPosition();

            val wordUnderCursor = sqlQueryAnalyzer.getWordAtPosition(query.getFlattenedQuery(), position);

            val topicName = query.getNormalizedTopicName();
            if (StringUtils.isNotBlank(topicName)) {
                final StructType schema = sparkSession.sqlContext().table(topicName).schema();
                List<StructField> allColumnNames = sparkSchemaTraverseUtil.getChildrenOfField(wordUnderCursor, schema);

                entries.clear();
                entries.addAll(allColumnNames);
            }

            if (sqlQueryAnalyzer.isLastTypedCharacterADot(query.getFlattenedQuery(), position)) {
                autocompletePopUp.showEntriesPopUp(this.getStage(), entries);
                return true;
            } else {
                if (wordUnderCursor.size() > 0) {
                    val lastWordUnderCursor = wordUnderCursor.get(wordUnderCursor.size() - 1);
                    autocompletePopUp.applySearchStringToSelectableEntries(this.getStage(), entries, lastWordUnderCursor);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public void setOnEnterEventCallback(Consumer<KeyEvent> onEnterKeyPressed) {
        this.onEnterKeyPressed = onEnterKeyPressed;
    }

    public Consumer<String> newEntrySelected() {
        return entry -> {
            val position = getCaretPosition();
            val query = getText();
            val newQuery = applySelectedEntry.toQuery(entry, position, query);

            replaceText(newQuery);
            final int caretPosition = calculateCaretPosition.forSelectedEntry(position, newQuery);
            moveTo(caretPosition);

            autocompletePopUp.hide();
        };
    }

    public Stage getStage() {
        if (this.stage == null) {
            this.stage = (Stage) this.getScene().getWindow();
        }
        return stage;
    }
}
