package com.rewe.digital.gui.controls.helper.autocomplete;

public class CalculateCaretPosition {
    public int forSelectedEntry(final int position,
                                final String newQuery) {
        int pos=newQuery.length();
        for(int i = position; i< newQuery.length(); i++) {
            if (newQuery.charAt(i) == ' ') {
                pos = i;
                break;
            }
        }

        return pos;
    }
}
