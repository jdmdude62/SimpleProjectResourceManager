-- Add soft delete columns to main tables
-- These track when items were deleted and by whom

-- Projects table
ALTER TABLE projects ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE projects ADD COLUMN deleted_by TEXT NULL;

-- Assignments table  
ALTER TABLE assignments ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE assignments ADD COLUMN deleted_by TEXT NULL;

-- Resources table
ALTER TABLE resources ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE resources ADD COLUMN deleted_by TEXT NULL;

-- Tasks table
ALTER TABLE tasks ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE tasks ADD COLUMN deleted_by TEXT NULL;

-- Create indexes for performance on soft delete queries
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at);
CREATE INDEX idx_assignments_deleted_at ON assignments(deleted_at);
CREATE INDEX idx_resources_deleted_at ON resources(deleted_at);
CREATE INDEX idx_tasks_deleted_at ON tasks(deleted_at);

-- Create a trash/recycle bin metadata table
CREATE TABLE IF NOT EXISTS trash_metadata (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT NOT NULL, -- 'project', 'assignment', 'resource'
    entity_id INTEGER NOT NULL,
    entity_name TEXT NOT NULL,
    deleted_at TIMESTAMP NOT NULL,
    deleted_by TEXT,
    related_deletions TEXT, -- JSON string describing what else was deleted
    can_restore BOOLEAN DEFAULT 1,
    permanently_deleted_at TIMESTAMP NULL,
    UNIQUE(entity_type, entity_id)
);