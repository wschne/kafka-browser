package com.rewe.digital.gui.controls;

import com.rewe.digital.gui.controls.helper.autocomplete.ApplySelectedEntry;
import com.rewe.digital.gui.controls.helper.autocomplete.AutocompletePopUp;
import com.rewe.digital.gui.controls.helper.autocomplete.CalculateCaretPosition;
import com.rewe.digital.gui.controls.helper.autocomplete.SparkSchemaTraverseUtil;
import com.rewe.digital.gui.controls.helper.autocomplete.SqlQueryAnalyzer;
import com.rewe.digital.model.Query;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Named
public class AutoCompleteTextField extends TextField {
    private SqlQueryAnalyzer sqlQueryAnalyzer = new SqlQueryAnalyzer();
    private SparkSchemaTraverseUtil sparkSchemaTraverseUtil = new SparkSchemaTraverseUtil();
    private AutocompletePopUp autocompletePopUp = new AutocompletePopUp();
    private ApplySelectedEntry applySelectedEntry = new ApplySelectedEntry(sqlQueryAnalyzer);
    private CalculateCaretPosition calculateCaretPosition = new CalculateCaretPosition();
    private List<StructField> entries;
    private ContextMenu entriesPopup;
    public SparkSession sparkSession;
    private Consumer<KeyEvent> onEnterKeyPressed;

    public AutoCompleteTextField() {
        super();
        entries = new ArrayList<>();
        entriesPopup = new ContextMenu();
        textProperty().addListener((observableValue, oldText, newText) -> calculateAutocompletePupup(newText));

        focusedProperty().addListener((observableValue, aBoolean, aBoolean2) -> entriesPopup.hide());

        this.autocompletePopUp.setEntrySelectedEvent(newEntrySelected());

        setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
                calculateAutocompletePupup(getText());
                this.autocompletePopUp.showEntriesPopUp(AutoCompleteTextField.this, entries);
            } else if (!this.autocompletePopUp.isPopupOpen() && event.getCode() == KeyCode.ENTER) {
                onEnterKeyPressed.accept(event);
            }
        });
    }

    private void calculateAutocompletePupup(String newText) {
        if (getText().length() == 0) {
            entriesPopup.hide();
        } else {
            Query query = new Query(newText);
            val position = getCaretPosition();

            val wordUnderCursor = sqlQueryAnalyzer.getWordAtPosition(query.getQuery(), position);

            val topicName = query.getNormalizedTopicName();
            if (StringUtils.isNotBlank(topicName)) {
                final StructType schema = sparkSession.sqlContext().table(topicName).schema();
                List<StructField> allColumnNames = sparkSchemaTraverseUtil.getChildrenOfField(wordUnderCursor, schema);

                entries.clear();
                entries.addAll(allColumnNames);
            }

            if (sqlQueryAnalyzer.isLastTypedCharacterADot(query.getQuery(), position)) {
                autocompletePopUp.showEntriesPopUp(AutoCompleteTextField.this, entries);
            } else {
                if (wordUnderCursor.size() > 0) {
                    val lastWordUnderCursor = wordUnderCursor.get(wordUnderCursor.size() - 1);
                    autocompletePopUp.applySearchStringToSelectableEntries(AutoCompleteTextField.this, entries, lastWordUnderCursor);
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

            setText(newQuery);
            positionCaret(calculateCaretPosition.forSelectedEntry(position, newQuery));

            entriesPopup.hide();
        };
    }
}
