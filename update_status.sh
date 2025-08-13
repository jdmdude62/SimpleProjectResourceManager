#!/bin/bash

# Simple script to update project status to PLANNED for October 2025 and later

DB_PATH="$HOME/.SimpleProjectResourceManager/scheduler.db"

echo "Updating projects from October 2025 onwards to PLANNED status..."

# First count how many projects we're updating
COUNT=$(echo "SELECT COUNT(*) FROM projects WHERE date(start_date) >= '2025-10-01';" | java -cp "target/classes:$(ls ~/.m2/repository/com/zaxxer/HikariCP/*/HikariCP-*.jar):$(ls ~/.m2/repository/org/xerial/sqlite-jdbc/*/sqlite-jdbc-*.jar):$(ls ~/.m2/repository/org/slf4j/slf4j-api/*/slf4j-api-*.jar)" org.sqlite.JDBC3CommandLine "$DB_PATH" 2>/dev/null || echo "0")

echo "Found $COUNT projects to update"

# Update the projects  
java -cp "target/classes:$(ls ~/.m2/repository/com/zaxxer/HikariCP/*/HikariCP-*.jar):$(ls ~/.m2/repository/org/xerial/sqlite-jdbc/*/sqlite-jdbc-*.jar):$(ls ~/.m2/repository/org/slf4j/slf4j-api/*/slf4j-api-*.jar)" com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig << EOF
UPDATE projects SET status = 'PLANNED' WHERE date(start_date) >= '2025-10-01';
EOF

echo "Update complete!"