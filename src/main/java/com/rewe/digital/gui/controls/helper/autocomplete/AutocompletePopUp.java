package com.rewe.digital.gui.controls.helper.autocomplete;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.types.NumericType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutocompletePopUp {
    private ContextMenu entriesPopup = new ContextMenu();
    private Consumer<String> eventCallback;

    public void setEntrySelectedEvent(final Consumer<String> eventCallback) {
        this.eventCallback = eventCallback;
    }

    public void applySearchStringToSelectableEntries(final Node anchor,
                                                     final List<StructField> availableEntries,
                                                     final String typingWord) {
        val searchResult = new LinkedList<>(findMatchingFieldsByName(availableEntries, typingWord));
        if (searchResult.size() > 0) {
            populatePopup(searchResult);
            showPopup(anchor);
        } else {
            entriesPopup.hide();
        }
    }

    private List<StructField> findMatchingFieldsByName(final List<StructField> availableFields, final String fieldName) {
        return availableFields.stream()
                .filter(s -> StringUtils.containsIgnoreCase(s.name(), fieldName))
                .collect(Collectors.toList());
    }

    public void showEntriesPopUp(final Node anchor,
                                 final List<StructField> availableEntries) {
        populatePopup(new ArrayList<>(availableEntries));
        showPopup(anchor);
    }

    public void showPopup(Node anchor) {
        if (!entriesPopup.isShowing()) {
            entriesPopup.show(anchor, Side.BOTTOM, 0, 0);
        }
    }

    public boolean isPopupOpen() {
        return entriesPopup.isShowing();
    }

    private void populatePopup(List<StructField> searchResult) {
        List<MenuItem> menuItems = new LinkedList<>();
        for (final StructField result : searchResult) {
            val icon = getMenuIconForType(result);
            val openView = new ImageView(icon);
            openView.setFitWidth(15);
            openView.setFitHeight(15);
            val newMenuItem = new MenuItem(result.name());
            newMenuItem.setGraphic(openView);

            newMenuItem.setOnAction(event -> {
                eventCallback.accept(result.name());
            });
            menuItems.add(newMenuItem);
        }
        entriesPopup.getItems().clear();
        entriesPopup.getItems().addAll(menuItems);
    }

    private Image getMenuIconForType(StructField result) {
        if (result.dataType() instanceof StructType) {
            return new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("images/struct.png")));
        } else if (result.dataType() instanceof NumericType) {
            return new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("images/number.png")));
        } else {
            return new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("images/string.png")));
        }
    }
}
