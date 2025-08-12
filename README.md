# Simple Project Resource Manager

A JavaFX desktop application for field service scheduling and resource management, designed to replace Excel-based workflows with a modern, database-driven solution.

## Features

### Core Functionality
- **Timeline View**: Interactive calendar with drag-and-drop project management
- **Resource Management**: Track employees, contractors, and equipment
- **Project Tracking**: Complete project lifecycle management
- **Assignment System**: Allocate resources with automatic conflict detection
- **Financial Tracking**: Comprehensive cost management with PO tracking

### Reporting Suite
- Revenue/Budget Analysis
- Resource Utilization Reports
- Project Status Tracking
- Assignment Overview
- Workload Analysis
- Financial Summary with Export Options

### Financial Management
- Purchase Order tracking with approval workflow
- Actual cost recording with variance analysis
- Change order management
- Budget categorization and tracking
- Quick entry for batch cost input
- Integration-ready for Sage/QuickBooks

## System Requirements

- **Java**: JDK 17 or higher
- **Maven**: 3.8 or higher
- **Operating System**: Windows 10/11 or Linux
- **Memory**: Minimum 512MB heap size
- **Storage**: 100MB for application + database

## Installation

### Windows Setup

1. **Install Prerequisites**
   ```powershell
   # Install Java 17+ (if not already installed)
   # Download from: https://adoptium.net/
   
   # Install Maven
   # Download from: https://maven.apache.org/download.cgi
   
   # Set Environment Variables
   setx JAVA_HOME "C:\Program Files\Java\jdk-17"
   setx PATH "%PATH%;%JAVA_HOME%\bin;C:\Maven\bin"
   ```

2. **Clone and Build**
   ```bash
   git clone [repository-url]
   cd SimpleProjectResourceManager
   mvn clean compile
   ```

3. **Run Application**
   ```bash
   mvn javafx:run
   ```

### Linux Setup

1. **Install Prerequisites**
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install openjdk-17-jdk maven git
   
   # Fedora/RHEL
   sudo dnf install java-17-openjdk-devel maven git
   ```

2. **Clone and Build**
   ```bash
   git clone [repository-url]
   cd SimpleProjectResourceManager
   mvn clean compile
   ```

3. **Run Application**
   ```bash
   mvn javafx:run
   ```

## Database Location

The SQLite database is automatically created on first run:
- **Windows**: `%USERPROFILE%\.SimpleProjectResourceManager\scheduler.db`
- **Linux**: `~/.SimpleProjectResourceManager/scheduler.db`

## Quick Start Guide

1. **Launch the application**
   ```bash
   mvn javafx:run
   ```

2. **Initial Setup**
   - The database will be created automatically
   - Default admin user is created on first run

3. **Create Your First Project**
   - Click "New Project" in the toolbar
   - Enter project details and budget information
   - Set start and end dates

4. **Add Resources**
   - Navigate to Resources menu
   - Add employees, contractors, or equipment
   - Set availability and rates

5. **Create Assignments**
   - Drag resources onto projects in Timeline view
   - System automatically checks for conflicts
   - Adjust dates as needed

6. **Track Financials**
   - Right-click on any project in Timeline
   - Select "Track Financials"
   - Enter POs, costs, and change orders

7. **Generate Reports**
   - Click Reports menu
   - Select desired report type
   - Export to CSV or print

## Development

### Building from Source
```bash
# Full build with tests
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run tests only
mvn test
```

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/subliminalsearch/simpleprojectresourcemanager/
│   │       ├── model/          # Data models
│   │       ├── view/           # UI components
│   │       ├── controller/     # Controllers
│   │       ├── service/        # Business logic
│   │       ├── repository/     # Data access
│   │       └── config/         # Configuration
│   └── resources/
│       ├── fxml/              # FXML layouts
│       └── css/               # Stylesheets
└── test/                      # Unit tests
```

### Key Technologies
- **JavaFX 17**: Modern UI framework
- **SQLite**: Embedded database
- **HikariCP**: Connection pooling
- **Maven**: Build management
- **JUnit 5**: Testing framework

## Troubleshooting

### Common Issues

1. **Application won't start**
   - Verify Java 17+ is installed: `java -version`
   - Check Maven installation: `mvn -version`
   - Ensure JAVA_HOME is set correctly

2. **Database errors**
   - Check file permissions in user home directory
   - Verify no other instance is running
   - Delete database file to reset (data will be lost)

3. **UI scaling issues**
   - Adjust Windows display scaling settings
   - Use JVM parameter: `-Dglass.win.uiScale=1.0`

4. **Build failures**
   - Clear Maven cache: `mvn clean`
   - Update dependencies: `mvn dependency:resolve`
   - Check proxy settings if behind corporate firewall

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[License Type] - See LICENSE file for details

## Support

For issues, questions, or suggestions:
- Create an issue on GitHub
- Check CLAUDE.md for detailed development documentation
- Review existing issues before creating new ones

## Roadmap

### Phase 2 (In Progress)
- Financial timeline visualization
- Budget burn rate analysis
- Cost forecasting

### Phase 3 (Planned)
- Sage Accounting integration
- QuickBooks export
- Microsoft 365 calendar sync
- Mobile companion app

### Future Enhancements
- Web-based version
- Cloud database support
- Multi-tenancy
- REST API for integrations

## Version History

### v1.0.0 (Current)
- Core scheduling functionality
- Financial tracking system
- Comprehensive reporting suite
- SQLite database integration

---

For detailed development documentation, see [CLAUDE.md](CLAUDE.md)