-- Migration: Add Open Items table for granular project tracking
-- Date: 2025-08-29

-- Create open_items table
CREATE TABLE IF NOT EXISTS open_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    task_id INTEGER,  -- Optional link to tasks table
    
    -- Basic Information
    item_number VARCHAR(50),  -- e.g., "OI-2025-001"
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),  -- e.g., "Documentation", "Development", "Testing"
    priority VARCHAR(20) DEFAULT 'MEDIUM',  -- HIGH, MEDIUM, LOW
    
    -- Progress Tracking
    estimated_start_date DATE,
    estimated_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    progress_percentage INTEGER DEFAULT 0,  -- 0-100
    
    -- Status Management
    status VARCHAR(50) DEFAULT 'NOT_STARTED',  -- NOT_STARTED, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
    health_status VARCHAR(50) DEFAULT 'ON_TRACK',  -- ON_TRACK, AT_RISK, DELAYED, CRITICAL
    
    -- Assignment
    assigned_to VARCHAR(255),  -- Resource name or ID
    assigned_resource_id INTEGER,
    
    -- Dependencies
    depends_on_item_id INTEGER,  -- Reference to another open_item
    blocks_item_ids TEXT,  -- Comma-separated list of blocked items
    
    -- Additional Fields
    notes TEXT,
    tags TEXT,  -- Comma-separated tags for filtering
    estimated_hours DECIMAL(10,2),
    actual_hours DECIMAL(10,2),
    
    -- Metadata
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Soft delete support
    is_deleted BOOLEAN DEFAULT 0,
    deleted_at TIMESTAMP,
    
    -- Foreign Keys
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_resource_id) REFERENCES resources(id) ON DELETE SET NULL,
    FOREIGN KEY (depends_on_item_id) REFERENCES open_items(id) ON DELETE SET NULL
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_open_items_project_id ON open_items(project_id);
CREATE INDEX IF NOT EXISTS idx_open_items_status ON open_items(status);
CREATE INDEX IF NOT EXISTS idx_open_items_health_status ON open_items(health_status);
CREATE INDEX IF NOT EXISTS idx_open_items_assigned_resource ON open_items(assigned_resource_id);
CREATE INDEX IF NOT EXISTS idx_open_items_priority ON open_items(priority);
CREATE INDEX IF NOT EXISTS idx_open_items_is_deleted ON open_items(is_deleted);

-- Create a view for active open items
CREATE VIEW IF NOT EXISTS active_open_items AS
SELECT * FROM open_items 
WHERE is_deleted = 0;