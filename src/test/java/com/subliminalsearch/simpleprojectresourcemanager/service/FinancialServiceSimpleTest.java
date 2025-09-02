package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests for FinancialService without mocks
 * Uses in-memory database for testing
 */
@DisplayName("Financial Service Simple Tests")
class FinancialServiceSimpleTest {
    
    private FinancialService financialService;
    private DataSource dataSource;
    private ProjectRepository projectRepository;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory SQLite database
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite::memory:");
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);
        
        // Initialize tables
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create projects table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id TEXT NOT NULL,
                    description TEXT,
                    project_manager_id INTEGER,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    status TEXT DEFAULT 'PLANNING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    budget_amount REAL DEFAULT 0,
                    actual_cost REAL DEFAULT 0,
                    revenue_amount REAL DEFAULT 0,
                    contact_name TEXT,
                    contact_email TEXT,
                    contact_phone TEXT,
                    contact_company TEXT,
                    contact_role TEXT,
                    contact_address TEXT,
                    send_reports BOOLEAN DEFAULT 1,
                    report_frequency TEXT DEFAULT 'WEEKLY',
                    last_report_sent TIMESTAMP,
                    currency_code TEXT DEFAULT 'USD',
                    labor_cost REAL,
                    material_cost REAL,
                    travel_cost REAL,
                    other_cost REAL,
                    cost_notes TEXT,
                    is_travel BOOLEAN DEFAULT 0
                )
            """);
        }
        
        projectRepository = new ProjectRepository(dataSource);
        financialService = new FinancialService(dataSource, projectRepository);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }
    
    @Test
    @DisplayName("Should save and retrieve purchase order")
    void shouldSaveAndRetrievePurchaseOrder() {
        // Given
        PurchaseOrder po = new PurchaseOrder();
        po.setProjectId(1L);
        po.setPoNumber("PO-2025-001");
        po.setVendor("Test Vendor");
        po.setAmount(1000.0);
        po.setStatus(PurchaseOrder.POStatus.DRAFT);
        
        // When
        financialService.savePurchaseOrder(po);
        List<PurchaseOrder> orders = financialService.getPurchaseOrdersForProject(1L);
        
        // Then
        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals("PO-2025-001", orders.get(0).getPoNumber());
    }
    
    @Test
    @DisplayName("Should save and retrieve actual cost")
    void shouldSaveAndRetrieveActualCost() {
        // Given
        ActualCost cost = new ActualCost();
        cost.setProjectId(1L);
        cost.setCategory(ActualCost.CostCategory.LABOR);
        cost.setDescription("Developer hours");
        cost.setAmount(5000.0);
        cost.setCostDate(LocalDate.now());
        
        // When
        financialService.saveActualCost(cost);
        List<ActualCost> costs = financialService.getActualCostsForProject(1L);
        
        // Then
        assertNotNull(costs);
        assertEquals(1, costs.size());
        assertEquals(5000.0, costs.get(0).getAmount());
    }
    
    @Test
    @DisplayName("Should save and retrieve change order")
    void shouldSaveAndRetrieveChangeOrder() {
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
        List<ChangeOrder> orders = financialService.getChangeOrdersForProject(1L);
        
        // Then
        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals("CO-2025-001", orders.get(0).getChangeOrderNumber());
    }
    
    @Test
    @DisplayName("Should update project financials")
    void shouldUpdateProjectFinancials() {
        // Given
        Project project = new Project();
        project.setProjectId("TEST-001");
        project.setDescription("Test Project");
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(3));
        project.setBudgetAmount(100000.0);
        
        // Save project first
        projectRepository.save(project);
        
        // Update financials
        project.setActualCost(50000.0);
        project.setRevenueAmount(120000.0);
        
        // When
        financialService.updateProjectFinancials(project);
        
        // Then
        var retrieved = projectRepository.findById(project.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(50000.0, retrieved.get().getActualCost());
        assertEquals(120000.0, retrieved.get().getRevenueAmount());
    }
    
    @Test
    @DisplayName("Should delete purchase order")
    void shouldDeletePurchaseOrder() {
        // Given
        PurchaseOrder po = new PurchaseOrder();
        po.setProjectId(1L);
        po.setPoNumber("PO-2025-002");
        po.setVendor("Test Vendor");
        po.setAmount(2000.0);
        po.setStatus(PurchaseOrder.POStatus.DRAFT);
        
        financialService.savePurchaseOrder(po);
        List<PurchaseOrder> ordersBefore = financialService.getPurchaseOrdersForProject(1L);
        assertEquals(1, ordersBefore.size());
        
        // When
        financialService.deletePurchaseOrder(ordersBefore.get(0).getId());
        
        // Then
        List<PurchaseOrder> ordersAfter = financialService.getPurchaseOrdersForProject(1L);
        assertEquals(0, ordersAfter.size());
    }
}