-- Task Management Schema
-- Created: 2025-08-09
-- Adds comprehensive task management capabilities

-- Project phases/milestones
CREATE TABLE project_phases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    sequence_order INTEGER DEFAULT 0,
    planned_start DATE,
    planned_end DATE,
    actual_start DATE,
    actual_end DATE,
    status VARCHAR(20) DEFAULT 'NOT_STARTED',
    color VARCHAR(7), -- Hex color for UI
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Main tasks table
CREATE TABLE tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    phase_id INTEGER,
    parent_task_id INTEGER, -- For subtasks
    task_code VARCHAR(50) UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- Task categorization
    task_type VARCHAR(50) DEFAULT 'GENERAL', -- INSTALLATION, CONFIGURATION, TESTING, DOCUMENTATION, TRAINING
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- CRITICAL, HIGH, MEDIUM, LOW
    status VARCHAR(30) DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, BLOCKED, REVIEW, COMPLETED, CANCELLED
    progress_percentage INTEGER DEFAULT 0,
    
    -- Scheduling
    planned_start DATE,
    planned_end DATE,
    actual_start DATE,
    actual_end DATE,
    estimated_hours DECIMAL(10,2),
    actual_hours DECIMAL(10,2),
    
    -- Assignment
    assigned_to INTEGER,
    assigned_by INTEGER,
    reviewer_id INTEGER,
    
    -- Field service specific
    location TEXT,
    equipment_required TEXT,
    safety_requirements TEXT,
    site_access_notes TEXT,
    
    -- Risk management
    risk_level VARCHAR(20) DEFAULT 'LOW', -- HIGH, MEDIUM, LOW
    risk_notes TEXT,
    
    -- Integration
    ms365_task_id VARCHAR(255), -- Microsoft Planner/Project task ID
    ms365_sync_status VARCHAR(20),
    ms365_last_sync TIMESTAMP,
    
    -- Tracking
    created_by INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    completed_by INTEGER,
    
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (phase_id) REFERENCES project_phases(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES resources(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_by) REFERENCES project_managers(id) ON DELETE SET NULL,
    FOREIGN KEY (reviewer_id) REFERENCES resources(id) ON DELETE SET NULL
);

-- Task dependencies (predecessor/successor relationships)
CREATE TABLE task_dependencies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    predecessor_id INTEGER NOT NULL, -- Task that must complete first
    successor_id INTEGER NOT NULL, -- Task that depends on predecessor
    dependency_type VARCHAR(20) DEFAULT 'FS', -- FS (Finish-Start), SS (Start-Start), FF (Finish-Finish), SF (Start-Finish)
    lag_days INTEGER DEFAULT 0, -- Days to wait after predecessor (can be negative)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (predecessor_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (successor_id) REFERENCES tasks(id) ON DELETE CASCADE,
    UNIQUE(predecessor_id, successor_id)
);

-- Additional resources assigned to tasks
CREATE TABLE task_resources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    role VARCHAR(50) DEFAULT 'SUPPORT', -- PRIMARY, SUPPORT, REVIEWER, OBSERVER
    hours_allocated DECIMAL(10,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
    UNIQUE(task_id, resource_id)
);

-- Task comments/discussion thread
CREATE TABLE task_comments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    author_id INTEGER,
    author_name VARCHAR(100), -- Store name in case user is deleted
    comment_text TEXT NOT NULL,
    is_system_comment BOOLEAN DEFAULT 0, -- For automated status changes
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- Task attachments
CREATE TABLE task_attachments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT,
    file_size INTEGER,
    mime_type VARCHAR(100),
    uploaded_by INTEGER,
    uploaded_by_name VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- Task change history for audit trail
CREATE TABLE task_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    field_name VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    changed_by INTEGER,
    changed_by_name VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- Task templates for common workflows
CREATE TABLE task_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    task_type VARCHAR(50),
    estimated_hours DECIMAL(10,2),
    equipment_required TEXT,
    safety_requirements TEXT,
    is_active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Checklist items for tasks
CREATE TABLE task_checklist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    item_text VARCHAR(255) NOT NULL,
    is_completed BOOLEAN DEFAULT 0,
    completed_by INTEGER,
    completed_at TIMESTAMP,
    sequence_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- Performance indexes
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_phase ON tasks(phase_id);
CREATE INDEX idx_tasks_parent ON tasks(parent_task_id);
CREATE INDEX idx_tasks_assigned ON tasks(assigned_to);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_dates ON tasks(planned_start, planned_end);
CREATE INDEX idx_task_deps_pred ON task_dependencies(predecessor_id);
CREATE INDEX idx_task_deps_succ ON task_dependencies(successor_id);
CREATE INDEX idx_task_resources_task ON task_resources(task_id);
CREATE INDEX idx_task_resources_resource ON task_resources(resource_id);
CREATE INDEX idx_task_comments_task ON task_comments(task_id);
CREATE INDEX idx_task_ms365 ON tasks(ms365_task_id);

-- Insert default task templates
INSERT INTO task_templates (name, task_type, estimated_hours, equipment_required, safety_requirements) VALUES
    ('Site Survey', 'ASSESSMENT', 4.0, 'Measuring tools, Camera, Laptop', 'Safety vest, Hard hat'),
    ('Equipment Installation', 'INSTALLATION', 8.0, 'Tool kit, Ladder, Testing equipment', 'Safety vest, Hard hat, Safety glasses'),
    ('System Configuration', 'CONFIGURATION', 6.0, 'Laptop, Network cables, Console cable', 'None'),
    ('User Training', 'TRAINING', 4.0, 'Training materials, Projector', 'None'),
    ('Documentation Review', 'DOCUMENTATION', 2.0, 'None', 'None'),
    ('System Testing', 'TESTING', 6.0, 'Testing equipment, Laptop', 'Safety vest if on-site'),
    ('Final Inspection', 'REVIEW', 2.0, 'Checklist, Camera', 'Safety vest, Hard hat');

-- Sample phases for demonstration
INSERT INTO project_phases (project_id, name, sequence_order, status) 
SELECT id, 'Planning', 1, 'COMPLETED' FROM projects LIMIT 1;

INSERT INTO project_phases (project_id, name, sequence_order, status) 
SELECT id, 'Execution', 2, 'IN_PROGRESS' FROM projects LIMIT 1;

INSERT INTO project_phases (project_id, name, sequence_order, status) 
SELECT id, 'Testing', 3, 'NOT_STARTED' FROM projects LIMIT 1;

INSERT INTO project_phases (project_id, name, sequence_order, status) 
SELECT id, 'Delivery', 4, 'NOT_STARTED' FROM projects LIMIT 1;