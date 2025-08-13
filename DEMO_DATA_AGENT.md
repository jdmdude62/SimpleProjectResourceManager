# Demo Data Management Agent

## Purpose
This agent handles the complete workflow for creating, managing, and generating demo data using a template-based system for the Simple Project Resource Manager application.

## Capabilities
1. Clear existing project data while preserving base resources
2. Guide creation of template projects
3. Export projects as reusable templates
4. Generate varied demo data from templates
5. Validate generated data

## Prerequisites
- Maven installed and configured
- Java 17+ 
- Application compiled (`mvn clean compile`)

## Workflow Commands

### 1. Clear Database
Removes all project data while preserving resources and project managers:

```bash
# Using Java utility (recommended - no external dependencies)
java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
  com.subliminalsearch.simpleprojectresourcemanager.data.DatabaseCleaner 2>/dev/null

# Alternative using shell script (requires sqlite3)
echo "y" | scripts/clear_database.sh
```

### 2. Run Application
Start the application to create template projects:

```bash
mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.SchedulerApplication" -q
```

### 3. Template Project Guidelines

Create 3 diverse template projects that represent different business patterns:

#### Garden Installation Project (GAR-2025-001)
- **Type:** Seasonal, outdoor work
- **Duration:** 3-5 days
- **Resources:** 1-3 field technicians
- **Pattern:** Peak in spring/summer (April-August)
- **Tasks:** Site prep, installation, cleanup
- **Travel:** Varies by location

#### Doghouse Build Project (DH-2025-001)  
- **Type:** Manufacturing + installation
- **Duration:** 4 days (2 workshop, 2 field)
- **Resources:** 2-5 technicians (rotating teams)
- **Pattern:** Peak in fall (September-November)
- **Tasks:** Material prep, assembly, delivery, installation
- **Travel:** Standardized routes

#### Cathouse Construction Project (CAT-2025-001)
- **Type:** Custom builds
- **Duration:** 2-3 days
- **Resources:** 2-4 specialists
- **Pattern:** Year-round steady
- **Tasks:** Design review, construction, finishing
- **Travel:** Minimal (mostly local)

### 4. Export Templates

After creating template projects in the UI:

```bash
# Run the export script
chmod +x scripts/export_templates.sh
scripts/export_templates.sh

# Or directly with Java
mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.data.TemplateExporter" \
  -Dexec.args="--ids=1,2,3 --output=templates/demo_templates.json"
```

### 5. Configure Generation Rules

Create/edit `config/demo_data_config.yaml`:

```yaml
clearExisting: false
durationVariance: 0.2
conflictRate: 0.1

projectTypes:
  garden:
    minPerMonth: 3
    maxPerMonth: 8
    peakMonths: [4, 5, 6, 7, 8]
    offMonths: [12, 1, 2]
  doghouse:
    minPerMonth: 2
    maxPerMonth: 5
    peakMonths: [9, 10, 11]
    offMonths: [6, 7, 8]
  cathouse:
    minPerMonth: 2
    maxPerMonth: 4
    peakMonths: []
    offMonths: []

businessRules:
  workDays: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
  preferMondayStarts: true
  avoidHolidays: true
  maxConcurrentProjects: 15
  resourceUtilization: 0.75
```

### 6. Generate Demo Data

```bash
# Standard generation (2024-2026)
scripts/generate_demo_data.sh

# Custom date range
scripts/generate_demo_data.sh --start 2023 --end 2025

# With clearing existing data
scripts/generate_demo_data.sh --clear

# Direct Java execution
mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.data.DemoDataGenerator" \
  -Dexec.args="--config=config/demo_data_config.yaml --templates=templates/demo_templates.json --start=2024 --end=2026"
```

### 7. Verify Generated Data

```bash
# Check database statistics
sqlite3 ~/.SimpleProjectResourceManager/scheduler.db << 'EOF'
SELECT 'Total Projects: ' || COUNT(*) FROM projects;
SELECT 'Total Assignments: ' || COUNT(*) FROM assignments;
SELECT 'Total Tasks: ' || COUNT(*) FROM tasks;
SELECT '';
SELECT 'Projects by Year:';
SELECT strftime('%Y', start_date) as year, COUNT(*) as count 
FROM projects GROUP BY year ORDER BY year;
EOF

# Run application to visually verify
mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.SchedulerApplication"
```

## File Structure

```
SimpleProjectResourceManager/
├── src/main/java/.../data/
│   ├── DatabaseCleaner.java       # Clears project data
│   ├── TemplateExporter.java      # Exports templates
│   └── DemoDataGenerator.java     # Generates demo data
├── scripts/
│   ├── clear_database.sh          # Shell script to clear DB
│   ├── export_templates.sh        # Interactive template export
│   └── generate_demo_data.sh      # Demo data generation
├── templates/
│   └── demo_templates.json        # Exported project templates
└── config/
    └── demo_data_config.yaml      # Generation configuration
```

## Common Issues & Solutions

### Issue: "sqlite3: command not found"
**Solution:** Use the Java DatabaseCleaner instead of shell script

### Issue: Projects not visible after generation
**Solution:** Check the date range in timeline view - navigate to the correct time period

### Issue: Compilation errors with YAML
**Solution:** Ensure SnakeYAML is in pom.xml and module-info.java includes `requires org.yaml.snakeyaml;`

### Issue: Resource conflicts in generated data
**Solution:** Adjust `conflictRate` in config or `resourceUtilization` to reduce overlaps

## Advanced Features

### Custom Variation Rules
Edit templates to include specific variation patterns:
- Seasonal adjustments
- Resource rotation requirements  
- Location-based duration changes
- Skill level requirements

### Business Rule Enforcement
The generator automatically:
- Avoids weekend starts
- Prefers Monday project starts
- Distributes resources evenly
- Creates realistic conflict scenarios
- Applies seasonal patterns

### Template Analysis
Before generation, analyze your templates:
```bash
mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.data.TemplateAnalyzer" \
  -Dexec.args="--templates=templates/demo_templates.json"
```

## Testing Workflow

1. **Clear database**
2. **Create 3 template projects manually**
3. **Export templates**
4. **Clear database again**
5. **Generate demo data from templates**
6. **Verify in application**
7. **Adjust configuration if needed**
8. **Regenerate**

## Integration with CI/CD

```bash
#!/bin/bash
# ci-demo-data.sh
set -e

echo "Building application..."
mvn clean compile

echo "Clearing database..."
java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
  com.subliminalsearch.simpleprojectresourcemanager.data.DatabaseCleaner

echo "Generating demo data..."
mvn exec:java -Dexec.mainClass="com.subliminalsearch.simpleprojectresourcemanager.data.DemoDataGenerator" \
  -Dexec.args="--config=config/demo_data_config.yaml --templates=templates/demo_templates.json"

echo "Demo data generation complete!"
```

## Maintenance

- **Update templates** when business requirements change
- **Adjust configuration** for different demo scenarios
- **Version control** templates and configurations
- **Document** any custom template patterns
- **Test** generation after model changes

## Success Indicators

✅ Database cleared successfully shows 0 projects  
✅ Templates exported to JSON file  
✅ Configuration YAML validates  
✅ Generation completes without errors  
✅ Generated data spans requested date range  
✅ Resource utilization is realistic  
✅ Seasonal patterns are visible  
✅ Conflicts are intentional and manageable  
✅ Application displays data correctly