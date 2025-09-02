-- Test SQL to create sample task dependencies for testing
-- Run this in your SQLite database to create sample dependencies

-- First, let's see what tasks exist for a project
SELECT id, title, project_id, planned_start, planned_end 
FROM tasks 
WHERE project_id IN (SELECT id FROM projects LIMIT 1)
ORDER BY planned_start;

-- Create sample dependencies between tasks
-- You'll need to replace these IDs with actual task IDs from your database
-- INSERT INTO task_dependencies (predecessor_id, successor_id, dependency_type, lag_days)
-- VALUES 
--   (1, 2, 'FINISH_TO_START', 0),
--   (2, 3, 'FINISH_TO_START', 0),
--   (3, 4, 'START_TO_START', 1);