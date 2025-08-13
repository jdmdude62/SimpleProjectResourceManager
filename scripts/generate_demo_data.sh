#!/bin/bash

# Generate Demo Data Script
# Generates demo data from templates using the configured rules

echo "=== Generate Demo Data Script ==="
echo

# Configuration paths
CONFIG_FILE="config/demo_data_config.yaml"
TEMPLATE_FILE="templates/demo_templates.json"
LOG_FILE="logs/demo_data_generation.log"

# Create log directory if it doesn't exist
mkdir -p logs

# Check prerequisites
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    exit 1
fi

if [ ! -f "$TEMPLATE_FILE" ]; then
    echo "Template file not found: $TEMPLATE_FILE"
    echo "Please run export_templates.sh first to create templates."
    exit 1
fi

# Parse command line arguments
START_YEAR=2024
END_YEAR=2026
MODE="standard"

while [[ $# -gt 0 ]]; do
    case $1 in
        --start)
            START_YEAR="$2"
            shift 2
            ;;
        --end)
            END_YEAR="$2"
            shift 2
            ;;
        --mode)
            MODE="$2"  # standard, performance, minimal
            shift 2
            ;;
        --clear)
            CLEAR_EXISTING="true"
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--start YEAR] [--end YEAR] [--mode MODE] [--clear]"
            exit 1
            ;;
    esac
done

echo "Configuration:"
echo "  Start Year: $START_YEAR"
echo "  End Year: $END_YEAR"
echo "  Mode: $MODE"
echo "  Template File: $TEMPLATE_FILE"
echo "  Config File: $CONFIG_FILE"
echo

if [ "$CLEAR_EXISTING" = "true" ]; then
    echo "WARNING: This will clear existing data before generation."
    read -p "Continue? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled."
        exit 1
    fi
fi

# Run the demo data generator
echo "Starting demo data generation..."
echo "Check $LOG_FILE for detailed progress."
echo

mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.data.DemoDataGenerator" \
              -Dexec.args="--config=$CONFIG_FILE --templates=$TEMPLATE_FILE --start=$START_YEAR --end=$END_YEAR --mode=$MODE" \
              -Dexec.classpathScope=runtime \
              -Dlog4j.configurationFile=log4j2.xml \
              2>&1 | tee "$LOG_FILE"

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo
    echo "=== Generation Complete ==="
    
    # Show summary statistics
    DB_PATH="$HOME/.SimpleProjectResourceManager/scheduler.db"
    echo
    echo "Database Statistics:"
    sqlite3 "$DB_PATH" << 'EOF'
SELECT 'Total Projects: ' || COUNT(*) FROM projects;
SELECT 'Total Assignments: ' || COUNT(*) FROM assignments;
SELECT 'Total Tasks: ' || COUNT(*) FROM tasks;
SELECT '';
SELECT 'Projects by Year:';
SELECT strftime('%Y', start_date) as year, COUNT(*) as count 
FROM projects 
GROUP BY year 
ORDER BY year;
SELECT '';
SELECT 'Projects by Status:';
SELECT status, COUNT(*) as count 
FROM projects 
GROUP BY status;
EOF
    
    echo
    echo "Demo data generated successfully!"
    echo "Start the application to see the generated data."
else
    echo
    echo "Generation failed. Check $LOG_FILE for errors."
    exit 1
fi