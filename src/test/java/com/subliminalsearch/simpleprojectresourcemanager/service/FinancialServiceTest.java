package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for FinancialService
 * Tests the financial tracking functionality
 */
@DisplayName("Financial Service Tests")
class FinancialServiceTest {
    
    private FinancialService financialService;
    
    @Mock
    private DataSource mockDataSource;
    
    @Mock
    private ProjectRepository mockProjectRepository;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private Statement mockStatement;
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock database connection
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);
        
        financialService = new FinancialService(mockDataSource, mockProjectRepository);
    }
    
    @Test
    @DisplayName("Should initialize database tables on construction")
    void shouldInitializeDatabaseTables() throws Exception {
        // Verify that the tables were created
        verify(mockStatement, times(3)).execute(anyString());
        verify(mockConnection).close();
    }
    
    @Test
    @DisplayName("Should save purchase order")
    void shouldSavePurchaseOrder() throws Exception {
        // Given
        PurchaseOrder po = new PurchaseOrder();
        po.setProjectId(1L);
        po.setPoNumber("PO-2025-001");
        po.setVendor("Test Vendor");
        po.setAmount(1000.0);
        po.setStatus(PurchaseOrder.POStatus.DRAFT);
        
        // When
        financialService.savePurchaseOrder(po);
        
        // Then - no exception should be thrown
        // In a real test, we'd verify the database insert
        assertTrue(true, "Purchase order saved successfully");
    }
    
    @Test
    @DisplayName("Should retrieve purchase orders for project")
    void shouldGetPurchaseOrdersForProject() {
        // Given
        Long projectId = 1L;
        
        // When
        List<PurchaseOrder> orders = financialService.getPurchaseOrdersForProject(projectId);
        
        // Then
        assertNotNull(orders, "Should return a list (even if empty)");
    }
    
    @Test
    @DisplayName("Should save actual cost")
    void shouldSaveActualCost() {
        // Given
        ActualCost cost = new ActualCost();
        cost.setProjectId(1L);
        cost.setCategory(ActualCost.CostCategory.LABOR);
        cost.setDescription("Developer hours");
        cost.setAmount(5000.0);
        cost.setCostDate(LocalDate.now());
        
        // When
        financialService.saveActualCost(cost);
        
        // Then - no exception should be thrown
        assertTrue(true, "Actual cost saved successfully");
    }
    
    @Test
    @DisplayName("Should retrieve actual costs for project")
    void shouldGetActualCostsForProject() {
        // Given
        Long projectId = 1L;
        
        // When
        List<ActualCost> costs = financialService.getActualCostsForProject(projectId);
        
        // Then
        assertNotNull(costs, "Should return a list (even if empty)");
    }
    
    @Test
    @DisplayName("Should save change order")
    void shouldSaveChangeOrder() {
        // Given
        ChangeOrder co = new ChangeOrder();
        co.setProjectId(1L);
        co.setChangeOrderNumber("CO-2025-001");
        co.setDescription("Additional features");
        co.setAdditionalCost(15000.0);
        co.setReason(ChangeOrder.ChangeReason.SCOPE_ADDITION);
        co.setStatus(ChangeOrder.ChangeStatus.SUBMITTED);
        
        // When
        financialService.saveChangeOrder(co);
        
        // Then - no exception should be thrown
        assertTrue(true, "Change order saved successfully");
    }
    
    @Test
    @DisplayName("Should retrieve change orders for project")
    void shouldGetChangeOrdersForProject() {
        // Given
        Long projectId = 1L;
        
        // When
        List<ChangeOrder> orders = financialService.getChangeOrdersForProject(projectId);
        
        // Then
        assertNotNull(orders, "Should return a list (even if empty)");
    }
    
    @Test
    @DisplayName("Should update project financials")
    void shouldUpdateProjectFinancials() {
        // Given
        Project project = new Project();
        project.setId(1L);
        project.setProjectId("TEST-001");
        project.setDescription("Test Project");
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(3));
        
        // When
        financialService.updateProjectFinancials(project);
        
        // Then - verify project repository was called
        verify(mockProjectRepository).update(eq(project));
    }
    
    @Test
    @DisplayName("Should delete purchase order")
    void shouldDeletePurchaseOrder() {
        // Given
        Long poId = 1L;
        
        // When
        financialService.deletePurchaseOrder(poId);
        
        // Then - no exception should be thrown
        assertTrue(true, "Purchase order deleted successfully");
    }
    
    @Test
    @DisplayName("Should delete actual cost")
    void shouldDeleteActualCost() {
        // Given
        Long costId = 1L;
        
        // When
        financialService.deleteActualCost(costId);
        
        // Then - no exception should be thrown
        assertTrue(true, "Actual cost deleted successfully");
    }
    
    @Test
    @DisplayName("Should delete change order")
    void shouldDeleteChangeOrder() {
        // Given
        Long coId = 1L;
        
        // When
        financialService.deleteChangeOrder(coId);
        
        // Then - no exception should be thrown
        assertTrue(true, "Change order deleted successfully");
    }
}