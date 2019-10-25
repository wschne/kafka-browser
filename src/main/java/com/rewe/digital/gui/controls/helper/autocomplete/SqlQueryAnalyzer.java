package com.rewe.digital.gui.controls.helper.autocomplete;

import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Named
public class SqlQueryAnalyzer {
    public List<String> getWordAtPosition(final String query, final int position) {
        if (query == null ||
                position <= 0 ||
                query.length() < position ||
                query.substring(position - 1, position).equals(" "))
            return new ArrayList<>();

        val wordBeforePosition = query.substring(position);
        val wordAfterPosition = query.substring(0, position);
        val firstPartOfWord = getFirstPartOfWord(wordAfterPosition);
        val secondPartOfWord = getSecondPartOfWord(wordBeforePosition);
        val word = firstPartOfWord + secondPartOfWord;

        if (word.contains(".")) {
            return Arrays.asList(word.split("\\."));
        } else {
            return Arrays.asList(word);
        }
    }

    public boolean isLastTypedCharacterADot(final String query, final int position) {
        if (query == null) return false;
        if (query.length() == position) return query.charAt(query.length()-1) == '.';
        if (query.length() < position + 1) return false;
        final String character = query.substring(position, position + 1);
        return character.equals(".") || (character.equals(" ") && query.substring(position-1, position).equals("."));
    }

    private String getSecondPartOfWord(String wordBeforePosition) {
        val substringBefore = StringUtils.substringBefore(wordBeforePosition, " ");
        if (StringUtils.isBlank(substringBefore)) {
            return substringBefore;
        } else {
            return substringBefore;
        }
    }

    private String getFirstPartOfWord(String wordAfterPosition) {
        val substringAfterLast = StringUtils.substringAfterLast(wordAfterPosition, " ");
        if (StringUtils.isBlank(substringAfterLast)) {
            return wordAfterPosition;
        } else {
            return substringAfterLast;
        }
    }
}
