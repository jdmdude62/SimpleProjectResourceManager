# Simple Project Resource Manager - Development Guide

## Project Overview
JavaFX desktop application for field service scheduling, replacing Excel-based workflow.
**Target Users:** 6 concurrent users (Project Managers, Schedulers, Technicians, 3rd Party Vendors)
**Database:** SQLite embedded database for on-premises deployment
**Integration:** Microsoft 365 SharePoint/Teams for real-time calendar sync and notifications

## Windows Setup Instructions
1. **Prerequisites:**
   - Install Java 17+ JDK (Oracle or OpenJDK)
   - Install Maven 3.8+
   - Install Git for Windows
   - Set JAVA_HOME environment variable
   - Add Maven bin directory to PATH

2. **Database Location:**
   - Windows: `%USERPROFILE%\.SimpleProjectResourceManager\scheduler.db`
   - Linux: `~/.SimpleProjectResourceManager/scheduler.db`

3. **First Run:**
   ```bash
   git clone [repository-url]
   cd SimpleProjectResourceManager
   mvn clean compile
   mvn javafx:run
   ```

## Build Commands
- **Build:** `mvn clean compile`
- **Test:** `mvn test`
- **Run Application:** `mvn javafx:run`
- **Package:** `mvn clean package`
- **Integration Tests:** `mvn test -Dtest=**/*IntegrationTest`
- **UI Tests:** `mvn test -Dtest=**/*UITest`

## Current Implementation Status

### ✅ Completed Features

#### Core Functionality
- **Timeline View:** Interactive calendar with drag-drop project/assignment management
- **Resource Management:** Complete CRUD for resources with availability tracking
- **Project Management:** Full project lifecycle with status tracking
- **Assignment System:** Resource allocation with conflict detection
- **Database:** SQLite with HikariCP connection pooling

#### Reports Module
1. **Revenue/Budget Report:** Financial overview with date filtering
2. **Resource Utilization Report:** Availability and assignment tracking
3. **Project Status Report:** Comprehensive project metrics
4. **Assignment Report:** Detailed assignment tracking
5. **Workload Report:** Resource capacity analysis

#### Financial Tracking System (Phase 1 Complete)
- **Purchase Orders:** Full lifecycle tracking (Draft → Pending → Approved → Ordered → Received → Paid)
- **Actual Costs:** Cost tracking with PO linking and variance analysis
- **Change Orders:** Complete approval workflow with impact analysis
- **Budget Management:** Category-based budget breakdown
- **Financial Summary:** Comprehensive financial reporting with export options
- **Quick Entry:** Batch cost entry for rapid data input

### 🚧 In Progress / Pending Features

#### Phase 2 - Financial Timeline
- Visual cost tracking over project timeline
- Budget burn rate visualization
- Cost forecasting based on current trends

#### Phase 3 - Accounting Integration
- Sage Accounting export functionality
- QuickBooks IIF format export
- Automated invoice generation

#### Additional Reports
- Geographic Distribution Report with map visualization
- Completion Analytics with performance metrics
- Custom report builder

## Code Standards
- **Java Version:** Java 17+ with JavaFX 17+
- **Indentation:** 4-space indentation (no tabs)
- **Naming Conventions:** 
  - Classes: PascalCase (e.g., `ProjectController`)
  - Methods/Variables: camelCase (e.g., `calculateTravelDays`)
  - Constants: UPPER_SNAKE_CASE (e.g., `DEFAULT_TRAVEL_DAYS`)
- **Comments:** Do not add comments unless explicitly requested
- **Null Safety:** Use Optional<> for nullable return types
- **Exception Handling:** Use specific exceptions, avoid generic Exception catching

## Architecture Patterns
- **MVC Pattern:** Model-View-Controller with FXML for UI layouts
- **Repository Pattern:** Data access layer abstraction
- **Service Layer:** Business logic separation from controllers
- **Dependency Injection:** Constructor injection preferred
- **Observer Pattern:** PropertyChangeListener for UI updates
- **Command Pattern:** User actions as command objects for undo/redo

## Project Structure
```
src/main/java/com.subliminalsearch.simpleprojectresourcemanager/
├── model/              # Data models (Project, Resource, Assignment, PurchaseOrder, ActualCost, ChangeOrder)
├── repository/         # Data access layer (ProjectRepository, ResourceRepository, AssignmentRepository)
├── service/            # Business logic (SchedulingService, ConflictDetectionService, FinancialService)
├── controller/         # JavaFX controllers (MainController, ProjectDialogController)
├── view/               # Custom JavaFX components (TimelineView, FinancialTrackingDialog)
├── component/          # Reusable UI components
├── integration/        # Microsoft Graph API integration
├── util/               # Utilities and helpers
└── config/             # Configuration classes

src/main/resources/
├── fxml/               # FXML layout files
├── css/                # JavaFX stylesheets
└── db/                 # Database schema and migrations
```

## Key Models and Services

### Financial Models
- **PurchaseOrder:** Tracks POs with vendor, amount, status, dates
- **ActualCost:** Records actual expenses with category and PO linking
- **ChangeOrder:** Manages project changes with approval workflow

### Financial Service
- Manages all financial data operations
- Creates and maintains financial tracking tables
- Provides CRUD operations for all financial entities

### Financial Tracking Dialog
- Multi-tab interface for comprehensive financial management
- Budget & Estimates, Purchase Orders, Actual Costs, Change Orders, Summary & Export
- Dynamic sizing to match Timeline view dimensions
- Real-time data refresh and validation

