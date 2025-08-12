#!/bin/bash

# Load doghouse installation sample data
echo "Loading doghouse installation sample data..."

# Database path
DB_PATH="$HOME/.SimpleProjectResourceManager/scheduler.db"

# Load the SQL file
cat doghouse_data_generator.sql | sqlite3 "$DB_PATH"

if [ $? -eq 0 ]; then
    echo "✓ Doghouse data loaded successfully!"
    
    # Show summary
    echo ""
    echo "Data Summary:"
    echo "============="
    
    # Count new technicians
    TECH_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM resources WHERE id BETWEEN 51 AND 60;")
    echo "New Field Technicians: $TECH_COUNT"
    
    # Count doghouse projects
    DH_COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM projects WHERE project_id LIKE 'DH-%';")
    echo "Doghouse Projects: $DH_COUNT"
    
    # Count January assignments for doghouse projects
    JAN_ASSIGN=$(sqlite3 "$DB_PATH" "
        SELECT COUNT(*) FROM assignments a 
        JOIN projects p ON a.project_id = p.id 
        WHERE p.project_id LIKE 'DH-%' 
        AND date(a.start_date) >= date('2025-01-01') 
        AND date(a.start_date) <= date('2025-01-31');
    ")
    echo "January Doghouse Assignments: $JAN_ASSIGN"
    
    # Show team rotation schedule
    echo ""
    echo "January 2025 Team Rotation Schedule:"
    echo "===================================="
    echo "Week 1 (Jan 6-10):  Team Alpha builds, Team Bravo installs"
    echo "Week 2 (Jan 13-17): Team Bravo builds, Team Alpha installs"
    echo "Week 3 (Jan 20-24): Team Alpha builds, Team Bravo installs"
    echo "Week 4 (Jan 27-31): Team Bravo builds, Team Alpha installs"
    
else
    echo "✗ Error loading doghouse data!"
    exit 1
fi