package com.rewe.digital.gui.controls.helper.autocomplete;

public class CalculateCaretPosition {
    public int forSelectedEntry(final int position,
                                final String newQuery) {
        final String normalizedQuery = newQuery.replace("\n", " ");
        int pos=normalizedQuery.length();
        for(int i = position; i< normalizedQuery.length(); i++) {
            if (normalizedQuery.charAt(i) == ' ') {
                pos = i;
                break;
            }
        }

        return pos;
    }
}
