package com.subliminalsearch.simpleprojectresourcemanager.ui.framework;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.TextInputControlMatchers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

/**
 * Base framework for UI testing with common validation and interaction patterns
 */
public abstract class UITestFramework extends ApplicationTest {
    
    protected FxRobot robot = new FxRobot();
    
    // ============== Input Validation Methods ==============
    
    /**
     * Test email validation on a text field
     */
    protected void testEmailValidation(String fieldId, String submitButtonId) {
        // Test invalid emails
        String[] invalidEmails = {
            "notanemail",
            "@example.com",
            "user@",
            "user@.com",
            "user space@example.com",
            "user@example",
            "user@@example.com",
            "user.example.com"
        };
        
        for (String invalidEmail : invalidEmails) {
            clearAndType(fieldId, invalidEmail);
            clickOn(submitButtonId);
            
            // Verify error is shown
            verifyValidationError(fieldId, "Invalid email format");
            
            // Verify form is not submitted
            verifyFormNotSubmitted();
        }
        
        // Test valid emails
        String[] validEmails = {
            "user@example.com",
            "john.doe@company.org",
            "test+tag@domain.co.uk",
            "123@numbers.net"
        };
        
        for (String validEmail : validEmails) {
            clearAndType(fieldId, validEmail);
            
            // Verify no error for valid email
            verifyNoValidationError(fieldId);
        }
    }
    
    /**
     * Test phone number validation
     */
    protected void testPhoneValidation(String fieldId, String submitButtonId) {
        // Test invalid phone numbers
        String[] invalidPhones = {
            "123",                    // Too short
            "abcdefghij",            // Letters
            "123-456-789",           // Wrong format
            "12345678901234567890"   // Too long
        };
        
        for (String invalidPhone : invalidPhones) {
            clearAndType(fieldId, invalidPhone);
            clickOn(submitButtonId);
            
            verifyValidationError(fieldId, "Invalid phone number");
            verifyFormNotSubmitted();
        }
        
        // Test valid phone numbers (US formats)
        String[] validPhones = {
            "(555) 123-4567",
            "555-123-4567",
            "5551234567",
            "+1-555-123-4567",
            "555.123.4567"
        };
        
        for (String validPhone : validPhones) {
            clearAndType(fieldId, validPhone);
            verifyNoValidationError(fieldId);
        }
    }
    
    /**
     * Test date picker validation
     */
    protected void testDateValidation(String startDateId, String endDateId, String submitButtonId) {
        // Test end date before start date
        setDate(startDateId, LocalDate.now().plusDays(10));
        setDate(endDateId, LocalDate.now().plusDays(5));
        clickOn(submitButtonId);
        
        verifyValidationError(endDateId, "End date must be after start date");
        verifyFormNotSubmitted();
        
        // Test dates too far in the past
        setDate(startDateId, LocalDate.now().minusYears(5));
        clickOn(submitButtonId);
        
        verifyValidationError(startDateId, "Date cannot be more than 2 years in the past");
        verifyFormNotSubmitted();
        
        // Test dates too far in the future
        setDate(startDateId, LocalDate.now().plusYears(5));
        clickOn(submitButtonId);
        
        verifyValidationError(startDateId, "Date cannot be more than 3 years in the future");
        verifyFormNotSubmitted();
        
        // Test valid date range
        setDate(startDateId, LocalDate.now().plusDays(1));
        setDate(endDateId, LocalDate.now().plusDays(10));
        
        verifyNoValidationError(startDateId);
        verifyNoValidationError(endDateId);
    }
    
    /**
     * Test required field validation
     */
    protected void testRequiredFields(String[] requiredFieldIds, String submitButtonId) {
        // Clear all fields
        for (String fieldId : requiredFieldIds) {
            clearField(fieldId);
        }
        
        // Try to submit with empty required fields
        clickOn(submitButtonId);
        
        // Verify all required fields show error
        for (String fieldId : requiredFieldIds) {
            verifyValidationError(fieldId, "This field is required");
        }
        
        verifyFormNotSubmitted();
    }
    
    /**
     * Test numeric field validation
     */
    protected void testNumericValidation(String fieldId, double min, double max, String submitButtonId) {
        // Test non-numeric input
        clearAndType(fieldId, "abc");
        clickOn(submitButtonId);
        verifyValidationError(fieldId, "Must be a valid number");
        
        // Test below minimum
        clearAndType(fieldId, String.valueOf(min - 1));
        clickOn(submitButtonId);
        verifyValidationError(fieldId, "Value must be at least " + min);
        
        // Test above maximum
        clearAndType(fieldId, String.valueOf(max + 1));
        clickOn(submitButtonId);
        verifyValidationError(fieldId, "Value must be at most " + max);
        
        // Test valid values
        clearAndType(fieldId, String.valueOf((min + max) / 2));
        verifyNoValidationError(fieldId);
    }
    
    /**
     * Test text length validation
     */
    protected void testTextLengthValidation(String fieldId, int minLength, int maxLength, String submitButtonId) {
        // Test too short
        if (minLength > 0) {
            clearAndType(fieldId, "a".repeat(minLength - 1));
            clickOn(submitButtonId);
            verifyValidationError(fieldId, "Must be at least " + minLength + " characters");
        }
        
        // Test too long
        clearAndType(fieldId, "a".repeat(maxLength + 1));
        clickOn(submitButtonId);
        verifyValidationError(fieldId, "Must be at most " + maxLength + " characters");
        
        // Test valid length
        clearAndType(fieldId, "a".repeat(minLength + 1));
        verifyNoValidationError(fieldId);
    }
    
