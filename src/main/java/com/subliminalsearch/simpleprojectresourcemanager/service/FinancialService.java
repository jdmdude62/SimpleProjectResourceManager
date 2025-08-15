package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FinancialService {
    private static final Logger logger = LoggerFactory.getLogger(FinancialService.class);
    private final DataSource dataSource;
    private final ProjectRepository projectRepository;
    
    public FinancialService(DataSource dataSource, ProjectRepository projectRepository) {
        this.dataSource = dataSource;
        this.projectRepository = projectRepository;
        initializeTables();
    }
    
    private void initializeTables() {
        try (Connection conn = dataSource.getConnection()) {
            // Create purchase_orders table
            String createPOTable = """
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    po_number TEXT NOT NULL,
                    vendor TEXT,
                    description TEXT,
                    amount REAL,
                    status TEXT,
                    order_date TEXT,
                    expected_date TEXT,
                    received_date TEXT,
                    invoice_number TEXT,
                    resource_id INTEGER,
                    assignment_id INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id)
                )
                """;
            
            // Create actual_costs table
            String createCostsTable = """
                CREATE TABLE IF NOT EXISTS actual_costs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    cost_date TEXT,
                    category TEXT,
                    description TEXT,
                    amount REAL,
                    invoice_number TEXT,
                    receipt_number TEXT,
                    status TEXT,
                    estimated_amount REAL,
                    notes TEXT,
                    purchase_order_id INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id),
                    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id)
                )
                """;
            
            // Create change_orders table
            String createChangeOrdersTable = """
                CREATE TABLE IF NOT EXISTS change_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    change_order_number TEXT NOT NULL,
                    description TEXT,
                    additional_cost REAL,
                    reason TEXT,
                    status TEXT,
                    request_date TEXT,
                    approval_date TEXT,
                    requested_by TEXT,
                    approved_by TEXT,
                    additional_days INTEGER,
                    impact_description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (project_id) REFERENCES projects(id)
                )
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createPOTable);
                stmt.execute(createCostsTable);
                stmt.execute(createChangeOrdersTable);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to initialize financial tables", e);
        }
    }
    
    // Purchase Order operations
    public List<PurchaseOrder> getPurchaseOrdersForProject(Long projectId) {
        List<PurchaseOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM purchase_orders WHERE project_id = ? ORDER BY order_date DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PurchaseOrder po = mapPurchaseOrder(rs);
                orders.add(po);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get purchase orders for project: " + projectId, e);
        }
        
        return orders;
    }
    
    public void savePurchaseOrder(PurchaseOrder po) {
        if (po.getId() == null) {
            insertPurchaseOrder(po);
        } else {
            updatePurchaseOrder(po);
        }
    }
    
    private void insertPurchaseOrder(PurchaseOrder po) {
        String sql = """
            INSERT INTO purchase_orders (project_id, po_number, vendor, description, amount, 
                                        status, order_date, expected_date, received_date, 
                                        invoice_number, resource_id, assignment_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, po.getProjectId());
            stmt.setString(2, po.getPoNumber());
            stmt.setString(3, po.getVendor());
            stmt.setString(4, po.getDescription());
            stmt.setObject(5, po.getAmount());
            stmt.setString(6, po.getStatus().name());
            stmt.setString(7, po.getOrderDate() != null ? po.getOrderDate().toString() : null);
            stmt.setString(8, po.getExpectedDate() != null ? po.getExpectedDate().toString() : null);
            stmt.setString(9, po.getReceivedDate() != null ? po.getReceivedDate().toString() : null);
            stmt.setString(10, po.getInvoiceNumber());
            stmt.setObject(11, po.getResourceId());
            stmt.setObject(12, po.getAssignmentId());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                po.setId(rs.getLong(1));
            }
            
        } catch (SQLException e) {
            logger.error("Failed to insert purchase order", e);
        }
    }
    
    private void updatePurchaseOrder(PurchaseOrder po) {
        String sql = """
            UPDATE purchase_orders 
            SET po_number = ?, vendor = ?, description = ?, amount = ?, status = ?, 
                order_date = ?, expected_date = ?, received_date = ?, invoice_number = ?,
                resource_id = ?, assignment_id = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, po.getPoNumber());
            stmt.setString(2, po.getVendor());
            stmt.setString(3, po.getDescription());
            stmt.setObject(4, po.getAmount());
            stmt.setString(5, po.getStatus().name());
            stmt.setString(6, po.getOrderDate() != null ? po.getOrderDate().toString() : null);
            stmt.setString(7, po.getExpectedDate() != null ? po.getExpectedDate().toString() : null);
            stmt.setString(8, po.getReceivedDate() != null ? po.getReceivedDate().toString() : null);
            stmt.setString(9, po.getInvoiceNumber());
            stmt.setObject(10, po.getResourceId());
            stmt.setObject(11, po.getAssignmentId());
            stmt.setLong(12, po.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to update purchase order", e);
        }
    }
    
    public void deletePurchaseOrder(Long id) {
        String sql = "DELETE FROM purchase_orders WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to delete purchase order: " + id, e);
        }
    }
    
    // Actual Cost operations
    public List<ActualCost> getActualCostsForProject(Long projectId) {
        List<ActualCost> costs = new ArrayList<>();
        String sql = "SELECT * FROM actual_costs WHERE project_id = ? ORDER BY cost_date DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ActualCost cost = mapActualCost(rs);
                costs.add(cost);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get actual costs for project: " + projectId, e);
        }
        
        return costs;
    }
    
    public void saveActualCost(ActualCost cost) {
        if (cost.getId() == null) {
            insertActualCost(cost);
        } else {
            updateActualCost(cost);
        }
    }
    
    private void insertActualCost(ActualCost cost) {
        String sql = """
            INSERT INTO actual_costs (project_id, cost_date, category, description, amount,
                                     invoice_number, receipt_number, status, estimated_amount,
                                     notes, purchase_order_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, cost.getProjectId());
            stmt.setString(2, cost.getCostDate() != null ? cost.getCostDate().toString() : null);
            stmt.setString(3, cost.getCategory() != null ? cost.getCategory().name() : null);
            stmt.setString(4, cost.getDescription());
            stmt.setObject(5, cost.getAmount());
            stmt.setString(6, cost.getInvoiceNumber());
            stmt.setString(7, cost.getReceiptNumber());
            stmt.setString(8, cost.getStatus().name());
            stmt.setObject(9, cost.getEstimatedAmount());
            stmt.setString(10, cost.getNotes());
            stmt.setObject(11, cost.getPurchaseOrderId());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                cost.setId(rs.getLong(1));
            }
            
        } catch (SQLException e) {
            logger.error("Failed to insert actual cost", e);
        }
    }
    
    private void updateActualCost(ActualCost cost) {
        String sql = """
            UPDATE actual_costs 
            SET cost_date = ?, category = ?, description = ?, amount = ?, 
                invoice_number = ?, receipt_number = ?, status = ?, estimated_amount = ?,
                notes = ?, purchase_order_id = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cost.getCostDate() != null ? cost.getCostDate().toString() : null);
            stmt.setString(2, cost.getCategory() != null ? cost.getCategory().name() : null);
            stmt.setString(3, cost.getDescription());
            stmt.setObject(4, cost.getAmount());
            stmt.setString(5, cost.getInvoiceNumber());
            stmt.setString(6, cost.getReceiptNumber());
            stmt.setString(7, cost.getStatus().name());
            stmt.setObject(8, cost.getEstimatedAmount());
            stmt.setString(9, cost.getNotes());
            stmt.setObject(10, cost.getPurchaseOrderId());
            stmt.setLong(11, cost.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to update actual cost", e);
        }
    }
    
    public void deleteActualCost(Long id) {
        String sql = "DELETE FROM actual_costs WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to delete actual cost: " + id, e);
        }
    }
    
    // Change Order operations
    public List<ChangeOrder> getChangeOrdersForProject(Long projectId) {
        List<ChangeOrder> orders = new ArrayList<>();
        String sql = "SELECT * FROM change_orders WHERE project_id = ? ORDER BY request_date DESC";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, projectId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ChangeOrder co = mapChangeOrder(rs);
                orders.add(co);
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get change orders for project: " + projectId, e);
        }
        
        return orders;
    }
    
    public void saveChangeOrder(ChangeOrder co) {
        if (co.getId() == null) {
            insertChangeOrder(co);
        } else {
            updateChangeOrder(co);
        }
    }
    
    private void insertChangeOrder(ChangeOrder co) {
        String sql = """
            INSERT INTO change_orders (project_id, change_order_number, description, additional_cost,
                                      reason, status, request_date, approval_date, requested_by,
                                      approved_by, additional_days, impact_description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, co.getProjectId());
            stmt.setString(2, co.getChangeOrderNumber());
            stmt.setString(3, co.getDescription());
            stmt.setObject(4, co.getAdditionalCost());
            stmt.setString(5, co.getReason() != null ? co.getReason().name() : null);
            stmt.setString(6, co.getStatus().name());
            stmt.setString(7, co.getRequestDate() != null ? co.getRequestDate().toString() : null);
            stmt.setString(8, co.getApprovalDate() != null ? co.getApprovalDate().toString() : null);
            stmt.setString(9, co.getRequestedBy());
            stmt.setString(10, co.getApprovedBy());
            stmt.setObject(11, co.getAdditionalDays());
            stmt.setString(12, co.getImpactDescription());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                co.setId(rs.getLong(1));
            }
            
        } catch (SQLException e) {
            logger.error("Failed to insert change order", e);
        }
    }
    
    private void updateChangeOrder(ChangeOrder co) {
        String sql = """
            UPDATE change_orders 
            SET change_order_number = ?, description = ?, additional_cost = ?, reason = ?, 
                status = ?, request_date = ?, approval_date = ?, requested_by = ?,
                approved_by = ?, additional_days = ?, impact_description = ?, 
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, co.getChangeOrderNumber());
            stmt.setString(2, co.getDescription());
            stmt.setObject(3, co.getAdditionalCost());
            stmt.setString(4, co.getReason() != null ? co.getReason().name() : null);
            stmt.setString(5, co.getStatus().name());
            stmt.setString(6, co.getRequestDate() != null ? co.getRequestDate().toString() : null);
            stmt.setString(7, co.getApprovalDate() != null ? co.getApprovalDate().toString() : null);
            stmt.setString(8, co.getRequestedBy());
            stmt.setString(9, co.getApprovedBy());
            stmt.setObject(10, co.getAdditionalDays());
            stmt.setString(11, co.getImpactDescription());
            stmt.setLong(12, co.getId());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to update change order", e);
        }
    }
    
    public void deleteChangeOrder(Long id) {
        String sql = "DELETE FROM change_orders WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Failed to delete change order: " + id, e);
        }
    }
    
    // Update project financials
    public void updateProjectFinancials(Project project) {
        projectRepository.updateFinancials(project);
    }
    
    // Mapping methods
    private PurchaseOrder mapPurchaseOrder(ResultSet rs) throws SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(rs.getLong("id"));
        po.setProjectId(rs.getLong("project_id"));
        po.setPoNumber(rs.getString("po_number"));
        po.setVendor(rs.getString("vendor"));
        po.setDescription(rs.getString("description"));
        
        Double amount = rs.getDouble("amount");
        if (!rs.wasNull()) po.setAmount(amount);
        
        String status = rs.getString("status");
        if (status != null) po.setStatus(PurchaseOrder.POStatus.valueOf(status));
        
        String orderDate = rs.getString("order_date");
        if (orderDate != null) po.setOrderDate(LocalDate.parse(orderDate));
        
        String expectedDate = rs.getString("expected_date");
        if (expectedDate != null) po.setExpectedDate(LocalDate.parse(expectedDate));
        
        String receivedDate = rs.getString("received_date");
        if (receivedDate != null) po.setReceivedDate(LocalDate.parse(receivedDate));
        
        po.setInvoiceNumber(rs.getString("invoice_number"));
        
        Long resourceId = rs.getLong("resource_id");
        if (!rs.wasNull()) po.setResourceId(resourceId);
        
        Long assignmentId = rs.getLong("assignment_id");
        if (!rs.wasNull()) po.setAssignmentId(assignmentId);
        
        return po;
    }
    
    private ActualCost mapActualCost(ResultSet rs) throws SQLException {
        ActualCost cost = new ActualCost();
        cost.setId(rs.getLong("id"));
        cost.setProjectId(rs.getLong("project_id"));
        
        String costDate = rs.getString("cost_date");
        if (costDate != null) cost.setCostDate(LocalDate.parse(costDate));
        
        String category = rs.getString("category");
        if (category != null) cost.setCategory(ActualCost.CostCategory.valueOf(category));
        
        cost.setDescription(rs.getString("description"));
        
        Double amount = rs.getDouble("amount");
        if (!rs.wasNull()) cost.setAmount(amount);
        
        cost.setInvoiceNumber(rs.getString("invoice_number"));
        cost.setReceiptNumber(rs.getString("receipt_number"));
        
        String status = rs.getString("status");
        if (status != null) cost.setStatus(ActualCost.CostStatus.valueOf(status));
        
        Double estimatedAmount = rs.getDouble("estimated_amount");
        if (!rs.wasNull()) cost.setEstimatedAmount(estimatedAmount);
        
        cost.setNotes(rs.getString("notes"));
        
        Long poId = rs.getLong("purchase_order_id");
        if (!rs.wasNull()) cost.setPurchaseOrderId(poId);
        
        return cost;
    }
    
    private ChangeOrder mapChangeOrder(ResultSet rs) throws SQLException {
        ChangeOrder co = new ChangeOrder();
        co.setId(rs.getLong("id"));
        co.setProjectId(rs.getLong("project_id"));
        co.setChangeOrderNumber(rs.getString("change_order_number"));
        co.setDescription(rs.getString("description"));
        
        Double additionalCost = rs.getDouble("additional_cost");
        if (!rs.wasNull()) co.setAdditionalCost(additionalCost);
        
        String reason = rs.getString("reason");
        if (reason != null) co.setReason(ChangeOrder.ChangeReason.valueOf(reason));
        
        String status = rs.getString("status");
        if (status != null) co.setStatus(ChangeOrder.ChangeStatus.valueOf(status));
        
        String requestDate = rs.getString("request_date");
        if (requestDate != null) co.setRequestDate(LocalDate.parse(requestDate));
        
        String approvalDate = rs.getString("approval_date");
        if (approvalDate != null) co.setApprovalDate(LocalDate.parse(approvalDate));
        
        co.setRequestedBy(rs.getString("requested_by"));
        co.setApprovedBy(rs.getString("approved_by"));
        
        Integer additionalDays = rs.getInt("additional_days");
        if (!rs.wasNull()) co.setAdditionalDays(additionalDays);
        
        co.setImpactDescription(rs.getString("impact_description"));
        
        return co;
    }
}