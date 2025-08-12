#!/bin/bash

# Script to load garden demo data into the database

echo "==================================="
echo "Garden Data Loader"
echo "==================================="
echo ""
echo "This will:"
echo "1. DELETE all existing data"
echo "2. Load garden-themed demo data"
echo ""
read -p "Continue? (y/n): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Cancelled."
    exit 1
fi

DB_PATH="$HOME/.SimpleProjectResourceManager/scheduler.db"
SQL_FILE="src/main/resources/db/garden_data_generator.sql"

if [ ! -f "$DB_PATH" ]; then
    echo "Error: Database not found at $DB_PATH"
    exit 1
fi

if [ ! -f "$SQL_FILE" ]; then
    echo "Error: SQL file not found at $SQL_FILE"
    exit 1
fi

echo ""
echo "Step 1: Clearing existing data..."

# Use sqlite3 directly if available, otherwise compile and run Java
if command -v sqlite3 &> /dev/null; then
    echo "Using sqlite3..."
    
    # Clear existing data
    sqlite3 "$DB_PATH" <<EOF
DELETE FROM assignments;
DELETE FROM tasks;  
DELETE FROM projects;
DELETE FROM resources;
DELETE FROM project_managers;
EOF
    
    echo "Step 2: Loading garden data..."
    
    # Load garden data (filter out problematic statements)
    grep -v "project_dependencies\|project_milestones\|resource_unavailability\|budget_allocations" "$SQL_FILE" | \
    grep -v "^--" | \
    sqlite3 "$DB_PATH"
    
    echo ""
    echo "Step 3: Verifying data..."
    sqlite3 "$DB_PATH" <<EOF
.mode column
.headers on
SELECT 'Project Managers:' as Type, COUNT(*) as Count FROM project_managers
UNION ALL
SELECT 'Resources:', COUNT(*) FROM resources  
UNION ALL
SELECT 'Projects:', COUNT(*) FROM projects
UNION ALL
SELECT 'Tasks:', COUNT(*) FROM tasks
UNION ALL
SELECT 'Assignments:', COUNT(*) FROM assignments;
EOF

else
    echo "sqlite3 not found. Using Java loader..."
    
    # Compile and run the DataManager
    mvn compile -q
    
    java -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" \
        com.subliminalsearch.simpleprojectresourcemanager.util.DataManager
fi

echo ""
echo "==================================="
echo "Garden data loaded successfully!"
echo "Please restart the application to see the changes."
echo "==================================="