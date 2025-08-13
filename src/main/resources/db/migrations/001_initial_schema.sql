-- Initial schema for Simple Project Resource Manager
-- Created: 2025-08-06
-- Updated: 2025-08-09 - Added project_managers table

PRAGMA foreign_keys = ON;

CREATE TABLE project_managers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(50),
    department VARCHAR(100),
    active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id VARCHAR(50) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    project_manager_id INTEGER,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)
);

CREATE TABLE resource_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL
);

CREATE TABLE resources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    resource_type_id INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_type_id) REFERENCES resource_types(id)
);

CREATE TABLE assignments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    travel_out_days INTEGER DEFAULT 1,
    travel_back_days INTEGER DEFAULT 1,
    is_override BOOLEAN DEFAULT FALSE,
    override_reason TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (resource_id) REFERENCES resources(id)
);

CREATE TABLE resource_availability (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    availability_type VARCHAR(20) NOT NULL,
    reason VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_id) REFERENCES resources(id)
);

CREATE TABLE sharepoint_sync_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_id INTEGER,
    assignment_id INTEGER,
    sync_type VARCHAR(20) NOT NULL,
    sharepoint_event_id VARCHAR(100),
    sync_status VARCHAR(20) DEFAULT 'PENDING',
    error_message TEXT,
    synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_id) REFERENCES resources(id),
    FOREIGN KEY (assignment_id) REFERENCES assignments(id)
);

-- Indexes for better performance
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_dates ON projects(start_date, end_date);
CREATE INDEX idx_projects_pm ON projects(project_manager_id);
CREATE INDEX idx_assignments_project ON assignments(project_id);
CREATE INDEX idx_assignments_resource ON assignments(resource_id);
CREATE INDEX idx_assignments_dates ON assignments(start_date, end_date);
CREATE INDEX idx_availability_resource ON resource_availability(resource_id);
CREATE INDEX idx_availability_dates ON resource_availability(start_date, end_date);
CREATE INDEX idx_sync_log_assignment ON sharepoint_sync_log(assignment_id);
CREATE INDEX idx_sync_log_status ON sharepoint_sync_log(sync_status);

-- Insert default project managers
INSERT INTO project_managers (name, email, department, active) VALUES 
    ('Unassigned', '', 'General', 1),
    ('David Thompson', 'david.thompson@company.com', 'Engineering', 1),
    ('Maria Garcia', 'maria.garcia@company.com', 'Operations', 1),
    ('James Wilson', 'james.wilson@company.com', 'Infrastructure', 1),
    ('Jennifer Lee', 'jennifer.lee@company.com', 'Technology', 1);

-- Insert default resource types
INSERT INTO resource_types (name, category) VALUES 
    ('Full-Time Employee', 'INTERNAL'),
    ('Part-Time Employee', 'INTERNAL'),
    ('Contractor', 'CONTRACTOR'),
    ('Vendor', 'VENDOR');