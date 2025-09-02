package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Centralized financial calculation utility to ensure consistency and accuracy
 * across all financial views and reports. All financial calculations should
 * go through this class to ensure auditability.
 */
public class FinancialCalculator {
    private static final Logger logger = LoggerFactory.getLogger(FinancialCalculator.class);
    
    /**
     * Financial summary data structure
     */
    public static class FinancialSummary {
        // Budget fields
        public final double totalBudget;
        public final double laborBudget;
        public final double materialBudget;
        public final double travelBudget;
        public final double otherBudget;
        
        // Actual costs
        public final double totalActualCost;
        public final double laborActual;
        public final double materialActual;
        public final double travelActual;
        public final double otherActual;
        
        // Purchase orders
        public final double totalPOAmount;
        public final double committedPOAmount;  // Approved/Ordered POs
        public final double pendingPOAmount;    // Pending approval
        
        // Change orders
        public final double approvedChangeOrders;
        public final double pendingChangeOrders;
        public final double totalChangeOrderImpact;
        
        // Calculated fields
        public final double revisedBudget;      // Original + approved change orders
        public final double totalCommitted;     // Actual costs + committed POs
        public final double projectedTotal;     // Total committed + pending
        public final double budgetVariance;     // Revised budget - projected total
        public final double budgetUtilization;  // Percentage of budget used
        public final double costPerformanceIndex; // Budget efficiency metric
        
        // Audit fields
        public final LocalDate calculatedAt;
        public final String calculationMethod;
        public final int recordCount;
        
        public FinancialSummary(
            double totalBudget, double laborBudget, double materialBudget, 
            double travelBudget, double otherBudget,
            double totalActualCost, double laborActual, double materialActual,
            double travelActual, double otherActual,
            double totalPOAmount, double committedPOAmount, double pendingPOAmount,
            double approvedChangeOrders, double pendingChangeOrders,
            int recordCount) {
            
            this.totalBudget = totalBudget;
            this.laborBudget = laborBudget;
            this.materialBudget = materialBudget;
            this.travelBudget = travelBudget;
            this.otherBudget = otherBudget;
            
            this.totalActualCost = totalActualCost;
            this.laborActual = laborActual;
            this.materialActual = materialActual;
            this.travelActual = travelActual;
            this.otherActual = otherActual;
            
            this.totalPOAmount = totalPOAmount;
            this.committedPOAmount = committedPOAmount;
            this.pendingPOAmount = pendingPOAmount;
            
            this.approvedChangeOrders = approvedChangeOrders;
            this.pendingChangeOrders = pendingChangeOrders;
            this.totalChangeOrderImpact = approvedChangeOrders + pendingChangeOrders;
            
            // Calculate derived fields
            this.revisedBudget = totalBudget + approvedChangeOrders;
            this.totalCommitted = totalActualCost + committedPOAmount;
            this.projectedTotal = totalCommitted + pendingPOAmount;
            this.budgetVariance = revisedBudget - projectedTotal;
            this.budgetUtilization = revisedBudget > 0 ? (projectedTotal / revisedBudget) * 100 : 0;
            this.costPerformanceIndex = projectedTotal > 0 ? revisedBudget / projectedTotal : 1.0;
            
            this.calculatedAt = LocalDate.now();
            this.calculationMethod = "v1.0-standard";
            this.recordCount = recordCount;
            
            // Log calculation for audit trail
            logger.info("Financial calculation completed - Project Budget: ${}, Actual: ${}, Variance: ${}, Utilization: {}%",
                String.format("%,.2f", revisedBudget),
                String.format("%,.2f", totalActualCost),
                String.format("%,.2f", budgetVariance),
                String.format("%.1f", budgetUtilization));
        }
        
        public boolean isOverBudget() {
            return budgetVariance < 0;
        }
        
        public boolean isHighRisk() {
            return budgetUtilization > 90 || budgetVariance < (revisedBudget * 0.05);
        }
        
