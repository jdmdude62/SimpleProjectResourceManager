package com.subliminalsearch.simpleprojectresourcemanager.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.util.stream.Collectors;

/**
 * Utility class to add Google-like autocomplete functionality to ComboBoxes
 */
public class AutoCompleteComboBox {
    
    /**
     * Configures a ComboBox to have autocomplete functionality
     * @param comboBox The ComboBox to configure
     * @param <T> The type of items in the ComboBox
     */
    public static <T> void configure(ComboBox<T> comboBox) {
        configure(comboBox, null);
    }
    
    /**
     * Configures a ComboBox to have autocomplete functionality with a custom converter
     * @param comboBox The ComboBox to configure
     * @param converter Custom StringConverter for display
     * @param <T> The type of items in the ComboBox
     */
    public static <T> void configure(ComboBox<T> comboBox, StringConverter<T> converter) {
        // Store the original items
        ObservableList<T> originalItems = FXCollections.observableArrayList(comboBox.getItems());
        
        // Set converter if provided
        if (converter != null) {
            comboBox.setConverter(converter);
        }
        
        // Get the editor (text field) of the ComboBox
        TextField editor = comboBox.getEditor();
        
        // Add key released event handler for filtering
        EventHandler<KeyEvent> keyHandler = event -> {
            if (event.getCode() == KeyCode.UP || 
                event.getCode() == KeyCode.DOWN || 
                event.getCode() == KeyCode.LEFT ||
                event.getCode() == KeyCode.RIGHT ||
                event.getCode() == KeyCode.ENTER ||
                event.getCode() == KeyCode.TAB ||
                event.getCode() == KeyCode.ESCAPE) {
                return;
            }
            
            String searchText = editor.getText().toLowerCase();
            
            if (searchText.isEmpty()) {
                // Show all items if search is empty
                comboBox.setItems(originalItems);
                if (!comboBox.isShowing()) {
                    comboBox.show();
                }
            } else {
                // Filter items based on search text
                ObservableList<T> filteredItems = originalItems.stream()
                    .filter(item -> {
                        String itemText;
                        if (converter != null) {
                            itemText = converter.toString(item);
                        } else {
                            itemText = item.toString();
                        }
                        return itemText != null && itemText.toLowerCase().contains(searchText);
                    })
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
                
                // Update the items
                comboBox.setItems(filteredItems);
                
                // Show dropdown if there are matches
                if (!filteredItems.isEmpty()) {
                    if (!comboBox.isShowing()) {
                        comboBox.show();
                    }
                } else {
                    // If no matches, restore original items but keep the typed text
                    comboBox.setItems(originalItems);
                    editor.setText(searchText);
                    editor.positionCaret(searchText.length());
                }
            }
        };
        
        editor.setOnKeyReleased(keyHandler);
        
        // Handle selection changes
        comboBox.setOnAction(event -> {
            T selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Restore all items after selection
                comboBox.setItems(originalItems);
            }
        });
        
        // Focus handling - show all items when focused
        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused && editor.getText().isEmpty()) {
                comboBox.setItems(originalItems);
            }
        });
    }
    
    /**
     * Creates an autocomplete ComboBox for String items
     * @param items The items to populate the ComboBox
     * @return A configured ComboBox with autocomplete
     */
    public static ComboBox<String> createStringComboBox(ObservableList<String> items) {
        ComboBox<String> comboBox = new ComboBox<>(items);
        comboBox.setEditable(true);
        configure(comboBox);
        return comboBox;
    }
    
    /**
     * Updates the items in an autocomplete ComboBox while preserving functionality
     * @param comboBox The ComboBox to update
     * @param newItems The new items to set
     * @param <T> The type of items
     */
    public static <T> void updateItems(ComboBox<T> comboBox, ObservableList<T> newItems) {
        // Store current value
        T currentValue = comboBox.getValue();
        
        // Update items
        comboBox.setItems(newItems);
        
        // Restore value if it exists in new items
        if (currentValue != null && newItems.contains(currentValue)) {
            comboBox.setValue(currentValue);
        }
        
        // Reconfigure autocomplete with new items
        configure(comboBox, comboBox.getConverter());
    }
}