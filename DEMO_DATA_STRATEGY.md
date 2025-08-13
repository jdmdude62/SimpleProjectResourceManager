# Demo Data Generation Strategy

## Overview
This document outlines the template-based demo data generation system for the Simple Project Resource Manager. The system uses manually-created template projects as the foundation for generating realistic, varied demo data that spans multiple years.

## Core Principles

### 1. Template-Based Generation
- Start with 3 manually-created projects through the UI (Garden, Doghouse, Cathouse)
- These serve as "golden templates" that guarantee schema compatibility
- All generated data derives from these templates with intelligent variations

### 2. Data Integrity First
- Never manually craft SQL inserts
- Always derive from known-good database records
- Maintain referential integrity through template relationships
- Validate all generated data before insertion

### 3. Realistic Business Patterns
- Seasonal variations (gardens in spring/summer)
- Resource utilization targets (65-80%)
- Natural project clustering and gaps
- Realistic conflict rates (10-15%)

## Process Workflow

### Phase 1: Template Creation
1. **Clear Database**
   ```bash
   ./scripts/clear_database.sh
   ```

2. **Manual Project Creation**
   - Create Garden Project (2-5 days, 1-3 resources)
   - Create Doghouse Project (build phase: 4 days, install phase: 3 days)
   - Create Cathouse Project (similar to doghouse)

3. **Template Export**
   ```bash
   ./scripts/export_templates.sh
   ```
   Creates: `templates/demo_templates.json`

### Phase 2: Data Generation
1. **Configure Generation Parameters**
   Edit `config/demo_data_config.yaml`

2. **Generate Demo Data**
   ```java
   DemoDataGenerator generator = new DemoDataGenerator();
   generator.loadTemplates("templates/demo_templates.json");
   generator.generateDemoData(2024, 2026);
   ```

3. **Validate Generated Data**
   ```java
   DataValidator validator = new DataValidator();
   validator.validateAllData();
   ```

## Configuration Structure

### demo_data_config.yaml
```yaml
generation:
  start_year: 2024
  end_year: 2026
  
  projects_per_month:
    garden: 
      min: 3
      max: 8
      peak_months: [4, 5, 6, 7, 8]  # April-August
    doghouse:
      min: 2
      max: 5
      peak_months: [9, 10, 11]  # Fall
    cathouse:
      min: 1
      max: 3
      consistent: true  # Same throughout year

  resources:
    utilization_target: 0.75
    max_concurrent_projects: 3
    travel_time_between_projects: 1  # days
    
  variations:
    duration_variance: 0.2  # ±20%
    resource_substitution_rate: 0.3  # 30% use different resources
    priority_distribution:
      high: 0.2
      medium: 0.6
      low: 0.2
    
  conflicts:
    target_conflict_rate: 0.1  # 10% of assignments
    types:
      - resource_double_booking
      - insufficient_travel_time
      - project_overlap
    
  historical_data:
    past_completion_rate: 0.95
    past_on_time_rate: 0.7
    past_delay_reasons:
      - "Weather delay"
      - "Material shortage"
      - "Resource unavailable"
      - "Scope change"
```

## Template Structure

### templates/demo_templates.json
```json
{
  "version": "1.0",
  "created": "2025-01-11",
  "templates": [
    {
      "type": "garden",
      "base_project": {
        "project_id": "GAR-TEMPLATE-001",
        "description": "Garden Installation Template",
        "duration_days": 3,
        "resources": [
          {
            "role": "lead_gardener",
            "skill_level": "senior"
          },
          {
            "role": "assistant",
            "skill_level": "junior"
          }
        ],
        "tasks": [
          {
            "name": "Site Planning",
            "duration_hours": 4,
            "sequence": 1
          },
          {
            "name": "Soil Preparation",
            "duration_hours": 8,
            "sequence": 2
          },
          {
            "name": "Planting",
            "duration_hours": 12,
            "sequence": 3
          }
        ]
      },
      "variation_rules": {
        "duration_range": [2, 5],
        "resource_count_range": [1, 3],
        "seasonal_preference": "spring_summer",
        "location_pool": "residential_addresses"
      }
    }
  ]
}
```

## Variation Engine Rules

### Project Variations
1. **Temporal Shifts**
   - Move project dates while maintaining duration
   - Respect weekends and holidays
   - Maintain minimum gaps between similar projects

2. **Resource Variations**
   - Substitute resources with similar skill levels
   - Rotate through available resources
   - Maintain team compositions (Alpha/Bravo for doghouses)

3. **Content Variations**
   - Modify descriptions with location/customer details
   - Add contextual notes based on season/conditions
   - Vary priorities based on business cycles

### Intelligent Patterns
1. **Seasonal Logic**
   - Gardens: Peak in spring/summer, minimal in winter
   - Doghouses: Higher in fall (prep for winter)
   - Cathouses: Consistent year-round

2. **Resource Loading**
   - Monday starts preferred
   - Avoid scheduling over holidays
   - Respect resource capacity limits

3. **Business Realism**
   - Fiscal year-end rush (December)
   - Summer vacation impact (July-August)
   - Spring project surge (March-May)

## Data Validation Rules

### Pre-Generation Validation
- Template completeness check
- Resource availability verification
- Date range validity

### Post-Generation Validation
- No resource double-booking beyond threshold
- All foreign keys valid
- Date ranges logical
- Status progression realistic

### Integrity Checks
```java
public class DataIntegrityChecker {
    checkReferentialIntegrity()
    checkDateConsistency()
    checkResourceUtilization()
    checkProjectCompleteness()
    checkTaskDependencies()
}
```

## Maintenance Strategy

### Schema Changes
1. Update template export/import to handle new fields
2. Modify variation engine for new business rules
3. Update validation rules
4. Re-export templates after schema migration

### Version Control
- Keep templates in version control
- Tag templates with schema version
- Document breaking changes
- Maintain migration scripts

### Testing Integration
```java
@BeforeEach
void setupDemoData() {
    DemoDataGenerator.loadStandardDataset();
}

@Test
void testWithRealisticData() {
    // Tests run against consistent demo data
}
```

## Usage Examples

### Quick Demo Data
```bash
# Generate standard demo dataset
mvn exec:java -Dexec.mainClass="DemoDataGenerator" -Dexec.args="--quick"
```

### Custom Date Range
```bash
# Generate data for specific period
mvn exec:java -Dexec.mainClass="DemoDataGenerator" -Dexec.args="--start=2024-01 --end=2025-12"
```

### Performance Testing
```bash
# Generate large dataset for stress testing
mvn exec:java -Dexec.mainClass="DemoDataGenerator" -Dexec.args="--performance --multiplier=10"
```

## Benefits

1. **Reliability**: Template-based approach ensures data compatibility
2. **Realism**: Business rules create believable patterns
3. **Maintainability**: Configuration-driven, easy to update
4. **Testability**: Consistent, reproducible datasets
5. **Flexibility**: Easy to add new project types or rules

## Future Enhancements

1. **Machine Learning Integration**
   - Learn patterns from production data
   - Generate even more realistic distributions

2. **Industry Templates**
   - Pre-built templates for different industries
   - Compliance with industry standards

3. **Data Anonymization**
   - Import real data and anonymize for demos
   - Maintain statistical properties

4. **Multi-tenant Support**
   - Generate isolated datasets for SaaS demos
   - Different data patterns per tenant type