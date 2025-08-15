package com.subliminalsearch.simpleprojectresourcemanager.ui;

import com.subliminalsearch.simpleprojectresourcemanager.SchedulerApplication;
import com.subliminalsearch.simpleprojectresourcemanager.ui.framework.UITestFramework;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.testfx.api.FxToolkit;

import java.util.concurrent.TimeoutException;

@DisplayName("Comprehensive UI Input Validation Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComprehensiveUIValidationTest extends UITestFramework {
    
    @Override
    public void start(Stage stage) throws Exception {
        System.setProperty("test.mode", "true");
        System.setProperty("test.db.url", "jdbc:sqlite:test_ui_validation.db");
        
        SchedulerApplication app = new SchedulerApplication();
        app.start(stage);
    }
    
    @AfterEach
    void cleanup() throws TimeoutException {
        FxToolkit.hideStage();
    }
    
    // ============== PROJECT DIALOG VALIDATION ==============
    
    @Test
    @Order(1)
    @DisplayName("Validate Project Dialog inputs")
    void testProjectDialogValidation() {
        // Open new project dialog
        clickOn("File").clickOn("New Project");
        
        // Test required fields
        testRequiredFields(
            new String[]{"#projectIdField", "#descriptionField", "#startDatePicker", "#endDatePicker"},
            "#saveProjectButton"
        );
        
        // Test project ID format (alphanumeric with dashes)
        clearAndType("#projectIdField", "Invalid Project ID!");
        clickOn("#saveProjectButton");
        verifyValidationError("#projectIdField", "Project ID can only contain letters, numbers, and dashes");
        
        clearAndType("#projectIdField", "PROJ-2024-001");
        verifyNoValidationError("#projectIdField");
        
        // Test description length
        testTextLengthValidation("#descriptionField", 3, 200, "#saveProjectButton");
        
        // Test date validation
        testDateValidation("#startDatePicker", "#endDatePicker", "#saveProjectButton");
        
        // Test budget field (numeric, positive)
        testNumericValidation("#budgetField", 0, 10000000, "#saveProjectButton");
        
        // Test location field special characters
        testSpecialCharacterHandling("#locationField", false);
        
        // Close dialog
        clickOn("#cancelButton");
    }
    
    // ============== RESOURCE DIALOG VALIDATION ==============
    
    @Test
    @Order(2)
    @DisplayName("Validate Resource Dialog inputs")
    void testResourceDialogValidation() {
        // Open new resource dialog
        clickOn("Resources").clickOn("Add Resource");
        
        // Test required fields
        testRequiredFields(
            new String[]{"#nameField", "#emailField", "#resourceTypeCombo"},
            "#saveResourceButton"
        );
        
        // Test name validation (letters, spaces, hyphens only)
        clearAndType("#nameField", "John123");
        clickOn("#saveResourceButton");
        verifyValidationError("#nameField", "Name can only contain letters, spaces, and hyphens");
        
        clearAndType("#nameField", "John Smith-Jones");
        verifyNoValidationError("#nameField");
        
        // Test email validation
        testEmailValidation("#emailField", "#saveResourceButton");
        
        // Test phone validation
        testPhoneValidation("#phoneField", "#saveResourceButton");
        
        // Test daily rate validation (positive number)
        testNumericValidation("#dailyRateField", 0, 5000, "#saveResourceButton");
        
        // Test skills field (comma-separated)
        clearAndType("#skillsField", "Java, Python, SQL");
        verifyNoValidationError("#skillsField");
        
        clearAndType("#skillsField", "Java; Python; SQL");
        verifyValidationError("#skillsField", "Use commas to separate skills");
        
        // Close dialog
        clickOn("#cancelButton");
    }
    
    // ============== ASSIGNMENT DIALOG VALIDATION ==============
    
    @Test
    @Order(3)
    @DisplayName("Validate Assignment Dialog inputs")
    void testAssignmentDialogValidation() {
        // Open assignment dialog
        clickOn("#assignmentButton");
        
        // Test required fields
        testRequiredFields(
            new String[]{"#projectSelect", "#resourceSelect", "#startDatePicker", "#endDatePicker"},
            "#saveAssignmentButton"
        );
        
        // Test date validation with project constraints
        testDateValidation("#startDatePicker", "#endDatePicker", "#saveAssignmentButton");
        
        // Test travel days validation (non-negative integers)
        testNumericValidation("#travelOutDaysField", 0, 30, "#saveAssignmentButton");
        testNumericValidation("#travelBackDaysField", 0, 30, "#saveAssignmentButton");
        
        // Test notes field length
        testTextLengthValidation("#notesField", 0, 500, "#saveAssignmentButton");
        
        // Test allocation percentage
        testNumericValidation("#allocationPercentage", 0, 100, "#saveAssignmentButton");
        
        // Close dialog
        clickOn("#cancelButton");
    }
    
    // ============== UNAVAILABILITY DIALOG VALIDATION ==============
    
    @Test
    @Order(4)
    @DisplayName("Validate Unavailability Dialog inputs")
    void testUnavailabilityDialogValidation() {
        // Open unavailability dialog
        clickOn("View").clickOn("Resource Availability");
        clickOn("#addUnavailabilityButton");
        
        // Test required fields
        testRequiredFields(
            new String[]{"#resourceSelect", "#unavailabilityType", "#startDatePicker", "#endDatePicker"},
            "#saveUnavailabilityButton"
        );
        
        // Test date validation
        testDateValidation("#startDatePicker", "#endDatePicker", "#saveUnavailabilityButton");
        
        // Test reason field
        testTextLengthValidation("#reasonField", 5, 200, "#saveUnavailabilityButton");
        
        // Test recurring pattern validation
        clickOn("#recurringCheckbox");
        clearAndType("#recurrencePattern", "INVALID");
        clickOn("#saveUnavailabilityButton");
        verifyValidationError("#recurrencePattern", "Invalid recurrence pattern");
        
        clearAndType("#recurrencePattern", "WEEKLY:MONDAY,FRIDAY");
        verifyNoValidationError("#recurrencePattern");
        
        // Close dialog
        clickOn("#cancelButton");
    }
    
    // ============== FINANCIAL TRACKING VALIDATION ==============
    
    @Test
    @Order(5)
    @DisplayName("Validate Financial Tracking inputs")
    void testFinancialTrackingValidation() {
        // Open financial tracking dialog
        clickOn("#timelineView");
        rightClickOn(".project-bar");
        clickOn("Financial Tracking");
        
        // Test Purchase Order validation
        clickOn("#purchaseOrderTab");
        
        // PO Number format
        clearAndType("#poNumberField", "PO 123");
        clickOn("#savePOButton");
        verifyValidationError("#poNumberField", "PO number must be in format PO-XXXX");
        
        clearAndType("#poNumberField", "PO-2024-001");
        verifyNoValidationError("#poNumberField");
        
        // Vendor name
        testTextLengthValidation("#vendorNameField", 2, 100, "#savePOButton");
        
        // Amount validation
        testNumericValidation("#amountField", 0.01, 1000000, "#savePOButton");
        
        // Test Actual Costs validation
        clickOn("#actualCostsTab");
        
        // Cost category required
        testRequiredFields(new String[]{"#costCategory", "#actualAmount"}, "#saveCostButton");
        
        // Invoice number format
        clearAndType("#invoiceNumberField", "INV#123!");
        clickOn("#saveCostButton");
        verifyValidationError("#invoiceNumberField", "Invalid invoice number format");
        
        // Test Change Order validation
        clickOn("#changeOrderTab");
        
        // Change order number
        clearAndType("#changeOrderNumber", "CO-001");
        verifyNoValidationError("#changeOrderNumber");
        
        // Impact amount can be negative (for cost reduction)
        testNumericValidation("#impactAmount", -100000, 100000, "#saveChangeOrderButton");
        
        // Approval chain email validation
        clearAndType("#approverEmailField", "manager@company.com, director@company.com");
        verifyNoValidationError("#approverEmailField");
        
        // Close dialog
        clickOn("#closeButton");
    }
    
    // ============== PROJECT MANAGER DIALOG VALIDATION ==============
    
    @Test
    @Order(6)
    @DisplayName("Validate Project Manager Dialog inputs")
    void testProjectManagerDialogValidation() {
        // Open project manager management
        clickOn("Admin").clickOn("Manage Project Managers");
        clickOn("#addManagerButton");
        
        // Test required fields
        testRequiredFields(
            new String[]{"#managerNameField", "#managerEmailField", "#departmentField"},
            "#saveManagerButton"
        );
        
        // Test name validation
        testTextLengthValidation("#managerNameField", 2, 100, "#saveManagerButton");
        
        // Test email validation
        testEmailValidation("#managerEmailField", "#saveManagerButton");
        
        // Test phone validation
        testPhoneValidation("#managerPhoneField", "#saveManagerButton");
        
        // Test department dropdown
        clickOn("#departmentField");
        clickOn("Engineering");
        verifyNoValidationError("#departmentField");
        
        // Test certification field
        clearAndType("#certificationField", "PMP, PMI-ACP");
        verifyNoValidationError("#certificationField");
        
        // Close dialog
        clickOn("#cancelButton");
    }
    
    // ============== REPORT PARAMETERS VALIDATION ==============
    
    @Test
    @Order(7)
    @DisplayName("Validate Report Parameters inputs")
    void testReportParametersValidation() {
        // Open reports dialog
        clickOn("Reports").clickOn("Generate Reports");
        
        // Test date range validation
        testDateValidation("#reportStartDate", "#reportEndDate", "#generateReportButton");
        
        // Test resource selection (at least one required)
        clickOn("#clearAllResources");
        clickOn("#generateReportButton");
        verifyValidationError("#resourceSelection", "Select at least one resource");
        
        // Test project selection
        clickOn("#clearAllProjects");
        clickOn("#generateReportButton");
        verifyValidationError("#projectSelection", "Select at least one project");
        
        // Test export format selection
        clickOn("#exportFormatCombo");
        clickOn("PDF");
        verifyNoValidationError("#exportFormatCombo");
        
        // Test email distribution list
        clearAndType("#distributionListField", "report@company.com, manager@company.com");
        verifyNoValidationError("#distributionListField");
        
        clearAndType("#distributionListField", "invalid-email");
        verifyValidationError("#distributionListField", "Invalid email in distribution list");
        
        // Close dialog
        clickOn("#cancelButton");
    }
    
    // ============== CROSS-FIELD VALIDATION ==============
    
    @Test
    @Order(8)
    @DisplayName("Test cross-field validation rules")
    void testCrossFieldValidation() {
        // Test assignment dates must be within project dates
        clickOn("#assignmentButton");
        
        // Select a project (assume it has specific date range)
        clickOn("#projectSelect").clickOn("TEST-PROJECT");
        
        // Try to set assignment dates outside project range
        setDate("#startDatePicker", LocalDate.now().minusDays(10));
        clickOn("#saveAssignmentButton");
        verifyValidationError("#startDatePicker", "Assignment must be within project dates");
        
        // Test resource availability conflicts
        clickOn("#resourceSelect").clickOn("John Doe");
        setDate("#startDatePicker", LocalDate.now().plusDays(5));
        setDate("#endDatePicker", LocalDate.now().plusDays(10));
        
        // If resource is already assigned or unavailable
        clickOn("#saveAssignmentButton");
        // Should show conflict warning
        verifyThat(".conflict-warning", NodeMatchers.isVisible());
        
        clickOn("#cancelButton");
    }
    
    // ============== ACCESSIBILITY VALIDATION ==============
    
    @Test
    @Order(9)
    @DisplayName("Test accessibility features")
    void testAccessibilityFeatures() {
        // Test tab order through main form
        clickOn("File").clickOn("New Project");
        
        testTabOrder(
            "#projectIdField",
            "#descriptionField", 
            "#startDatePicker",
            "#endDatePicker",
            "#budgetField",
            "#locationField",
            "#projectManagerCombo",
            "#saveProjectButton",
            "#cancelButton"
        );
        
        // Test keyboard shortcuts
        push(KeyCode.ESCAPE); // Should close dialog
        verifyThat("#projectDialog", NodeMatchers.isInvisible());
        
        // Test form submission with Enter key
        clickOn("File").clickOn("New Project");
        fillValidProjectData();
        push(KeyCode.ENTER);
        verifyFormSubmitted();
    }
    
    // ============== PERFORMANCE VALIDATION ==============
    
    @Test
    @Order(10)
    @DisplayName("Test validation performance")
    void testValidationPerformance() {
        clickOn("Resources").clickOn("Add Resource");
        
        // Measure validation response time
        long startTime = System.currentTimeMillis();
        
        // Trigger multiple validations rapidly
        for (int i = 0; i < 10; i++) {
            clearAndType("#emailField", "invalid" + i);
            clearAndType("#phoneField", "123" + i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Validation should be responsive (under 2 seconds for 20 validations)
        assertThat(duration).isLessThan(2000);
        
        clickOn("#cancelButton");
    }
    
    // Helper method
    private void fillValidProjectData() {
        clearAndType("#projectIdField", "PROJ-2024-TEST");
        clearAndType("#descriptionField", "Test Project Description");
        setDate("#startDatePicker", LocalDate.now().plusDays(1));
        setDate("#endDatePicker", LocalDate.now().plusDays(30));
        clearAndType("#budgetField", "50000");
    }
}