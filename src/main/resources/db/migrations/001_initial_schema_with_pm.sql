-- Initial schema creation with Project Manager support
-- Date: 2025-08-08
-- This creates all base tables including project_manager_id

PRAGMA foreign_keys = ON;

-- Projects table with project_manager_id included
CREATE TABLE projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    project_manager_id INTEGER,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Resources table
CREATE TABLE resources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    resource_type VARCHAR(50) NOT NULL,
    resource_category VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Assignments table
CREATE TABLE assignments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    travel_out_days INTEGER DEFAULT 0,
    travel_back_days INTEGER DEFAULT 0,
    override BOOLEAN DEFAULT FALSE,
    override_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_projects_start_date ON projects(start_date);
CREATE INDEX idx_projects_end_date ON projects(end_date);
CREATE INDEX idx_assignments_project_id ON assignments(project_id);
CREATE INDEX idx_assignments_resource_id ON assignments(resource_id);
CREATE INDEX idx_assignments_dates ON assignments(start_date, end_date);