        public String getHealthStatus() {
            if (budgetUtilization > 100) return "OVER BUDGET";
            if (budgetUtilization > 90) return "HIGH RISK";
            if (budgetUtilization > 75) return "ON TRACK";
            return "HEALTHY";
        }
    }
    
    /**
     * Calculate comprehensive financial summary for a project
     */
    public static FinancialSummary calculateProjectFinancials(
            Project project,
            List<ActualCost> actualCosts,
            List<PurchaseOrder> purchaseOrders,
            List<ChangeOrder> changeOrders) {
        
        logger.debug("Calculating financials for project: {} with {} costs, {} POs, {} change orders",
            project.getProjectId(), actualCosts.size(), purchaseOrders.size(), changeOrders.size());
        
        // Extract budget from project
        double totalBudget = safeDouble(project.getBudgetAmount());
        double laborBudget = safeDouble(project.getLaborCost());
        double materialBudget = safeDouble(project.getMaterialCost());
        double travelBudget = safeDouble(project.getTravelCost());
        double otherBudget = safeDouble(project.getOtherCost());
        
        // Calculate actual costs by category
        Map<ActualCost.CostCategory, Double> actualByCategory = actualCosts.stream()
            .filter(c -> c.getStatus() != ActualCost.CostStatus.DISPUTED)
            .collect(Collectors.groupingBy(
                c -> c.getCategory() != null ? c.getCategory() : ActualCost.CostCategory.OTHER,
                Collectors.summingDouble(c -> safeDouble(c.getAmount()))
            ));
        
        double totalActualCost = actualByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        double laborActual = actualByCategory.getOrDefault(ActualCost.CostCategory.LABOR, 0.0);
        double materialActual = actualByCategory.getOrDefault(ActualCost.CostCategory.MATERIALS, 0.0);
        double travelActual = actualByCategory.getOrDefault(ActualCost.CostCategory.TRAVEL, 0.0);
        double otherActual = actualByCategory.getOrDefault(ActualCost.CostCategory.OTHER, 0.0) +
                            actualByCategory.getOrDefault(ActualCost.CostCategory.EQUIPMENT, 0.0) +
                            actualByCategory.getOrDefault(ActualCost.CostCategory.SUBCONTRACTOR, 0.0);
        
        // Calculate PO amounts by status
        double committedPOAmount = purchaseOrders.stream()
            .filter(po -> po.getStatus() == PurchaseOrder.POStatus.APPROVED || 
                         po.getStatus() == PurchaseOrder.POStatus.ORDERED ||
                         po.getStatus() == PurchaseOrder.POStatus.RECEIVED)
            .mapToDouble(po -> safeDouble(po.getAmount()))
            .sum();
        
        double pendingPOAmount = purchaseOrders.stream()
            .filter(po -> po.getStatus() == PurchaseOrder.POStatus.PENDING || 
                         po.getStatus() == PurchaseOrder.POStatus.DRAFT)
            .mapToDouble(po -> safeDouble(po.getAmount()))
            .sum();
        
        double totalPOAmount = committedPOAmount + pendingPOAmount;
        
        // Calculate change order impacts
        double approvedChangeOrders = changeOrders.stream()
            .filter(co -> co.getStatus() == ChangeOrder.ChangeStatus.APPROVED)
            .mapToDouble(co -> safeDouble(co.getAdditionalCost()))
            .sum();
        
        double pendingChangeOrders = changeOrders.stream()
            .filter(co -> co.getStatus() == ChangeOrder.ChangeStatus.SUBMITTED || 
                         co.getStatus() == ChangeOrder.ChangeStatus.DRAFT)
            .mapToDouble(co -> safeDouble(co.getAdditionalCost()))
            .sum();
        
        int recordCount = actualCosts.size() + purchaseOrders.size() + changeOrders.size();
        
        return new FinancialSummary(
            totalBudget, laborBudget, materialBudget, travelBudget, otherBudget,
            totalActualCost, laborActual, materialActual, travelActual, otherActual,
            totalPOAmount, committedPOAmount, pendingPOAmount,
            approvedChangeOrders, pendingChangeOrders,
            recordCount
        );
    }
    
