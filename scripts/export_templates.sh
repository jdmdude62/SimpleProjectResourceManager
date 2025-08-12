#!/bin/bash

# Export Templates Script
# Exports selected projects as templates for demo data generation

echo "=== Export Templates Script ==="
echo

DB_PATH="$HOME/.SimpleProjectResourceManager/scheduler.db"
TEMPLATE_DIR="templates"
OUTPUT_FILE="$TEMPLATE_DIR/demo_templates.json"

# Create template directory if it doesn't exist
mkdir -p "$TEMPLATE_DIR"

# Check if database exists
if [ ! -f "$DB_PATH" ]; then
    echo "Database not found at: $DB_PATH"
    exit 1
fi

# Show available projects
echo "Available projects in database:"
echo
sqlite3 "$DB_PATH" << 'EOF'
.mode column
.headers on
SELECT id, project_id, description, start_date, end_date 
FROM projects 
ORDER BY id 
LIMIT 20;
EOF

echo
echo "Enter the project IDs to export as templates (comma-separated):"
echo "Example: 1,2,3"
read -r PROJECT_IDS

if [ -z "$PROJECT_IDS" ]; then
    echo "No project IDs provided. Exiting."
    exit 1
fi

# Run the Java export utility
echo "Exporting projects: $PROJECT_IDS"
mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.data.TemplateExporter" \
              -Dexec.args="--ids=$PROJECT_IDS --output=$OUTPUT_FILE" \
              -Dexec.classpathScope=runtime

if [ $? -eq 0 ]; then
    echo "Templates exported successfully to: $OUTPUT_FILE"
    echo
    echo "Template file preview:"
    head -n 50 "$OUTPUT_FILE"
else
    echo "Export failed. Please check the error messages above."
    exit 1
fi