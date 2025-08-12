-- Add created_at timestamp to tasks table for proper ordering
-- This ensures new tasks always appear at the end of the day's schedule

ALTER TABLE tasks ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing tasks with a created_at based on their ID order
-- This preserves the current ordering while enabling proper timestamp-based sorting
UPDATE tasks 
SET created_at = datetime('2025-01-01 00:00:00', '+' || id || ' seconds')
WHERE created_at IS NULL;

-- Create index for performance
CREATE INDEX idx_tasks_planned_start_created ON tasks(planned_start, created_at);