    // ============== Helper Methods ==============
    
    protected void clearAndType(String fieldId, String text) {
        clickOn(fieldId);
        push(KeyCode.CONTROL, KeyCode.A);
        push(KeyCode.DELETE);
        write(text);
    }
    
    protected void clearField(String fieldId) {
        clickOn(fieldId);
        push(KeyCode.CONTROL, KeyCode.A);
        push(KeyCode.DELETE);
    }
    
    protected void setDate(String datePickerId, LocalDate date) {
        DatePicker picker = lookup(datePickerId).queryAs(DatePicker.class);
        interact(() -> picker.setValue(date));
    }
    
    protected void verifyValidationError(String fieldId, String expectedError) {
        // Look for error label or tooltip near the field
        Node field = lookup(fieldId).query();
        Node errorNode = lookup(".error-label").query();
        
        if (errorNode instanceof Label) {
            Label errorLabel = (Label) errorNode;
            verifyThat(errorLabel, NodeMatchers.isVisible());
            verifyThat(errorLabel.getText(), org.hamcrest.Matchers.containsString(expectedError));
        }
        
        // Also check if field has error style class
        verifyThat(field.getStyleClass(), org.hamcrest.Matchers.hasItem("error"));
    }
    
    protected void verifyNoValidationError(String fieldId) {
        Node field = lookup(fieldId).query();
        
        // Check field doesn't have error style
        verifyThat(field.getStyleClass(), org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.hasItem("error")
        ));
        
        // Check no error label is visible
        try {
            Node errorNode = lookup(".error-label").query();
            if (errorNode != null) {
                verifyThat(errorNode, NodeMatchers.isInvisible());
            }
        } catch (Exception e) {
            // No error label found - that's good
        }
    }
    
    protected void verifyFormNotSubmitted() {
        // Check that we're still on the same form (dialog didn't close)
        verifyThat(window(getTopModalStage()), NodeMatchers.isVisible());
    }
    
    protected void verifyFormSubmitted() {
        // Check that dialog closed or success message shown
        try {
            verifyThat(window(getTopModalStage()), NodeMatchers.isInvisible());
        } catch (Exception e) {
            // Or look for success message
            verifyThat(".success-message", NodeMatchers.isVisible());
        }
    }
    
    /**
     * Test that field accepts special characters appropriately
     */
    protected void testSpecialCharacterHandling(String fieldId, boolean allowSpecialChars) {
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        clearAndType(fieldId, specialChars);
        
        if (allowSpecialChars) {
            verifyNoValidationError(fieldId);
        } else {
            verifyValidationError(fieldId, "Special characters not allowed");
        }
    }
    
    /**
     * Test copy-paste functionality
     */
    protected void testCopyPaste(String fieldId, String testValue) {
        // Type value
        clearAndType(fieldId, testValue);
        
        // Select all and copy
        clickOn(fieldId);
        push(KeyCode.CONTROL, KeyCode.A);
        push(KeyCode.CONTROL, KeyCode.C);
        
        // Clear field
        push(KeyCode.DELETE);
        
        // Paste
        push(KeyCode.CONTROL, KeyCode.V);
        
        // Verify value was pasted
        TextField field = lookup(fieldId).queryAs(TextField.class);
        verifyThat(field, TextInputControlMatchers.hasText(testValue));
    }
    
    /**
     * Test field tab order
     */
    protected void testTabOrder(String... fieldIds) {
        // Start at first field
        clickOn(fieldIds[0]);
        
        for (int i = 1; i < fieldIds.length; i++) {
            // Press tab
            push(KeyCode.TAB);
            
            // Verify focus moved to next field
            Node expectedField = lookup(fieldIds[i]).query();
            verifyThat(expectedField, NodeMatchers.isFocused());
        }
    }
    
    /**
     * Test field tooltips
     */
    protected void testTooltips(String fieldId, String expectedTooltip) {
        // Hover over field
        moveTo(fieldId);
        
        // Wait for tooltip to appear
        sleep(1, TimeUnit.SECONDS);
        
        // Verify tooltip text
        Tooltip tooltip = lookup(".tooltip").queryAs(Tooltip.class);
        if (tooltip != null) {
            verifyThat(tooltip.getText(), org.hamcrest.Matchers.containsString(expectedTooltip));
        }
    }
    
    /**
     * Test undo/redo functionality
     */
    protected void testUndoRedo(String fieldId) {
        String original = "Original";
        String modified = "Modified";
        
        clearAndType(fieldId, original);
        clearAndType(fieldId, modified);
        
        // Undo
        push(KeyCode.CONTROL, KeyCode.Z);
        TextField field = lookup(fieldId).queryAs(TextField.class);
        verifyThat(field, TextInputControlMatchers.hasText(original));
        
        // Redo
        push(KeyCode.CONTROL, KeyCode.Y);
        verifyThat(field, TextInputControlMatchers.hasText(modified));
    }
}