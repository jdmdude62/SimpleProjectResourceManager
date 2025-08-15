package com.subliminalsearch.simpleprojectresourcemanager.validation;

import com.subliminalsearch.simpleprojectresourcemanager.util.InputValidator;
import com.subliminalsearch.simpleprojectresourcemanager.util.InputValidator.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Project Dialog Input Validation Tests")
public class ProjectDialogValidationTest {
    
    @Test
    @DisplayName("Test Project ID validation")
    void testProjectIdValidation() {
        // Valid project IDs
        assertTrue(InputValidator.validateProjectId("PRJ-2024-001").isValid());
        assertTrue(InputValidator.validateProjectId("PROJECT123").isValid());
        assertTrue(InputValidator.validateProjectId("Test_Project_01").isValid());
        
        // Invalid project IDs
        assertFalse(InputValidator.validateProjectId("").isValid());
        assertFalse(InputValidator.validateProjectId("PR").isValid()); // Too short
        assertFalse(InputValidator.validateProjectId("Project ID!").isValid()); // Special chars
        assertFalse(InputValidator.validateProjectId("Project@123").isValid()); // @ symbol
    }
    
    @Test
    @DisplayName("Test email validation")
    void testEmailValidation() {
        // Valid emails
        assertTrue(InputValidator.validateEmail("user@example.com").isValid());
        assertTrue(InputValidator.validateEmail("john.doe@company.org").isValid());
        assertTrue(InputValidator.validateEmail("test+tag@domain.co.uk").isValid());
        
        // Invalid emails
        assertFalse(InputValidator.validateEmail("notanemail").isValid());
        assertFalse(InputValidator.validateEmail("@example.com").isValid());
        assertFalse(InputValidator.validateEmail("user@").isValid());
        assertFalse(InputValidator.validateEmail("user space@example.com").isValid());
    }
    
    @Test
    @DisplayName("Test phone validation")
    void testPhoneValidation() {
        // Valid phone numbers (US format)
        assertTrue(InputValidator.validatePhone("(555) 123-4567").isValid());
        assertTrue(InputValidator.validatePhone("555-123-4567").isValid());
        assertTrue(InputValidator.validatePhone("5551234567").isValid());
        assertTrue(InputValidator.validatePhone("+1-555-123-4567").isValid());
        
        // Invalid phone numbers
        assertFalse(InputValidator.validatePhone("123").isValid());
        assertFalse(InputValidator.validatePhone("abcdefghij").isValid());
        assertFalse(InputValidator.validatePhone("123-456").isValid());
    }
    
    @Test
    @DisplayName("Test date range validation")
    void testDateRangeValidation() {
        LocalDate today = LocalDate.now();
        
        // Valid date ranges
        assertTrue(InputValidator.validateDateRange(
            today.plusDays(1), 
            today.plusDays(10)
        ).isValid());
        
        assertTrue(InputValidator.validateDateRange(
            today, 
            today.plusMonths(6)
        ).isValid());
        
        // Invalid date ranges
        assertFalse(InputValidator.validateDateRange(
            today.plusDays(10), 
            today.plusDays(5)
        ).isValid()); // End before start
        
        assertFalse(InputValidator.validateDateRange(
            today.minusYears(3), 
            today
        ).isValid()); // Too far in past
        
        assertFalse(InputValidator.validateDateRange(
            today, 
            today.plusYears(6)
        ).isValid()); // Too far in future
        
        assertFalse(InputValidator.validateDateRange(
            null, 
            today
        ).isValid()); // Null start date
        
        assertFalse(InputValidator.validateDateRange(
            today, 
            null
        ).isValid()); // Null end date
    }
    
    @Test
    @DisplayName("Test multiple email validation")
    void testMultipleEmailValidation() {
        // Test semicolon-separated emails
        String validEmails = "user1@example.com;user2@example.com;user3@example.com";
        String[] emails = validEmails.split(";");
        boolean allValid = true;
        
        for (String email : emails) {
            ValidationResult result = InputValidator.validateEmail(email.trim());
            if (!result.isValid()) {
                allValid = false;
                break;
            }
        }
        assertTrue(allValid);
        
        // Test with invalid email in the list
        String mixedEmails = "user1@example.com;invalid-email;user3@example.com";
        emails = mixedEmails.split(";");
        allValid = true;
        
        for (String email : emails) {
            ValidationResult result = InputValidator.validateEmail(email.trim());
            if (!result.isValid()) {
                allValid = false;
                break;
            }
        }
        assertFalse(allValid);
    }
    
    @Test
    @DisplayName("Test validation error messages")
    void testValidationErrorMessages() {
        ValidationResult result;
        
        // Project ID error messages
        result = InputValidator.validateProjectId("");
        assertFalse(result.isValid());
        assertEquals("Project ID is required", result.getErrorMessage());
        
        result = InputValidator.validateProjectId("AB");
        assertFalse(result.isValid());
        assertEquals("Project ID must be at least 3 characters", result.getErrorMessage());
        
        // Email error messages
        result = InputValidator.validateEmail("");
        assertFalse(result.isValid());
        assertEquals("Email is required", result.getErrorMessage());
        
        result = InputValidator.validateEmail("invalid");
        assertFalse(result.isValid());
        assertEquals("Invalid email format (e.g., user@example.com)", result.getErrorMessage());
        
        // Date range error messages
        LocalDate today = LocalDate.now();
        result = InputValidator.validateDateRange(today.plusDays(10), today);
        assertFalse(result.isValid());
        assertEquals("Start date must be before or equal to end date", result.getErrorMessage());
    }
}