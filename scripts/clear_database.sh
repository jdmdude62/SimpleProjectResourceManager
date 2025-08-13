#!/bin/bash

# Clear Database Script
# Removes all project data while preserving structure and base resources

echo "=== Clear Database Script ==="
echo "This will remove all project data from the database."
read -p "Are you sure you want to continue? (y/n): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Operation cancelled."
    exit 1
fi

DB_PATH="$HOME/.SimpleProjectResourceManager/scheduler.db"

if [ ! -f "$DB_PATH" ]; then
    echo "Database not found at: $DB_PATH"
    exit 1
fi

# Create backup first
BACKUP_PATH="$DB_PATH.backup.$(date +%Y%m%d_%H%M%S)"
cp "$DB_PATH" "$BACKUP_PATH"
echo "Backup created at: $BACKUP_PATH"

# Clear data using SQL commands
cat << 'EOF' | sqlite3 "$DB_PATH"
-- Disable foreign keys temporarily
PRAGMA foreign_keys = OFF;

-- Clear project-related data
DELETE FROM tasks;
DELETE FROM assignments;
DELETE FROM projects;

-- Reset auto-increment counters
DELETE FROM sqlite_sequence WHERE name IN ('tasks', 'assignments', 'projects');

-- Keep resources and project managers
-- They are base data needed for the system

-- Re-enable foreign keys
PRAGMA foreign_keys = ON;

-- Vacuum to reclaim space
VACUUM;

SELECT 'Cleared: ' || COUNT(*) || ' projects' FROM projects;
SELECT 'Cleared: ' || COUNT(*) || ' assignments' FROM assignments;
SELECT 'Cleared: ' || COUNT(*) || ' tasks' FROM tasks;
SELECT 'Preserved: ' || COUNT(*) || ' resources' FROM resources;
SELECT 'Preserved: ' || COUNT(*) || ' project managers' FROM project_managers;
EOF

echo "Database cleared successfully."
echo "Resources and project managers have been preserved."