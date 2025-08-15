package com.subliminalsearch.simpleprojectresourcemanager.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.function.UnaryOperator;

/**
 * Utility class for formatting phone numbers in text fields
 */
public class PhoneFormatter {
    
    /**
     * Apply US phone number formatting to a TextField
     * Formats as: (555) 123-4567
     */
    public static void applyPhoneFormat(TextField phoneField) {
        UnaryOperator<TextFormatter.Change> phoneFilter = change -> {
            String newText = change.getControlNewText();
            
            // Remove all non-digits for processing
            String digitsOnly = newText.replaceAll("[^\\d]", "");
            
            // Limit to 10 digits (US phone number without country code)
            if (digitsOnly.length() > 10) {
                return null; // Reject the change
            }
            
            // Format the number
            String formatted = formatPhoneNumber(digitsOnly);
            
            // Calculate new caret position
            int caretPosition = calculateCaretPosition(change.getCaretPosition(), 
                                                       change.getControlText(), 
                                                       formatted);
            
            change.setText(formatted);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(caretPosition);
            change.setAnchor(caretPosition);
            
            return change;
        };
        
        StringConverter<String> converter = new StringConverter<String>() {
            @Override
            public String toString(String object) {
                if (object == null || object.isEmpty()) {
                    return "";
                }
                // Remove formatting for storage
                return object.replaceAll("[^\\d]", "");
            }
            
            @Override
            public String fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return "";
                }
                String digitsOnly = string.replaceAll("[^\\d]", "");
                return formatPhoneNumber(digitsOnly);
            }
        };
        
        TextFormatter<String> formatter = new TextFormatter<>(converter, "", phoneFilter);
        phoneField.setTextFormatter(formatter);
        
        // Set prompt text to show format
        if (phoneField.getPromptText() == null || phoneField.getPromptText().isEmpty()) {
            phoneField.setPromptText("(555) 123-4567");
        }
        
        // Format any existing text
        String currentText = phoneField.getText();
        if (currentText != null && !currentText.isEmpty()) {
            String digitsOnly = currentText.replaceAll("[^\\d]", "");
            phoneField.setText(formatPhoneNumber(digitsOnly));
        }
    }
    
    /**
     * Format a string of digits into US phone format
     */
    private static String formatPhoneNumber(String digitsOnly) {
        if (digitsOnly == null || digitsOnly.isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        int len = digitsOnly.length();
        
        if (len > 0) {
            formatted.append("(");
        }
        
        // Area code
        for (int i = 0; i < Math.min(3, len); i++) {
            formatted.append(digitsOnly.charAt(i));
        }
        
        if (len >= 3) {
            formatted.append(") ");
        }
        
        // First 3 digits
        for (int i = 3; i < Math.min(6, len); i++) {
            formatted.append(digitsOnly.charAt(i));
        }
        
        if (len >= 6) {
            formatted.append("-");
        }
        
        // Last 4 digits
        for (int i = 6; i < Math.min(10, len); i++) {
            formatted.append(digitsOnly.charAt(i));
        }
        
        return formatted.toString();
    }
    
    /**
     * Calculate where the caret should be after formatting
     */
    private static int calculateCaretPosition(int originalPosition, String oldText, String newText) {
        // Count digits before original caret position
        String beforeCaret = oldText.substring(0, Math.min(originalPosition, oldText.length()));
        int digitCount = beforeCaret.replaceAll("[^\\d]", "").length();
        
        // Find position in new text with same number of digits
        int newPosition = 0;
        int digitsFound = 0;
        
        for (int i = 0; i < newText.length() && digitsFound < digitCount; i++) {
            if (Character.isDigit(newText.charAt(i))) {
                digitsFound++;
            }
            newPosition = i + 1;
        }
        
        // Adjust for formatting characters
        if (newPosition < newText.length()) {
            char nextChar = newText.charAt(newPosition);
            if (nextChar == ')' || nextChar == ' ' || nextChar == '-') {
                newPosition++;
                if (newPosition < newText.length() - 1 && newText.charAt(newPosition) == ' ') {
                    newPosition++;
                }
            }
        }
        
        return newPosition;
    }
    
    /**
     * Get unformatted phone number (digits only)
     */
    public static String getUnformattedPhone(TextField phoneField) {
        String text = phoneField.getText();
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("[^\\d]", "");
    }
    
    /**
     * Set phone number from unformatted string
     */
    public static void setPhoneNumber(TextField phoneField, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            phoneField.setText("");
            return;
        }
        
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        phoneField.setText(formatPhoneNumber(digitsOnly));
    }
    
    /**
     * Validate if a phone field has a valid US phone number
     */
    public static boolean isValidPhone(TextField phoneField) {
        String digits = getUnformattedPhone(phoneField);
        return digits.length() == 10;
    }
}