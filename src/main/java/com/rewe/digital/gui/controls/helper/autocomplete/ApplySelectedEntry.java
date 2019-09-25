package com.rewe.digital.gui.controls.helper.autocomplete;

import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;

@Named
public class ApplySelectedEntry {

    private SqlQueryAnalyzer sqlQueryAnalyzer;

    public ApplySelectedEntry(SqlQueryAnalyzer sqlQueryAnalyzer) {
        this.sqlQueryAnalyzer = sqlQueryAnalyzer;
    }

    public String toQuery(String entry, int position, String query) {
        val wordsToReplace = sqlQueryAnalyzer.getWordAtPosition(query, position);
        if (sqlQueryAnalyzer.isLastTypedCharacterADot(query, position) || wordsToReplace.isEmpty()) {
            return insertString(query, entry, position);
        } else {
            val stringToReplace = String.join(".", wordsToReplace);
            val relevantWords = wordsToReplace.subList(0, wordsToReplace.size() - 1);
            val stringToKeep = String.join(".", relevantWords);
            if (StringUtils.isNotBlank(stringToKeep)) {
                return replaceString(query, stringToReplace, stringToKeep + "." + entry, position);
            } else {
                return replaceString(query, stringToReplace, entry, position);
            }
        }
    }

    private String insertString(String originalString,
                                String stringToBeInserted,
                                int index) {
        StringBuilder newString = new StringBuilder(originalString);
        newString.insert(index, stringToBeInserted);
        return newString.toString();
    }

    private String replaceString(String originalString,
                                 String stringToBeReplaced,
                                 String stringToBeInserted,
                                 int index) {
        val startOfStringToBeRepaced = index - stringToBeReplaced.length();
        StringBuilder newString = new StringBuilder(originalString);
        newString.delete(startOfStringToBeRepaced, startOfStringToBeRepaced + stringToBeReplaced.length());
        newString.insert(startOfStringToBeRepaced, stringToBeInserted);
        return newString.toString();
    }
}
