-- Migration: Add project managers table and link to projects
-- Date: 2025-08-08
-- Description: Creates project_managers table and adds foreign key relationship

-- Create project_managers table
CREATE TABLE IF NOT EXISTS project_managers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(50),
    department VARCHAR(100),
    active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default "Unassigned" project manager
INSERT INTO project_managers (name, email, department, active) 
VALUES ('Unassigned', '', 'General', 1);

-- Add project_manager_id column to projects table
ALTER TABLE projects ADD COLUMN project_manager_id INTEGER;

-- Set default value to the "Unassigned" PM
UPDATE projects SET project_manager_id = (SELECT id FROM project_managers WHERE name = 'Unassigned') 
WHERE project_manager_id IS NULL;

-- Add foreign key constraint (SQLite doesn't enforce FK by default but good for documentation)
-- FOREIGN KEY (project_manager_id) REFERENCES project_managers(id)