package com.subliminalsearch.simpleprojectresourcemanager.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

public class HelpButton {
    
    /**
     * Creates a standardized help button for placement in dialogs and views
     * @param helpTitle The title for the help dialog
     * @param helpContent The markdown-formatted help content
     * @return A configured help button
     */
    public static Button create(String helpTitle, String helpContent) {
        Button helpButton = new Button("?");
        helpButton.setStyle(
            "-fx-background-radius: 15; " +
            "-fx-min-width: 30; " +
            "-fx-min-height: 30; " +
            "-fx-max-width: 30; " +
            "-fx-max-height: 30; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-color: #007bff; " +
            "-fx-text-fill: white; " +
            "-fx-cursor: hand;"
        );
        
        // Add hover effect
        helpButton.setOnMouseEntered(e -> 
            helpButton.setStyle(helpButton.getStyle() + "-fx-background-color: #0056b3;")
        );
        helpButton.setOnMouseExited(e -> 
            helpButton.setStyle(helpButton.getStyle() + "-fx-background-color: #007bff;")
        );
        
        helpButton.setOnAction(e -> {
            // Get the window that contains this button
            Window owner = helpButton.getScene() != null ? helpButton.getScene().getWindow() : null;
            showHelpDialog(helpTitle, helpContent, owner);
        });
        
        return helpButton;
    }
    
    /**
     * Adds a help button to the top-right corner of a container
     * @param container The container to add the help button to
     * @param helpTitle The title for the help dialog
     * @param helpContent The help content
     * @return The help button that was added
     */
    public static Button addToContainer(Region container, String helpTitle, String helpContent) {
        Button helpButton = create(helpTitle, helpContent);
        
        // Position the button in the top-right corner
        if (container instanceof StackPane) {
            StackPane stackPane = (StackPane) container;
            StackPane.setAlignment(helpButton, Pos.TOP_RIGHT);
            StackPane.setMargin(helpButton, new Insets(10));
            stackPane.getChildren().add(helpButton);
        } else if (container instanceof VBox || container instanceof javafx.scene.layout.HBox) {
            // For VBox/HBox, wrap in a StackPane
            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(container, helpButton);
            StackPane.setAlignment(helpButton, Pos.TOP_RIGHT);
            StackPane.setMargin(helpButton, new Insets(10));
        }
        
        return helpButton;
    }
    
    public static void showHelpDialog(String title, String content, Window owner) {
        Alert alert = DialogUtils.createScreenAwareAlert(Alert.AlertType.INFORMATION, owner);
        alert.setTitle("Help - " + title);
        alert.setHeaderText(title);
        
        // Create a TextFlow with formatted content
        TextFlow textFlow = createFormattedContent(content);
        textFlow.setPadding(new Insets(10));
        textFlow.setLineSpacing(2);
        
        // Wrap in scroll pane
        ScrollPane scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(620);
        scrollPane.setPrefHeight(420);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
        
        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setPrefWidth(650);
        alert.getDialogPane().setPrefHeight(500);
        
        // Style the dialog
        alert.getDialogPane().setStyle("-fx-font-family: 'Segoe UI', sans-serif;");
        
        // Show the dialog
        alert.showAndWait();
    }
    
