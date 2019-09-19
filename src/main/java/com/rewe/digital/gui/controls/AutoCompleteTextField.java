package com.rewe.digital.gui.controls;

import com.rewe.digital.gui.controls.helper.autocomplete.AutocompletePopUp;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AutoCompleteTextField extends TextField {
    private SqlQueryAnalyzer sqlQueryAnalyzer = new SqlQueryAnalyzer();
    private SparkSchemaTraverseUtil sparkSchemaTraverseUtil = new SparkSchemaTraverseUtil();
    private AutocompletePopUp autocompletePopUp = new AutocompletePopUp();
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

        autocompletePopUp.setEntrySelectedEvent(newEntrySelected());

        setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
                calculateAutocompletePupup(getText());
                autocompletePopUp.showEntriesPopUp(AutoCompleteTextField.this, entries);
            } else if (!autocompletePopUp.isPopupOpen() && event.getCode() == KeyCode.ENTER) {
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
            val newQuery = getNewQuery(entry, position, query);
            setText(newQuery);

            setCursorPosition(entry, position, newQuery);
            entriesPopup.hide();
        };
    }

    private void setCursorPosition(final String entry,
                                   final int position,
                                   final String newQuery) {
        val wordAtPosition = sqlQueryAnalyzer.getWordAtPosition(newQuery, position);
        if (!wordAtPosition.isEmpty()) {
            val newFieldsString = String.join(".", wordAtPosition);
            val newCursorPosition = newQuery.indexOf(newFieldsString) + newFieldsString.length();
            positionCaret(newCursorPosition);
        } else {
            val newCursorPosition = position + entry.length();
            positionCaret(newCursorPosition);
        }
    }

    private String getNewQuery(String entry, int position, String query) {
        val wordsToReplace = sqlQueryAnalyzer.getWordAtPosition(query, position);
        if (sqlQueryAnalyzer.isLastTypedCharacterADot(query, position) || wordsToReplace.isEmpty()) {
            return insertString(query, entry, position);
        } else {
            val stringToReplace = String.join(".", wordsToReplace);
            val relevantWords = wordsToReplace.subList(0, wordsToReplace.size() - 1);
            val stringToKeep = String.join(".", relevantWords);
            if (StringUtils.isNotBlank(stringToKeep)) {
                return query.replace(stringToReplace, stringToKeep + "." + entry);
            } else {
                return query.replace(stringToReplace, entry);
            }
        }
    }

    public String insertString(String originalString,
                               String stringToBeInserted,
                               int index) {
        StringBuilder newString = new StringBuilder(originalString);
        newString.insert(index, stringToBeInserted);
        return newString.toString();
    }
}