    /**
     * Calculate burn rate (spending per day)
     */
    public static double calculateBurnRate(Project project, List<ActualCost> actualCosts) {
        if (actualCosts.isEmpty()) return 0.0;
        
        LocalDate projectStart = project.getStartDate();
        LocalDate today = LocalDate.now();
        long daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(projectStart, today);
        
        if (daysSinceStart <= 0) return 0.0;
        
        double totalSpent = actualCosts.stream()
            .filter(c -> c.getStatus() != ActualCost.CostStatus.DISPUTED)
            .mapToDouble(c -> safeDouble(c.getAmount()))
            .sum();
        
        return totalSpent / daysSinceStart;
    }
    
    /**
     * Project remaining budget based on burn rate
     */
    public static LocalDate projectBudgetDepletion(Project project, double burnRate, double currentSpent) {
        if (burnRate <= 0) return null;
        
        double budget = safeDouble(project.getBudgetAmount());
        double remaining = budget - currentSpent;
        
        if (remaining <= 0) return LocalDate.now();
        
        long daysRemaining = (long)(remaining / burnRate);
        return LocalDate.now().plusDays(daysRemaining);
    }
    
    /**
     * Calculate cost variance by period (for timeline view)
     */
    public static Map<LocalDate, Double> calculateCostByPeriod(
            List<ActualCost> actualCosts,
            LocalDate startDate,
            LocalDate endDate) {
        
        Map<LocalDate, Double> costByDate = new HashMap<>();
        
        // Initialize all dates with 0
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            costByDate.put(current, 0.0);
            current = current.plusDays(1);
        }
        
        // Aggregate costs by date
        for (ActualCost cost : actualCosts) {
            if (cost.getCostDate() != null && cost.getStatus() != ActualCost.CostStatus.DISPUTED) {
                LocalDate costDate = cost.getCostDate();
                if (!costDate.isBefore(startDate) && !costDate.isAfter(endDate)) {
                    costByDate.merge(costDate, safeDouble(cost.getAmount()), Double::sum);
                }
            }
        }
        
        return costByDate;
    }
    
    /**
     * Calculate cumulative cost over time (for charts)
     */
    public static Map<LocalDate, Double> calculateCumulativeCost(Map<LocalDate, Double> dailyCosts) {
        Map<LocalDate, Double> cumulative = new HashMap<>();
        double runningTotal = 0.0;
        
        for (Map.Entry<LocalDate, Double> entry : dailyCosts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList())) {
            runningTotal += entry.getValue();
            cumulative.put(entry.getKey(), runningTotal);
        }
        
        return cumulative;
    }
    
    /**
     * Validate financial data for consistency
     */
    public static List<String> validateFinancialData(
            Project project,
            List<ActualCost> actualCosts,
            List<PurchaseOrder> purchaseOrders) {
        
        List<String> warnings = new java.util.ArrayList<>();
        
        // Check for actual costs without POs
        for (ActualCost cost : actualCosts) {
            if (cost.getPurchaseOrderId() == null && cost.getAmount() != null && cost.getAmount() > 5000) {
                warnings.add(String.format("Large cost ($%,.2f) without PO: %s", 
                    cost.getAmount(), cost.getDescription()));
            }
        }
        
        // Check for POs exceeding budget category
        double totalPOs = purchaseOrders.stream()
            .mapToDouble(po -> safeDouble(po.getAmount()))
            .sum();
        
        if (totalPOs > safeDouble(project.getBudgetAmount()) * 1.1) {
            warnings.add(String.format("Total POs ($%,.2f) exceed budget by more than 10%%", totalPOs));
        }
        
        // Check for duplicate PO numbers
        Map<String, Long> poCounts = purchaseOrders.stream()
            .collect(Collectors.groupingBy(PurchaseOrder::getPoNumber, Collectors.counting()));
        
        poCounts.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .forEach(e -> warnings.add("Duplicate PO number: " + e.getKey()));
        
        return warnings;
    }
    
    /**
     * Safe double conversion
     */
    private static double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}