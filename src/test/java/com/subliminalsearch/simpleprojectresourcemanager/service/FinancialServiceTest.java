package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Financial Service Tests - Core Budget and Purchase Order Management")
class FinancialServiceTest {
    
    private FinancialService financialService;
    
    @Mock
    private Connection mockConnection;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        financialService = new FinancialService();
    }
    
    @Nested
    @DisplayName("Purchase Order Workflow")
    class PurchaseOrderWorkflow {
        
        @Test
        @DisplayName("Should create new PO in DRAFT status")
        void shouldCreateNewPurchaseOrderInDraftStatus() {
            PurchaseOrder po = new PurchaseOrder();
            po.setProjectId(1);
            po.setVendor("Test Vendor");
            po.setAmount(new BigDecimal("10000.00"));
            po.setStatus(PurchaseOrder.Status.DRAFT);
            
            PurchaseOrder created = financialService.createPurchaseOrder(po);
            
            assertThat(created).isNotNull();
            assertThat(created.getStatus()).isEqualTo(PurchaseOrder.Status.DRAFT);
            assertThat(created.getCreatedDate()).isNotNull();
        }
        
        @Test
        @DisplayName("Should enforce approval workflow: Draft → Pending → Approved")
        void shouldEnforceApprovalWorkflow() {
            PurchaseOrder po = createTestPurchaseOrder();
            
            assertThat(po.getStatus()).isEqualTo(PurchaseOrder.Status.DRAFT);
            
            financialService.submitForApproval(po);
            assertThat(po.getStatus()).isEqualTo(PurchaseOrder.Status.PENDING);
            
            financialService.approvePurchaseOrder(po);
            assertThat(po.getStatus()).isEqualTo(PurchaseOrder.Status.APPROVED);
        }
        
        @Test
        @DisplayName("Should prevent editing of non-DRAFT purchase orders")
        void shouldPreventEditingOfNonDraftPurchaseOrders() {
            PurchaseOrder po = createTestPurchaseOrder();
            po.setStatus(PurchaseOrder.Status.APPROVED);
            
            assertThatThrownBy(() -> financialService.updatePurchaseOrder(po))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only DRAFT purchase orders can be edited");
        }
        
        @Test
        @DisplayName("Should not allow skipping approval steps")
        void shouldNotAllowSkippingApprovalSteps() {
            PurchaseOrder po = createTestPurchaseOrder();
            po.setStatus(PurchaseOrder.Status.DRAFT);
            
            assertThatThrownBy(() -> {
                po.setStatus(PurchaseOrder.Status.APPROVED);
                financialService.updatePurchaseOrderStatus(po);
            })
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid status transition from DRAFT to APPROVED");
        }
        
        @Test
        @DisplayName("Should calculate total committed amount for project")
        void shouldCalculateTotalCommittedAmountForProject() {
            int projectId = 1;
            
            createAndSavePurchaseOrder(projectId, new BigDecimal("5000"), PurchaseOrder.Status.APPROVED);
            createAndSavePurchaseOrder(projectId, new BigDecimal("3000"), PurchaseOrder.Status.APPROVED);
            createAndSavePurchaseOrder(projectId, new BigDecimal("2000"), PurchaseOrder.Status.DRAFT);
            
            BigDecimal committed = financialService.getTotalCommittedAmount(projectId);
            
            assertThat(committed).isEqualByComparingTo(new BigDecimal("8000"));
        }
    }
    
    @Nested
    @DisplayName("Budget Variance Analysis")
    class BudgetVarianceAnalysis {
        
        @Test
        @DisplayName("Should accurately calculate budget vs actual variance")
        void shouldCalculateBudgetVariance() {
            Project project = createTestProject();
            project.setBudget(new BigDecimal("100000"));
            
            addActualCost(project.getId(), new BigDecimal("45000"), "Labor");
            addActualCost(project.getId(), new BigDecimal("25000"), "Materials");
            
            BudgetVariance variance = financialService.calculateBudgetVariance(project);
            
            assertThat(variance.getBudget()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(variance.getActual()).isEqualByComparingTo(new BigDecimal("70000"));
            assertThat(variance.getVariance()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(variance.getVariancePercentage()).isEqualByComparingTo(new BigDecimal("30.0"));
        }
        
        @Test
        @DisplayName("Should warn when 85% of budget is consumed")
        void shouldWarnWhenBudgetThresholdReached() {
            Project project = createTestProject();
            project.setBudget(new BigDecimal("100000"));
            
            addActualCost(project.getId(), new BigDecimal("86000"), "Various");
            
            BudgetStatus status = financialService.getBudgetStatus(project);
            
            assertThat(status.getWarningLevel()).isEqualTo(BudgetStatus.WarningLevel.HIGH);
            assertThat(status.getMessage()).contains("86% of budget consumed");
        }
        
        @Test
        @DisplayName("Should alert when over budget")
        void shouldAlertWhenOverBudget() {
            Project project = createTestProject();
            project.setBudget(new BigDecimal("100000"));
            
            addActualCost(project.getId(), new BigDecimal("105000"), "Various");
            
            BudgetStatus status = financialService.getBudgetStatus(project);
            
            assertThat(status.getWarningLevel()).isEqualTo(BudgetStatus.WarningLevel.CRITICAL);
            assertThat(status.getMessage()).contains("Project is over budget");
        }
        
        @Test
        @DisplayName("Should track variance by cost category")
        void shouldTrackVarianceByCostCategory() {
            Project project = createTestProject();
            
            addActualCost(project.getId(), new BigDecimal("30000"), "Labor");
            addActualCost(project.getId(), new BigDecimal("20000"), "Materials");
            addActualCost(project.getId(), new BigDecimal("10000"), "Equipment");
            
            var categoryBreakdown = financialService.getCostBreakdownByCategory(project.getId());
            
            assertThat(categoryBreakdown).hasSize(3);
            assertThat(categoryBreakdown.get("Labor")).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(categoryBreakdown.get("Materials")).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(categoryBreakdown.get("Equipment")).isEqualByComparingTo(new BigDecimal("10000"));
        }
    }
    
    @Nested
    @DisplayName("Change Order Management")
    class ChangeOrderManagement {
        
        @Test
        @DisplayName("Should increase project budget when change order is approved")
        void shouldIncreaseProjectBudgetWhenChangeOrderApproved() {
            Project project = createTestProject();
            project.setBudget(new BigDecimal("100000"));
            
            ChangeOrder changeOrder = new ChangeOrder();
            changeOrder.setProjectId(project.getId());
            changeOrder.setAmount(new BigDecimal("15000"));
            changeOrder.setStatus(ChangeOrder.Status.PENDING);
            changeOrder.setReason("Additional scope requested by client");
            
            financialService.approveChangeOrder(changeOrder);
            
            BigDecimal newBudget = financialService.getProjectBudgetWithChangeOrders(project.getId());
            assertThat(newBudget).isEqualByComparingTo(new BigDecimal("115000"));
        }
        
        @Test
        @DisplayName("Should require approval for change orders over $10,000")
        void shouldRequireApprovalForLargeChangeOrders() {
            ChangeOrder changeOrder = new ChangeOrder();
            changeOrder.setAmount(new BigDecimal("15000"));
            
            boolean requiresApproval = financialService.requiresApproval(changeOrder);
            
            assertThat(requiresApproval).isTrue();
        }
        
        @Test
        @DisplayName("Should maintain history of all change orders")
        void shouldMaintainChangeOrderHistory() {
            int projectId = 1;
            
            createAndSaveChangeOrder(projectId, new BigDecimal("5000"), "Change 1");
            createAndSaveChangeOrder(projectId, new BigDecimal("8000"), "Change 2");
            createAndSaveChangeOrder(projectId, new BigDecimal("3000"), "Change 3");
            
            List<ChangeOrder> history = financialService.getChangeOrderHistory(projectId);
            
            assertThat(history).hasSize(3);
            assertThat(history).extracting("reason")
                .containsExactly("Change 1", "Change 2", "Change 3");
        }
        
        @Test
        @DisplayName("Should show cumulative impact of change orders on project")
        void shouldShowCumulativeImpactOfChangeOrders() {
            Project project = createTestProject();
            project.setBudget(new BigDecimal("100000"));
            
            createAndSaveChangeOrder(project.getId(), new BigDecimal("5000"), "Change 1");
            createAndSaveChangeOrder(project.getId(), new BigDecimal("8000"), "Change 2");
            
            ChangeOrderSummary summary = financialService.getChangeOrderSummary(project.getId());
            
            assertThat(summary.getOriginalBudget()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(summary.getTotalChangeOrders()).isEqualByComparingTo(new BigDecimal("13000"));
            assertThat(summary.getRevisedBudget()).isEqualByComparingTo(new BigDecimal("113000"));
            assertThat(summary.getPercentageIncrease()).isEqualByComparingTo(new BigDecimal("13.0"));
        }
    }
    
    @Nested
    @DisplayName("Actual Cost Tracking")
    class ActualCostTracking {
        
        @Test
        @DisplayName("Should link actual costs to purchase orders")
        void shouldLinkActualCostsToPurchaseOrders() {
            PurchaseOrder po = createTestPurchaseOrder();
            po.setAmount(new BigDecimal("10000"));
            financialService.createPurchaseOrder(po);
            
            ActualCost cost = new ActualCost();
            cost.setPurchaseOrderId(po.getId());
            cost.setAmount(new BigDecimal("9500"));
            cost.setDescription("Invoice #12345");
            
            financialService.addActualCost(cost);
            
            assertThat(cost.getPurchaseOrderId()).isEqualTo(po.getId());
            assertThat(financialService.getActualCostsForPO(po.getId())).hasSize(1);
        }
        
        @Test
        @DisplayName("Should prevent actual costs exceeding PO amount without approval")
        void shouldPreventCostsExceedingPOAmount() {
            PurchaseOrder po = createTestPurchaseOrder();
            po.setAmount(new BigDecimal("10000"));
            financialService.createPurchaseOrder(po);
            
            ActualCost cost = new ActualCost();
            cost.setPurchaseOrderId(po.getId());
            cost.setAmount(new BigDecimal("11000"));
            
            assertThatThrownBy(() -> financialService.addActualCost(cost))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Actual cost exceeds PO amount. Change order required.");
        }
        
        @Test
        @DisplayName("Should categorize costs correctly")
        void shouldCategorizeCostsCorrectly() {
            int projectId = 1;
            
            addActualCost(projectId, new BigDecimal("5000"), "Labor");
            addActualCost(projectId, new BigDecimal("3000"), "Materials");
            addActualCost(projectId, new BigDecimal("2000"), "Labor");
            
            var breakdown = financialService.getCostBreakdownByCategory(projectId);
            
            assertThat(breakdown.get("Labor")).isEqualByComparingTo(new BigDecimal("7000"));
            assertThat(breakdown.get("Materials")).isEqualByComparingTo(new BigDecimal("3000"));
        }
    }
    
    // Helper methods
    private PurchaseOrder createTestPurchaseOrder() {
        PurchaseOrder po = new PurchaseOrder();
        po.setProjectId(1);
        po.setVendor("Test Vendor");
        po.setAmount(new BigDecimal("10000"));
        po.setStatus(PurchaseOrder.Status.DRAFT);
        po.setCreatedDate(LocalDate.now());
        return po;
    }
    
    private Project createTestProject() {
        Project project = new Project();
        project.setId(1);
        project.setName("Test Project");
        project.setBudget(new BigDecimal("100000"));
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(3));
        return project;
    }
    
    private void createAndSavePurchaseOrder(int projectId, BigDecimal amount, PurchaseOrder.Status status) {
        PurchaseOrder po = new PurchaseOrder();
        po.setProjectId(projectId);
        po.setAmount(amount);
        po.setStatus(status);
        financialService.createPurchaseOrder(po);
    }
    
    private void addActualCost(int projectId, BigDecimal amount, String category) {
        ActualCost cost = new ActualCost();
        cost.setProjectId(projectId);
        cost.setAmount(amount);
        cost.setCategory(category);
        cost.setDate(LocalDate.now());
        financialService.addActualCost(cost);
    }
    
    private void createAndSaveChangeOrder(int projectId, BigDecimal amount, String reason) {
        ChangeOrder co = new ChangeOrder();
        co.setProjectId(projectId);
        co.setAmount(amount);
        co.setReason(reason);
        co.setStatus(ChangeOrder.Status.APPROVED);
        financialService.createChangeOrder(co);
    }
}