## Database Schema

### Core Tables
- `projects`: Project information with budget fields
- `resources`: Resource/employee data
- `assignments`: Resource-project allocations
- `project_managers`: Project manager information

### Financial Tables
- `purchase_orders`: PO tracking with lifecycle states
- `actual_costs`: Actual expense tracking with categories
- `change_orders`: Change order management with approval workflow

## Database Guidelines
- **Connection:** SQLite with HikariCP connection pooling
- **Location:** User home directory under .SimpleProjectResourceManager
- **Queries:** Always use PreparedStatement for parameterized queries
- **Transactions:** Wrap multi-table operations in transactions
- **Constraints:** Enforce foreign key constraints and data validation
- **Testing:** Use separate test database file for integration tests

## JavaFX Development
- **FXML:** Use FXML files for all complex UI layouts
- **CSS:** External stylesheets for theming and styling
- **Controllers:** Keep controllers thin, delegate to services
- **Threading:** Use Platform.runLater() for UI updates from background threads
- **Custom Components:** Extend Control class for reusable components
- **Bindings:** Use property bindings for reactive UI updates
- **Dialog Sizing:** Match parent window dimensions for consistency

## Key Dependencies
```xml
<!-- Core JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.2</version>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.42.0.0</version>
</dependency>

<!-- Connection Pooling -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>

<!-- Microsoft Graph (Future) -->
<dependency>
    <groupId>com.microsoft.graph</groupId>
    <artifactId>microsoft-graph</artifactId>
    <version>5.74.0</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.9.3</version>
    <scope>test</scope>
</dependency>
```

## Conflict Detection Rules
- **Resource Double-booking:** Same resource assigned to overlapping date ranges
- **Travel Time Conflicts:** Insufficient time between assignments based on travel days
- **Availability Conflicts:** Assignment during PTO, sick days, or unavailable periods
- **Project Boundary Violations:** Assignment dates outside project start/end dates

## Performance Requirements
- **Timeline Navigation:** < 2 seconds for month view with 100 projects
- **Conflict Detection:** < 5 seconds for analyzing 200 assignments
- **Database Operations:** < 500ms for typical CRUD operations
- **SharePoint Sync:** < 30 seconds for calendar event creation
- **Memory Usage:** < 512MB heap size for normal operations

## Security Guidelines
- **Database:** No SQL injection vulnerabilities (use PreparedStatement)
- **Credentials:** Store OAuth tokens securely, never in source code
- **File Permissions:** Restrict database file access to application user
- **Logging:** Never log sensitive data (tokens, passwords, PII)
- **Updates:** Regular dependency updates for security patches

## Deployment Notes
- **Target Platform:** Windows 10/11 desktop environments, Linux compatibility maintained
- **Java Distribution:** Bundle JRE with application using jlink/jpackage
- **Database Location:** User-specific directory for multi-user support
- **Configuration:** External config file for Microsoft 365 settings
- **Installation:** MSI installer for Windows deployment

## Development Workflow
1. Create feature branch from main
2. Implement with corresponding tests
3. Run full test suite before commit
4. Update CLAUDE.md if new patterns/commands added
5. Create pull request with test evidence
6. Merge after review and CI passing

## Known Issues and Solutions

### Common Issues
1. **Dialog Sizing:** Financial dialogs must match Timeline view size (90% width, 99% height)
2. **List Refresh:** Always reload ObservableList after database operations
3. **Date Handling:** Use LocalDate for dates, LocalDateTime for timestamps
4. **Cost Categories:** Use enum for consistent categorization

### Windows-Specific Considerations
- File paths use backslashes (handled by Java File API)
- Database file in %USERPROFILE% directory
- Maven commands work the same in PowerShell/CMD
- Use `mvn.cmd` if Maven wrapper is present

## Testing Strategy
- **Unit Tests:** JUnit 5 with Mockito for mocking
- **Integration Tests:** Test complete workflows with test database
- **UI Tests:** TestFX for JavaFX application testing
- **Test Naming:** `shouldDoSomething_WhenCondition()` format
- **Test Data:** Builder pattern for creating test objects
- **Coverage:** Aim for 80%+ code coverage on service layer

## Future Migration Notes
- **Web Version:** Design with REST API in mind for future web conversion
- **Cloud Database:** Abstract database layer for future PostgreSQL migration
- **Multi-tenancy:** Consider tenant isolation patterns for SaaS version
- **Dynamics 365:** Plan integration points for future ERP connection
- **Mobile Access:** Consider responsive design for tablet access

## Quick Reference - Key Classes

### Views
- `TimelineView`: Main calendar interface with project visualization
- `FinancialTrackingDialog`: Comprehensive financial management interface
- `ReportDialog`: Centralized reporting interface

### Services
- `FinancialService`: All financial data operations
- `SchedulingService`: Assignment and conflict management
- `ProjectService`: Project lifecycle management

### Models
- `Project`: Core project entity with financial fields
- `PurchaseOrder`: PO tracking with status workflow
- `ActualCost`: Expense tracking with categorization
- `ChangeOrder`: Change management with approval

## Recent Changes Log
- Implemented comprehensive financial tracking system (Phase 1)
- Added Purchase Order management with full lifecycle
- Created Actual Cost tracking with PO linking
- Built Change Order system with approval workflow
- Implemented Financial Summary with detailed reporting
- Added Quick Entry for batch cost input
- Fixed dialog sizing to match Timeline view
- Integrated financial tracking via project context menu