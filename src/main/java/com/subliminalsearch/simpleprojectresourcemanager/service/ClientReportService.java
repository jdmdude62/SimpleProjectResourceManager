package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClientReportService {
    private static final Logger logger = LoggerFactory.getLogger(ClientReportService.class);
    
    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 24;
    private static final float FONT_SIZE_HEADING = 18;
    private static final float FONT_SIZE_SUBHEADING = 14;
    private static final float FONT_SIZE_NORMAL = 12;
    private static final float LINE_SPACING = 1.5f;
    
    private final String dbPath;
    
    public ClientReportService() {
        this.dbPath = System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db";
    }
    
    public File generateProjectReport(Project project) throws IOException, SQLException {
        String fileName = String.format("ProjectReport_%s_%s.pdf", 
            project.getProjectId(), 
            LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        File outputFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        
        try (PDDocument document = new PDDocument()) {
            // Add cover page
            addCoverPage(document, project);
            
            // Add project status page
            addProjectStatusPage(document, project);
            
            // Add timeline/schedule page
            addTimelinePage(document, project);
            
            // Add resources page
            addResourcesPage(document, project);
            
            // Add tasks summary page
            addTasksSummaryPage(document, project);
            
            // Add contact information page
            addContactPage(document, project);
            
            // Save the document
            document.save(outputFile);
            logger.info("Generated PDF report: {}", outputFile.getAbsolutePath());
        }
        
        return outputFile;
    }
    
    private void addCoverPage(PDDocument document, Project project) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float centerX = pageWidth / 2;
            
            // Company logo placeholder (you can add actual logo later)
            drawRectangle(contentStream, centerX - 50, pageHeight - 150, 100, 100, 
                         new Color(41, 128, 185));
            
            // Title
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
            String title = "PROJECT STATUS REPORT";
            float titleWidth = getStringWidth(title, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
            contentStream.beginText();
            contentStream.newLineAtOffset(centerX - titleWidth / 2, pageHeight - 250);
            contentStream.showText(title);
            contentStream.endText();
            
            // Project ID
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
            String projectId = project.getProjectId();
            float projectIdWidth = getStringWidth(projectId, new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(centerX - projectIdWidth / 2, pageHeight - 300);
            contentStream.showText(projectId);
            contentStream.endText();
            
            // Client information
            if (project.getContactCompany() != null) {
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SUBHEADING);
                String company = project.getContactCompany();
                float companyWidth = getStringWidth(company, new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SUBHEADING);
                contentStream.beginText();
                contentStream.newLineAtOffset(centerX - companyWidth / 2, pageHeight - 350);
                contentStream.showText(company);
                contentStream.endText();
            }
            
            // Report date
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
            String reportDate = "Report Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            float dateWidth = getStringWidth(reportDate, new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
            contentStream.beginText();
            contentStream.newLineAtOffset(centerX - dateWidth / 2, MARGIN + 50);
            contentStream.showText(reportDate);
            contentStream.endText();
            
            // Add decorative elements
            drawLine(contentStream, MARGIN, pageHeight - 280, pageWidth - MARGIN, pageHeight - 280, 2);
            drawLine(contentStream, MARGIN, pageHeight - 320, pageWidth - MARGIN, pageHeight - 320, 2);
        }
    }
    
    private void addProjectStatusPage(PDDocument document, Project project) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - MARGIN;
            
            // Header
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("PROJECT STATUS OVERVIEW");
            contentStream.endText();
            yPosition -= 40;
            
            // Draw status indicator (colored box)
            Color statusColor = getStatusColor(project.getStatus());
            drawRectangle(contentStream, MARGIN, yPosition - 60, 150, 50, statusColor);
            
            // Status text
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADING);
            contentStream.setNonStrokingColor(Color.WHITE);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN + 20, yPosition - 35);
            contentStream.showText(project.getStatus().getDisplayName().toUpperCase());
            contentStream.endText();
            contentStream.setNonStrokingColor(Color.BLACK);
            yPosition -= 80;
            
            // Project details
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
            
            // Start date
            yPosition = addLabelValue(contentStream, "Start Date:", 
                project.getStartDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), 
                MARGIN, yPosition);
            
            // End date
            yPosition = addLabelValue(contentStream, "End Date:", 
                project.getEndDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), 
                MARGIN, yPosition);
            
            // Duration
            yPosition = addLabelValue(contentStream, "Duration:", 
                project.getDurationDays() + " days", 
                MARGIN, yPosition);
            
            // Progress calculation (simplified - you'd need actual task completion data)
            int progress = calculateProjectProgress(project);
            yPosition = addLabelValue(contentStream, "Overall Progress:", 
                progress + "%", 
                MARGIN, yPosition);
            
            // Draw progress bar
            yPosition -= 20;
            drawProgressBar(contentStream, MARGIN, yPosition, 400, 30, progress);
            yPosition -= 50;
            
            // Key metrics section
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("KEY METRICS");
            contentStream.endText();
            yPosition -= 30;
            
            // Add pie chart for task status
            BufferedImage pieChart = createTaskStatusPieChart(project);
            if (pieChart != null) {
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, 
                    imageToBytes(pieChart), "pie-chart");
                contentStream.drawImage(pdImage, MARGIN, yPosition - 200, 250, 200);
            }
        }
    }
    
    private void addTimelinePage(PDDocument document, Project project) throws IOException, SQLException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - MARGIN;
            
            // Header
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("PROJECT TIMELINE");
            contentStream.endText();
            yPosition -= 40;
            
            // Create timeline chart
            BufferedImage timelineChart = createTimelineChart(project);
            if (timelineChart != null) {
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, 
                    imageToBytes(timelineChart), "timeline-chart");
                contentStream.drawImage(pdImage, MARGIN, yPosition - 300, 500, 250);
                yPosition -= 320;
            }
            
            // Milestones section
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("KEY MILESTONES");
            contentStream.endText();
            yPosition -= 30;
            
            // List milestones (simplified - you'd get actual milestones from database)
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
            String[] milestones = {
                "• Project Kickoff - " + project.getStartDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                "• Phase 1 Completion - Expected " + project.getStartDate().plusDays(project.getDurationDays()/3).format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                "• Phase 2 Completion - Expected " + project.getStartDate().plusDays(2*project.getDurationDays()/3).format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                "• Project Completion - " + project.getEndDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            };
            
            for (String milestone : milestones) {
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN + 20, yPosition);
                contentStream.showText(milestone);
                contentStream.endText();
                yPosition -= 25;
            }
        }
    }
    
    private void addResourcesPage(PDDocument document, Project project) throws IOException, SQLException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - MARGIN;
            
            // Header
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("ASSIGNED RESOURCES");
            contentStream.endText();
            yPosition -= 40;
            
            // Get resources from database
            List<String> resources = getProjectResources(project);
            
            if (!resources.isEmpty()) {
                // Resource utilization chart
                BufferedImage resourceChart = createResourceUtilizationChart(project);
                if (resourceChart != null) {
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, 
                        imageToBytes(resourceChart), "resource-chart");
                    contentStream.drawImage(pdImage, MARGIN, yPosition - 250, 450, 200);
                    yPosition -= 270;
                }
                
                // List resources
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADING);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText("Team Members:");
                contentStream.endText();
                yPosition -= 25;
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
                for (String resource : resources) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN + 20, yPosition);
                    contentStream.showText("• " + resource);
                    contentStream.endText();
                    yPosition -= 20;
                }
            } else {
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText("No resources currently assigned to this project.");
                contentStream.endText();
            }
        }
    }
    
    private void addTasksSummaryPage(PDDocument document, Project project) throws IOException, SQLException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - MARGIN;
            
            // Header
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("TASKS SUMMARY");
            contentStream.endText();
            yPosition -= 40;
            
            // Get task statistics
            TaskStatistics stats = getTaskStatistics(project);
            
            // Summary boxes
            float boxWidth = 120;
            float boxHeight = 60;
            float spacing = 20;
            float startX = MARGIN;
            
            // Total tasks box
            drawStatBox(contentStream, startX, yPosition - boxHeight, boxWidth, boxHeight,
                       "Total Tasks", String.valueOf(stats.total), new Color(52, 152, 219));
            
            // Completed tasks box
            drawStatBox(contentStream, startX + boxWidth + spacing, yPosition - boxHeight, boxWidth, boxHeight,
                       "Completed", String.valueOf(stats.completed), new Color(46, 204, 113));
            
            // In progress tasks box
            drawStatBox(contentStream, startX + 2 * (boxWidth + spacing), yPosition - boxHeight, boxWidth, boxHeight,
                       "In Progress", String.valueOf(stats.inProgress), new Color(241, 196, 15));
            
            // Pending tasks box
            drawStatBox(contentStream, startX + 3 * (boxWidth + spacing), yPosition - boxHeight, boxWidth, boxHeight,
                       "Pending", String.valueOf(stats.pending), new Color(231, 76, 60));
            
            yPosition -= (boxHeight + 40);
            
            // Task completion chart
            BufferedImage taskChart = createTaskCompletionChart(stats);
            if (taskChart != null) {
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, 
                    imageToBytes(taskChart), "task-chart");
                contentStream.drawImage(pdImage, MARGIN, yPosition - 250, 400, 200);
            }
        }
    }
    
    private void addContactPage(PDDocument document, Project project) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - MARGIN;
            
            // Header
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("CONTACT INFORMATION");
            contentStream.endText();
            yPosition -= 40;
            
            // Client contact
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Client Contact:");
            contentStream.endText();
            yPosition -= 30;
            
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
            
            if (project.getContactName() != null) {
                yPosition = addLabelValue(contentStream, "Name:", project.getContactName(), MARGIN + 20, yPosition);
            }
            if (project.getContactRole() != null) {
                yPosition = addLabelValue(contentStream, "Role:", project.getContactRole(), MARGIN + 20, yPosition);
            }
            if (project.getContactCompany() != null) {
                yPosition = addLabelValue(contentStream, "Company:", project.getContactCompany(), MARGIN + 20, yPosition);
            }
            if (project.getContactEmail() != null) {
                yPosition = addLabelValue(contentStream, "Email:", project.getContactEmail(), MARGIN + 20, yPosition);
            }
            if (project.getContactPhone() != null) {
                yPosition = addLabelValue(contentStream, "Phone:", project.getContactPhone(), MARGIN + 20, yPosition);
            }
            
            yPosition -= 40;
            
            // Project manager contact (if available)
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADING);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Project Manager:");
            contentStream.endText();
            yPosition -= 30;
            
            // Footer
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, MARGIN);
            contentStream.showText("For questions about this report, please contact the project manager.");
            contentStream.endText();
        }
    }
    
    // Helper methods
    private Color getStatusColor(ProjectStatus status) {
        return switch (status) {
            case COMPLETED -> new Color(46, 204, 113);
            case ACTIVE -> new Color(52, 152, 219);
            case DELAYED -> new Color(231, 76, 60);
            case PLANNED -> new Color(155, 89, 182);
            case CANCELLED -> new Color(149, 165, 166);
        };
    }
    
    private void drawRectangle(PDPageContentStream contentStream, float x, float y, 
                               float width, float height, Color color) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.addRect(x, y, width, height);
        contentStream.fill();
        contentStream.setNonStrokingColor(Color.BLACK);
    }
    
    private void drawLine(PDPageContentStream contentStream, float x1, float y1, 
                         float x2, float y2, float width) throws IOException {
        contentStream.setLineWidth(width);
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }
    
    private void drawProgressBar(PDPageContentStream contentStream, float x, float y,
                                 float width, float height, int percentage) throws IOException {
        // Background
        drawRectangle(contentStream, x, y, width, height, new Color(236, 240, 241));
        
        // Progress
        float progressWidth = width * percentage / 100f;
        Color progressColor = percentage >= 75 ? new Color(46, 204, 113) :
                             percentage >= 50 ? new Color(241, 196, 15) :
                             new Color(231, 76, 60);
        drawRectangle(contentStream, x, y, progressWidth, height, progressColor);
        
        // Text
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.setNonStrokingColor(Color.WHITE);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + width/2 - 20, y + height/2 - 5);
        contentStream.showText(percentage + "%");
        contentStream.endText();
        contentStream.setNonStrokingColor(Color.BLACK);
    }
    
    private void drawStatBox(PDPageContentStream contentStream, float x, float y,
                             float width, float height, String label, String value,
                             Color color) throws IOException {
        // Box
        drawRectangle(contentStream, x, y, width, height, color);
        
        // Label
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        contentStream.setNonStrokingColor(Color.WHITE);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 10, y + height - 20);
        contentStream.showText(label);
        contentStream.endText();
        
        // Value
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 10, y + 10);
        contentStream.showText(value);
        contentStream.endText();
        contentStream.setNonStrokingColor(Color.BLACK);
    }
    
    private float addLabelValue(PDPageContentStream contentStream, String label, String value,
                                float x, float y) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label);
        contentStream.endText();
        
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 100, y);
        contentStream.showText(value);
        contentStream.endText();
        
        return y - 25;
    }
    
    private float getStringWidth(String text, PDType1Font font, float fontSize) throws IOException {
        return font.getStringWidth(text) * fontSize / 1000;
    }
    
    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
    
    // Chart creation methods
    private BufferedImage createTaskStatusPieChart(Project project) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Completed", 45);
        dataset.setValue("In Progress", 30);
        dataset.setValue("Pending", 25);
        
        JFreeChart chart = ChartFactory.createPieChart(
            "Task Status Distribution",
            dataset,
            true,
            false,
            false
        );
        
        return chart.createBufferedImage(400, 300);
    }
    
    private BufferedImage createTimelineChart(Project project) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Sample data - you'd get actual data from database
        dataset.addValue(100, "Planned", "Week 1");
        dataset.addValue(100, "Planned", "Week 2");
        dataset.addValue(100, "Planned", "Week 3");
        dataset.addValue(100, "Planned", "Week 4");
        
        dataset.addValue(100, "Actual", "Week 1");
        dataset.addValue(95, "Actual", "Week 2");
        dataset.addValue(85, "Actual", "Week 3");
        dataset.addValue(0, "Actual", "Week 4");
        
        JFreeChart chart = ChartFactory.createLineChart(
            "Project Timeline Progress",
            "Timeline",
            "Progress %",
            dataset
        );
        
        return chart.createBufferedImage(500, 300);
    }
    
    private BufferedImage createResourceUtilizationChart(Project project) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Sample data
        dataset.addValue(85, "Utilization %", "Resource 1");
        dataset.addValue(75, "Utilization %", "Resource 2");
        dataset.addValue(90, "Utilization %", "Resource 3");
        dataset.addValue(60, "Utilization %", "Resource 4");
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Resource Utilization",
            "Resources",
            "Utilization %",
            dataset
        );
        
        return chart.createBufferedImage(450, 250);
    }
    
    private BufferedImage createTaskCompletionChart(TaskStatistics stats) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        dataset.addValue(stats.completed, "Tasks", "Completed");
        dataset.addValue(stats.inProgress, "Tasks", "In Progress");
        dataset.addValue(stats.pending, "Tasks", "Pending");
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Task Completion Status",
            "Status",
            "Number of Tasks",
            dataset
        );
        
        return chart.createBufferedImage(400, 250);
    }
    
    // Database methods
    private int calculateProjectProgress(Project project) {
        // Simplified calculation - you'd get actual progress from task completion
        LocalDate today = LocalDate.now();
        if (today.isBefore(project.getStartDate())) return 0;
        if (today.isAfter(project.getEndDate())) return 100;
        
        long totalDays = project.getStartDate().until(project.getEndDate()).getDays();
        long elapsedDays = project.getStartDate().until(today).getDays();
        return (int) (elapsedDays * 100 / totalDays);
    }
    
    private List<String> getProjectResources(Project project) throws SQLException {
        List<String> resources = new ArrayList<>();
        String url = "jdbc:sqlite:" + dbPath;
        
        String sql = """
            SELECT DISTINCT r.name
            FROM resources r
            JOIN assignments a ON r.id = a.resource_id
            WHERE a.project_id = ?
            ORDER BY r.name
        """;
        
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, project.getId());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                resources.add(rs.getString("name"));
            }
        }
        
        return resources;
    }
    
    private TaskStatistics getTaskStatistics(Project project) throws SQLException {
        TaskStatistics stats = new TaskStatistics();
        String url = "jdbc:sqlite:" + dbPath;
        
        String sql = """
            SELECT 
                COUNT(*) as total,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
                SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress,
                SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending
            FROM tasks
            WHERE project_id = ?
        """;
        
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, project.getId());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                stats.total = rs.getInt("total");
                stats.completed = rs.getInt("completed");
                stats.inProgress = rs.getInt("in_progress");
                stats.pending = rs.getInt("pending");
            }
        }
        
        // If no tasks found, use sample data
        if (stats.total == 0) {
            stats.total = 20;
            stats.completed = 9;
            stats.inProgress = 6;
            stats.pending = 5;
        }
        
        return stats;
    }
    
    // Inner class for task statistics
    private static class TaskStatistics {
        int total = 0;
        int completed = 0;
        int inProgress = 0;
        int pending = 0;
    }
}