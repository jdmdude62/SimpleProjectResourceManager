-- Migration to remove UNIQUE constraint from project_id
-- This allows multiple projects with the same project_id (for different phases/locations)

-- SQLite doesn't support ALTER TABLE DROP CONSTRAINT directly
-- We need to recreate the table without the UNIQUE constraint

-- Step 1: Create new table without UNIQUE constraint on project_id
CREATE TABLE projects_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    project_manager_id INTEGER,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Client contact fields (added in previous migrations)
    contact_name VARCHAR(100),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    contact_company VARCHAR(100),
    contact_role VARCHAR(50),
    contact_address TEXT,
    send_reports BOOLEAN DEFAULT 1,
    report_frequency VARCHAR(20) DEFAULT 'WEEKLY',
    last_report_sent TIMESTAMP,
    
    -- Budget and financial fields (added in previous migrations)
    budget_amount DECIMAL(15,2),
    actual_cost DECIMAL(15,2),
    revenue_amount DECIMAL(15,2),
    currency_code VARCHAR(3) DEFAULT 'USD',
    labor_cost DECIMAL(15,2),
    material_cost DECIMAL(15,2),
    travel_cost DECIMAL(15,2),
    other_cost DECIMAL(15,2),
    cost_notes TEXT,
    
    FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)
);

-- Step 2: Copy data from old table to new table
INSERT INTO projects_new SELECT * FROM projects;

-- Step 3: Drop old table
DROP TABLE projects;

-- Step 4: Rename new table to original name
ALTER TABLE projects_new RENAME TO projects;

-- Step 5: Recreate any indexes that were on the original table
CREATE INDEX idx_projects_project_id ON projects(project_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_dates ON projects(start_date, end_date);
CREATE INDEX idx_projects_manager ON projects(project_manager_id);