    private static TextFlow createFormattedContent(String content) {
        TextFlow textFlow = new TextFlow();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                textFlow.getChildren().add(new Text("\n"));
            } else if (line.matches("^[A-Z][A-Z0-9 &-]+:$")) {
                // Main heading (all caps with colon)
                Text heading = new Text(line + "\n");
                heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                textFlow.getChildren().add(heading);
            } else if (line.matches("^\\d+\\. [A-Z][A-Z0-9 &-]+:$")) {
                // Numbered heading (e.g., "1. BUDGET & ESTIMATES:")
                Text heading = new Text(line + "\n");
                heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                textFlow.getChildren().add(heading);
            } else if (line.startsWith("• ")) {
                // Bullet point
                Text bullet = new Text("  • ");
                bullet.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                Text text = new Text(line.substring(2) + "\n");
                text.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                textFlow.getChildren().addAll(bullet, text);
            } else if (line.matches("^={3,}$")) {
                // Separator line - skip it
                continue;
            } else if (line.matches("^.+ - .+$") && line.equals(line.toUpperCase())) {
                // Title line (e.g., "TIMELINE VIEW - Resource Scheduling")
                Text heading = new Text(line + "\n");
                heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
                textFlow.getChildren().add(heading);
            } else {
                // Regular text
                Text text = new Text(line + "\n");
                text.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
                textFlow.getChildren().add(text);
            }
        }
        
        return textFlow;
    }
    
    // Pre-defined help content for common features
    public static class HelpContent {
        
        public static final String TIMELINE_VIEW = 
            "TIMELINE VIEW - Resource Scheduling\n\n" +
            "OVERVIEW:\n" +
            "The Timeline View provides a visual calendar for managing resource assignments and project scheduling.\n\n" +
            
            "KEY FEATURES:\n" +
            "• Drag-and-Drop Scheduling: Click and drag assignments to new dates or resources\n" +
            "• Edge Resizing: Drag assignment edges to adjust duration\n" +
            "• 6-Week View: Shows current month plus 2 weeks of next month\n" +
            "• Conflict Detection: Red highlighting for double-booked resources\n" +
            "• Smart Tooltips: Hover for details that stay visible while reading\n\n" +
            
            "VISUAL INDICATORS:\n" +
            "• Blue Bars: Active assignments\n" +
            "• Green Bars: Completed assignments\n" +
            "• Red Bars: Conflicts or delays\n" +
            "• Gray Bars: Planned/draft assignments\n" +
            "• Weekend Shading: Light blue background\n" +
            "• Holiday Shading: Light pink background\n" +
            "• Today Marker: Black highlight\n\n" +
            
            "INTERACTIONS:\n" +
            "• Left Click: Select assignment\n" +
            "• Right Click: Context menu with options\n" +
            "• Drag: Move assignment to new date/resource\n" +
            "• Edge Drag: Resize assignment duration\n" +
            "• Hover: View detailed information\n" +
            "• Double Click: Edit assignment\n\n" +
            
            "FILTERS:\n" +
            "• Project Manager: Show only specific PM's projects\n" +
            "• Project: Focus on single project\n" +
            "• Resource: Show specific resource\n" +
            "• Status: Filter by project status\n\n" +
            
            "NAVIGATION:\n" +
            "• Previous/Next buttons: Navigate by day/week/month\n" +
            "• Today button: Jump to current date\n" +
            "• Date picker: Jump to specific date\n" +
            "• Zoom: Adjust timeline scale\n\n" +
            
            "TIPS:\n" +
            "• Assignments automatically snap to day boundaries\n" +
            "• Tooltips show complete project and resource information\n" +
            "• Red striped patterns indicate resource conflicts\n" +
            "• Bar text abbreviates for narrow assignments";
            
        public static final String FINANCIAL_TRACKING = 
            "FINANCIAL TRACKING - Project Financials\n\n" +
            "OVERVIEW:\n" +
            "Comprehensive financial management for projects including budgets, purchase orders, and actual costs.\n\n" +
            
            "TABS:\n\n" +
            "1. BUDGET & ESTIMATES:\n" +
            "• Track budgets by category (Labor, Materials, Equipment, Travel, Other)\n" +
            "• View total project budget\n" +
            "• Monitor budget vs actual variance\n" +
            "• Set baseline estimates\n\n" +
            
            "2. PURCHASE ORDERS:\n" +
            "• Create and track POs through complete lifecycle\n" +
            "• Status workflow: Draft → Pending → Approved → Ordered → Received → Paid\n" +
            "• Link to vendors and track amounts\n" +
            "• Approval tracking with dates\n\n" +
            
            "3. ACTUAL COSTS:\n" +
            "• Record actual expenses by category\n" +
            "• Link costs to specific POs\n" +
            "• Track payment status\n" +
            "• Calculate variance from budget\n\n" +
            
            "4. CHANGE ORDERS:\n" +
            "• Document scope changes\n" +
            "• Track approval workflow\n" +
            "• Monitor impact on budget and timeline\n" +
            "• Maintain change history\n\n" +
            
            "5. SUMMARY & EXPORT:\n" +
            "• View consolidated financial metrics\n" +
            "• Budget vs Actual comparison\n" +
            "• Profit margin calculation\n" +
            "• Export to CSV for accounting\n\n" +
            
            "6. QUICK ENTRY:\n" +
            "• Rapid batch cost entry\n" +
            "• Bulk update capabilities\n" +
            "• Template support for common costs\n\n" +
            
            "WORKFLOWS:\n" +
            "• PO Approval: Create → Submit → Approve → Order\n" +
            "• Cost Entry: Enter → Categorize → Link to PO → Save\n" +
            "• Change Order: Request → Review → Approve → Implement\n\n" +
            
            "TIPS:\n" +
            "• All financial data auto-saves\n" +
            "• Use Quick Entry for multiple similar costs\n" +
            "• Link actual costs to POs for better tracking\n" +
            "• Export regularly for accounting reconciliation";
            
        public static final String GANTT_CHART = 
            "GANTT CHART - Task Management\n\n" +
            "OVERVIEW:\n" +
            "Visual project timeline showing tasks, dependencies, and progress.\n\n" +
            
            "FEATURES:\n" +
            "• Task bars show duration and progress\n" +
            "• Dependencies shown with arrow connectors\n" +
            "• Resource badges display assigned personnel\n" +
            "• Progress bars indicate completion percentage\n" +
            "• Milestone markers for key dates\n\n" +
            
            "TASK INFORMATION:\n" +
            "• Left panel shows task hierarchy\n" +
            "• Indentation indicates subtasks\n" +
            "• Task bars are color-coded by status\n" +
            "• Resource initials shown on bars\n" +
            "• Progress percentage displayed\n\n" +
            
            "INTERACTIONS:\n" +
            "• Click task to select\n" +
            "• Double-click to edit\n" +
            "• Drag edges to adjust dates\n" +
            "• Right-click for context menu\n" +
            "• Hover for detailed tooltips\n\n" +
            
            "DEPENDENCIES:\n" +
            "• Finish-to-Start (FS): Default dependency\n" +
            "• Start-to-Start (SS): Tasks start together\n" +
            "• Finish-to-Finish (FF): Tasks end together\n" +
            "• Start-to-Finish (SF): Rare dependency type\n\n" +
            
            "VISUAL INDICATORS:\n" +
            "• Blue: Active tasks\n" +
            "• Green: Completed tasks\n" +
            "• Red: Overdue tasks\n" +
            "• Orange: At-risk tasks\n" +
            "• Gray: Not started\n\n" +
            
            "TIPS:\n" +
            "• Critical path highlighted in red\n" +
            "• Zoom to see more/less detail\n" +
            "• Export to image for reports\n" +
            "• Print view available";
            
        public static final String EXECUTIVE_DASHBOARD = 
            "EXECUTIVE DASHBOARD - KPI Overview\n\n" +
            "OVERVIEW:\n" +
            "Real-time metrics and key performance indicators for executive oversight.\n\n" +
            
            "KEY METRICS:\n" +
            "• Active Projects: Current project count and status\n" +
            "• Resource Utilization: Team capacity and allocation\n" +
            "• Revenue Metrics: Budget vs actual, profit margins\n" +
            "• Timeline Health: On-time delivery rate\n" +
            "• Risk Indicators: At-risk projects and issues\n\n" +
            
            "DASHBOARD SECTIONS:\n\n" +
            "1. PROJECT OVERVIEW:\n" +
            "• Active project count\n" +
            "• Status distribution chart\n" +
            "• Upcoming milestones\n" +
            "• Recent completions\n\n" +
            
            "2. FINANCIAL SUMMARY:\n" +
            "• Total portfolio value\n" +
            "• Budget utilization\n" +
            "• Cost overruns\n" +
            "• Profit margins by project\n\n" +
            
            "3. RESOURCE METRICS:\n" +
            "• Team utilization percentage\n" +
            "• Available capacity\n" +
            "• Upcoming availability\n" +
            "• Overtime tracking\n\n" +
            
            "4. PERFORMANCE INDICATORS:\n" +
            "• On-time delivery rate\n" +
            "• Customer satisfaction\n" +
            "• Quality metrics\n" +
            "• Efficiency trends\n\n" +
            
            "5. RISK DASHBOARD:\n" +
            "• At-risk projects\n" +
            "• Resource conflicts\n" +
            "• Budget warnings\n" +
            "• Schedule delays\n\n" +
            
            "CHARTS & VISUALIZATIONS:\n" +
            "• Pie charts for status distribution\n" +
            "• Bar charts for resource allocation\n" +
            "• Line graphs for trends\n" +
            "• Heat maps for risk assessment\n\n" +
            
            "REFRESH & EXPORT:\n" +
            "• Auto-refresh every 5 minutes\n" +
            "• Manual refresh button\n" +
            "• Export to PDF for reports\n" +
            "• Email dashboard snapshot\n\n" +
            
            "TIPS:\n" +
            "• Click any metric for details\n" +
            "• Hover over charts for values\n" +
            "• Use filters to focus on specific areas\n" +
            "• Set up alerts for threshold breaches";
            
        public static final String PROJECT_DIALOG = 
            "PROJECT DIALOG - Project Management\n\n" +
            "FIELDS:\n" +
            "• Project ID: Unique identifier (auto-generated)\n" +
            "• Description: Project name/description\n" +
            "• Status: Current project status\n" +
            "• Project Manager: Assigned PM\n" +
            "• Start/End Dates: Project timeline\n" +
            "• Budget Fields: Financial allocations\n\n" +
            
            "REQUIRED FIELDS:\n" +
            "• Project ID (auto-filled)\n" +
            "• Description\n" +
            "• Start Date\n" +
            "• End Date\n" +
            "• Status\n\n" +
            
            "TIPS:\n" +
            "• Dates auto-validate for logical order\n" +
            "• Budget categories are optional\n" +
            "• Status affects timeline appearance";
            
        public static final String ASSIGNMENT_DIALOG = 
            "ASSIGNMENT DIALOG - Resource Scheduling\n\n" +
            "FIELDS:\n" +
            "• Project: Select from active projects\n" +
            "• Resource: Choose available resource\n" +
            "• Start/End Dates: Assignment period\n" +
            "• Travel Days: Out/back travel time\n" +
            "• Notes: Special instructions\n\n" +
            
            "CONFLICT DETECTION:\n" +
            "• Automatic checking for double-booking\n" +
            "• Warning for travel time conflicts\n" +
            "• Holiday/weekend alerts\n" +
            "• Override option with reason\n\n" +
            
            "TIPS:\n" +
            "• Travel days extend assignment duration\n" +
            "• Override requires justification\n" +
            "• Conflicts shown in red";
            
        public static final String RESOURCE_DIALOG = 
            "RESOURCE DIALOG - Resource Management\n\n" +
            "FIELDS:\n" +
            "• Name: Resource full name\n" +
            "• Email: Contact email\n" +
            "• Phone: Contact number\n" +
            "• Type: Resource category\n" +
            "• Skills: Capabilities and certifications\n\n" +
            
            "AVAILABILITY:\n" +
            "• Set regular working hours\n" +
            "• Mark unavailable dates\n" +
            "• Schedule recurring unavailability\n" +
            "• Track PTO and holidays\n\n" +
            
            "TIPS:\n" +
            "• Email validates format\n" +
            "• Phone auto-formats\n" +
            "• Skills help with assignment matching";
    }
}