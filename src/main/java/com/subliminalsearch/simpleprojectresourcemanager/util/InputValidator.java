package com.subliminalsearch.simpleprojectresourcemanager.util;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.css.PseudoClass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Comprehensive input validation utility for JavaFX forms
 */
public class InputValidator {
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+?1[-.]?)?(\\([0-9]{3}\\)|[0-9]{3})[-.]?[0-9]{3}[-.]?[0-9]{4}$"
    );
    
    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile(
        "^[A-Za-z0-9][A-Za-z0-9-_]*$"
    );
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^[A-Za-z][A-Za-z\\s\\-']*$"
    );
    
    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
        "^\\$?[0-9]{1,3}(,[0-9]{3})*(\\.\\d{0,2})?$|^\\$?[0-9]+(\\.\\d{0,2})?$"
    );
    
    // Date formats to try
    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ISO_LOCAL_DATE
    };
    
    // CSS pseudo-class for validation errors
    private static final PseudoClass ERROR_CLASS = PseudoClass.getPseudoClass("error");
    
    /**
     * Validates an email address
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ValidationResult(false, "Email is required");
        }
        
        email = email.trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new ValidationResult(false, "Invalid email format (e.g., user@example.com)");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validates a phone number (US format)
     */
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return new ValidationResult(true, null); // Phone is often optional
        }
        
        phone = phone.trim();
        // Remove common formatting characters for validation
        String cleanPhone = phone.replaceAll("[\\s\\(\\)\\-\\.]", "");
        
        if (cleanPhone.length() < 10 || cleanPhone.length() > 11) {
            return new ValidationResult(false, "Phone must be 10 digits (11 with country code)");
        }
        
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return new ValidationResult(false, "Invalid phone format (e.g., (555) 123-4567)");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validates a project ID
     */
    public static ValidationResult validateProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return new ValidationResult(false, "Project ID is required");
        }
        
        projectId = projectId.trim();
        if (projectId.length() < 3) {
            return new ValidationResult(false, "Project ID must be at least 3 characters");
        }
        
        if (projectId.length() > 50) {
            return new ValidationResult(false, "Project ID must be less than 50 characters");
        }
        
        if (!PROJECT_ID_PATTERN.matcher(projectId).matches()) {
            return new ValidationResult(false, "Project ID can only contain letters, numbers, hyphens, and underscores");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validates a name field
     */
    public static ValidationResult validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            return new ValidationResult(false, fieldName + " is required");
        }
        
        name = name.trim();
        if (name.length() < 2) {
            return new ValidationResult(false, fieldName + " must be at least 2 characters");
        }
        
        if (name.length() > 100) {
            return new ValidationResult(false, fieldName + " must be less than 100 characters");
        }
        
        if (!NAME_PATTERN.matcher(name).matches()) {
            return new ValidationResult(false, fieldName + " can only contain letters, spaces, hyphens, and apostrophes");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validates date range
     */
    public static ValidationResult validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return new ValidationResult(false, "Start date is required");
        }
        
        if (endDate == null) {
            return new ValidationResult(false, "End date is required");
        }
        
        if (startDate.isAfter(endDate)) {
            return new ValidationResult(false, "Start date must be before or equal to end date");
        }
        
        // Check for dates too far in the past (more than 2 years)
        if (startDate.isBefore(LocalDate.now().minusYears(2))) {
            return new ValidationResult(false, "Start date cannot be more than 2 years in the past");
        }
        
        // Check for dates too far in the future (more than 5 years)
        if (endDate.isAfter(LocalDate.now().plusYears(5))) {
            return new ValidationResult(false, "End date cannot be more than 5 years in the future");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validates currency/money input
     */
    public static ValidationResult validateCurrency(String amount, String fieldName) {
        if (amount == null || amount.trim().isEmpty()) {
            return new ValidationResult(true, null); // Amount often optional
        }
        
        amount = amount.trim();
        if (!CURRENCY_PATTERN.matcher(amount).matches()) {
            return new ValidationResult(false, fieldName + " must be a valid currency amount");
        }
        
        // Parse to check range
        try {
            String cleanAmount = amount.replaceAll("[,$]", "");
            double value = Double.parseDouble(cleanAmount);
            
            if (value < 0) {
                return new ValidationResult(false, fieldName + " cannot be negative");
            }
            
            if (value > 999999999) {
                return new ValidationResult(false, fieldName + " exceeds maximum allowed value");
            }
        } catch (NumberFormatException e) {
            return new ValidationResult(false, fieldName + " must be a valid number");
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Applies validation to a TextField with real-time feedback
     */
    public static void addValidation(TextField field, ValidationType type, String fieldName) {
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        // Store error label as user data
        field.setUserData(errorLabel);
        
        // Add listener for real-time validation
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            ValidationResult result = validate(newVal, type, fieldName);
            updateFieldValidation(field, errorLabel, result);
        });
        
        // Validate on focus lost
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                ValidationResult result = validate(field.getText(), type, fieldName);
                updateFieldValidation(field, errorLabel, result);
            }
        });
    }
    
    /**
     * Adds date range validation to two DatePickers
     */
    public static void addDateRangeValidation(DatePicker startPicker, DatePicker endPicker) {
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        
        // Store error label in end picker's user data
        endPicker.setUserData(errorLabel);
        
        Runnable validateDates = () -> {
            ValidationResult result = validateDateRange(startPicker.getValue(), endPicker.getValue());
            updateDatePickerValidation(startPicker, endPicker, errorLabel, result);
        };
        
        startPicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates.run());
        endPicker.valueProperty().addListener((obs, oldVal, newVal) -> validateDates.run());
        
        // Add custom date parser to handle multiple formats
        addFlexibleDateParser(startPicker);
        addFlexibleDateParser(endPicker);
    }
    
    /**
     * Adds flexible date parsing to handle multiple formats
     */
    private static void addFlexibleDateParser(DatePicker picker) {
        picker.getEditor().focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String text = picker.getEditor().getText();
                if (text != null && !text.trim().isEmpty()) {
                    LocalDate date = parseDate(text);
                    if (date != null) {
                        picker.setValue(date);
                        picker.getEditor().setText(date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
                    } else {
                        // Show error
                        picker.pseudoClassStateChanged(ERROR_CLASS, true);
                        Tooltip errorTooltip = new Tooltip("Invalid date format. Use MM/DD/YYYY");
                        errorTooltip.setStyle("-fx-background-color: #f44336;");
                        Tooltip.install(picker, errorTooltip);
                    }
                }
            }
        });
    }
    
    /**
     * Tries to parse a date string using multiple formats
     */
    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        dateStr = dateStr.trim();
        
        // Try each format
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        // Try to be smart about common formats
        // Handle 8/15/2025 or 08/15/2025
        if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] parts = dateStr.split("/");
            try {
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Invalid date
            }
        }
        
        return null;
    }
    
    /**
     * Validates a field based on type
     */
    private static ValidationResult validate(String value, ValidationType type, String fieldName) {
        switch (type) {
            case EMAIL:
                return validateEmail(value);
            case PHONE:
                return validatePhone(value);
            case PROJECT_ID:
                return validateProjectId(value);
            case NAME:
                return validateName(value, fieldName);
            case CURRENCY:
                return validateCurrency(value, fieldName);
            case REQUIRED:
                if (value == null || value.trim().isEmpty()) {
                    return new ValidationResult(false, fieldName + " is required");
                }
                return new ValidationResult(true, null);
            default:
                return new ValidationResult(true, null);
        }
    }
    
    /**
     * Updates field validation styling
     */
    private static void updateFieldValidation(TextField field, Label errorLabel, ValidationResult result) {
        field.pseudoClassStateChanged(ERROR_CLASS, !result.isValid());
        
        if (!result.isValid() && result.getErrorMessage() != null) {
            errorLabel.setText(result.getErrorMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            
            // Add red border
            field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
        } else {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            field.setStyle("");
        }
    }
    
    /**
     * Updates date picker validation styling
     */
    private static void updateDatePickerValidation(DatePicker start, DatePicker end, Label errorLabel, ValidationResult result) {
        start.pseudoClassStateChanged(ERROR_CLASS, !result.isValid());
        end.pseudoClassStateChanged(ERROR_CLASS, !result.isValid());
        
        if (!result.isValid() && result.getErrorMessage() != null) {
            errorLabel.setText(result.getErrorMessage());
            errorLabel.setVisible(true);
            
            // Add red border to both pickers
            start.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
            end.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 1px;");
        } else {
            errorLabel.setVisible(false);
            start.setStyle("");
            end.setStyle("");
        }
    }
    
    /**
     * Validates all fields in a form
     */
    public static boolean validateForm(ValidationField... fields) {
        List<String> errors = new ArrayList<>();
        boolean isValid = true;
        
        for (ValidationField field : fields) {
            ValidationResult result = field.validate();
            if (!result.isValid()) {
                errors.add(result.getErrorMessage());
                isValid = false;
                
                // Highlight the field
                if (field.getControl() instanceof TextField) {
                    ((TextField) field.getControl()).pseudoClassStateChanged(ERROR_CLASS, true);
                } else if (field.getControl() instanceof DatePicker) {
                    ((DatePicker) field.getControl()).pseudoClassStateChanged(ERROR_CLASS, true);
                }
            }
        }
        
        return isValid;
    }
    
    /**
     * Creates an error summary dialog
     */
    public static void showValidationErrors(List<String> errors) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Errors");
        alert.setHeaderText("Please correct the following errors:");
        
        VBox content = new VBox(5);
        content.setPadding(new Insets(10));
        
        for (String error : errors) {
            Label errorLabel = new Label("• " + error);
            errorLabel.setWrapText(true);
            content.getChildren().add(errorLabel);
        }
        
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Validation types
     */
    public enum ValidationType {
        EMAIL,
        PHONE,
        PROJECT_ID,
        NAME,
        CURRENCY,
        REQUIRED,
        CUSTOM
    }
    
    /**
     * Validation field wrapper
     */
    public static class ValidationField {
        private final Control control;
        private final ValidationType type;
        private final String fieldName;
        
        public ValidationField(Control control, ValidationType type, String fieldName) {
            this.control = control;
            this.type = type;
            this.fieldName = fieldName;
        }
        
        public Control getControl() {
            return control;
        }
        
        public ValidationResult validate() {
            if (control instanceof TextField) {
                return InputValidator.validate(((TextField) control).getText(), type, fieldName);
            }
            return new ValidationResult(true, null);
        }
    }
}