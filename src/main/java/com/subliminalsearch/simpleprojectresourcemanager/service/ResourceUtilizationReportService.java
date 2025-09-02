package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import java.time.DayOfWeek;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceUtilizationReportService {
    private static final Logger logger = LoggerFactory.getLogger(ResourceUtilizationReportService.class);
    private final SchedulingService schedulingService;
    private final UtilizationService utilizationService;
    
    public ResourceUtilizationReportService(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
        this.utilizationService = new UtilizationService(schedulingService.getDataSource());
    }
    
    public ResourceUtilizationReportService(SchedulingService schedulingService, UtilizationService utilizationService) {
        this.schedulingService = schedulingService;
        this.utilizationService = utilizationService;
    }
    
    public File generateReport(boolean includeCharts, boolean showDetails) throws IOException, SQLException {
        File outputFile = new File(System.getProperty("java.io.tmpdir"), 
                                  "resource_utilization_" + System.currentTimeMillis() + ".pdf");
        
        try (PDDocument document = new PDDocument()) {
            // Add pages
            addCoverPage(document);
            addUtilizationSummaryPage(document);
            
            if (includeCharts) {
                addUtilizationChartsPage(document);
            }
            
            if (showDetails) {
                addResourceDetailsPages(document);
            }
            
            addRecommendationsPage(document);
            
            document.save(outputFile);
        }
        
        return outputFile;
    }
    
    private void addCoverPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            // Title
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
            content.newLineAtOffset(100, 700);
            content.showText("Resource Utilization Report");
            content.endText();
            
            // Date
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
            content.newLineAtOffset(100, 650);
            content.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            content.endText();
            
            // Period
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();
            content.beginText();
            content.newLineAtOffset(100, 620);
            content.showText("Analysis Period: " + startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + 
                           " to " + endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            content.endText();
            
            // Executive Summary Box
            content.setStrokingColor(Color.GRAY);
            content.addRect(50, 400, 495, 180);
            content.stroke();
            
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            content.newLineAtOffset(60, 560);
            content.showText("Executive Summary");
            content.endText();
            
            // Calculate key metrics
            List<Resource> resources = schedulingService.getAllResources();
            List<Assignment> assignments = schedulingService.getAssignmentRepository().findAll();
            
            double avgUtilization = calculateAverageUtilization(resources, assignments, startDate, endDate);
            int overUtilized = countOverUtilizedResources(resources, assignments, startDate, endDate);
            int underUtilized = countUnderUtilizedResources(resources, assignments, startDate, endDate);
            
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(60, 530);
            content.showText("• Average Resource Utilization: " + String.format("%.1f%%", avgUtilization));
            content.newLineAtOffset(0, -20);
            content.showText("• Over-utilized Resources (>80%): " + overUtilized);
            content.newLineAtOffset(0, -20);
            content.showText("• Under-utilized Resources (<40%): " + underUtilized);
            content.newLineAtOffset(0, -20);
            content.showText("• Total Active Resources: " + resources.size());
            content.endText();
        }
    }
    
    private void addUtilizationSummaryPage(PDDocument document) throws IOException, SQLException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.newLineAtOffset(50, 750);
            content.showText("Resource Utilization Summary");
            content.endText();
            
            // Table headers
            float yPosition = 700;
            float[] columnWidths = {150, 100, 100, 100, 100};
            String[] headers = {"Resource", "Utilization %", "Hours Assigned", "Available Hours", "Status"};
            
            // Draw table header
            content.setStrokingColor(Color.BLACK);
            content.setLineWidth(1f);
            
            float xPosition = 50;
            for (int i = 0; i < headers.length; i++) {
                content.addRect(xPosition, yPosition - 25, columnWidths[i], 25);
                content.stroke();
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                content.newLineAtOffset(xPosition + 5, yPosition - 18);
                content.showText(headers[i]);
                content.endText();
                
                xPosition += columnWidths[i];
            }
            
            yPosition -= 25;
            
            // Add resource data
            List<Resource> resources = schedulingService.getAllResources();
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();
            
            for (Resource resource : resources) {
                if (yPosition < 100) {
                    // Add new page if needed
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    yPosition = 750;
                }
                
                List<Assignment> resourceAssignments = schedulingService.getAssignmentsByResource(resource.getId());
                double utilization = calculateResourceUtilization(resource, resourceAssignments, startDate, endDate);
                int hoursAssigned = calculateAssignedHours(resourceAssignments, startDate, endDate);
                int availableHours = calculateAvailableHours(startDate, endDate);
                String status = getUtilizationStatus(utilization);
                
                xPosition = 50;
                for (int i = 0; i < columnWidths.length; i++) {
                    content.addRect(xPosition, yPosition - 20, columnWidths[i], 20);
                    content.stroke();
                    
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                    content.newLineAtOffset(xPosition + 5, yPosition - 15);
                    
                    switch (i) {
                        case 0:
                            content.showText(resource.getName());
                            break;
                        case 1:
                            content.showText(String.format("%.1f%%", utilization));
                            break;
                        case 2:
                            content.showText(String.valueOf(hoursAssigned));
                            break;
                        case 3:
                            content.showText(String.valueOf(availableHours));
                            break;
                        case 4:
                            // Color-code status
                            if (status.equals("Over-utilized")) {
                                content.setNonStrokingColor(Color.RED);
                            } else if (status.equals("Under-utilized")) {
                                content.setNonStrokingColor(Color.ORANGE);
                            } else {
                                content.setNonStrokingColor(Color.GREEN);
                            }
                            content.showText(status);
                            content.setNonStrokingColor(Color.BLACK);
                            break;
                    }
                    content.endText();
                    
                    xPosition += columnWidths[i];
                }
                
                yPosition -= 20;
            }
        }
    }
    
    private void addUtilizationChartsPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.newLineAtOffset(50, 750);
            content.showText("Utilization Analysis Charts");
            content.endText();
            
            // Create utilization distribution pie chart
            DefaultPieDataset pieDataset = new DefaultPieDataset();
            List<Resource> resources = schedulingService.getAllResources();
            List<Assignment> assignments = schedulingService.getAssignmentRepository().findAll();
            LocalDate startDate = LocalDate.now().minusMonths(1);
            LocalDate endDate = LocalDate.now();
            
            int overUtilized = countOverUtilizedResources(resources, assignments, startDate, endDate);
            int optimal = countOptimalResources(resources, assignments, startDate, endDate);
            int underUtilized = countUnderUtilizedResources(resources, assignments, startDate, endDate);
            
            pieDataset.setValue("Over-utilized (>80%)", overUtilized);
            pieDataset.setValue("Optimal (40-80%)", optimal);
            pieDataset.setValue("Under-utilized (<40%)", underUtilized);
            
            JFreeChart pieChart = ChartFactory.createPieChart(
                "Resource Utilization Distribution",
                pieDataset,
                true,
                true,
                false
            );
            
            // Customize pie chart colors
            org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) pieChart.getPlot();
            plot.setSectionPaint("Over-utilized (>80%)", new Color(239, 83, 80));  // Red
            plot.setSectionPaint("Optimal (40-80%)", new Color(102, 187, 106));    // Green
            plot.setSectionPaint("Under-utilized (<40%)", new Color(255, 167, 38)); // Orange
            
            // Add pie chart to PDF
            BufferedImage pieImage = pieChart.createBufferedImage(450, 300);
            PDImageXObject pdPieImage = LosslessFactory.createFromImage(document, pieImage);
            content.drawImage(pdPieImage, 75, 420, 450, 300);
            
            // Create utilization trend bar chart
            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
            
            // Add weekly utilization data with current week
            for (int week = 4; week >= 0; week--) {
                LocalDate weekStart = LocalDate.now().minusWeeks(week);
                LocalDate weekEnd = weekStart.plusDays(6);
                double weekUtilization = calculateAverageUtilization(resources, assignments, weekStart, weekEnd);
                String weekLabel = week == 0 ? "Current Week" : "Week -" + week;
                barDataset.addValue(weekUtilization, "Utilization %", weekLabel);
            }
            
            JFreeChart barChart = ChartFactory.createBarChart(
                "5-Week Utilization Trend",
                "Week",
                "Utilization %",
                barDataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
            );
            
            // Customize bar chart
            org.jfree.chart.plot.CategoryPlot catPlot = barChart.getCategoryPlot();
            org.jfree.chart.renderer.category.BarRenderer renderer = 
                (org.jfree.chart.renderer.category.BarRenderer) catPlot.getRenderer();
            renderer.setSeriesPaint(0, new Color(66, 165, 245)); // Blue bars
            
            // Add bar chart to PDF
            BufferedImage barImage = barChart.createBufferedImage(450, 280);
            PDImageXObject pdBarImage = LosslessFactory.createFromImage(document, barImage);
            content.drawImage(pdBarImage, 75, 100, 450, 280);
        }
    }
    
    private void addResourceDetailsPages(PDDocument document) throws IOException {
        List<Resource> resources = schedulingService.getAllResources();
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        for (Resource resource : resources) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                content.newLineAtOffset(50, 750);
                content.showText("Resource Detail: " + resource.getName());
                content.endText();
                
                // Resource info
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 720);
                content.showText("Type: " + (resource.getResourceType() != null ? 
                                            resource.getResourceType().getName() : "Unspecified"));
                content.newLineAtOffset(0, -20);
                content.showText("Phone: " + (resource.getPhone() != null ? resource.getPhone() : "N/A"));
                content.newLineAtOffset(0, -20);
                content.showText("Email: " + (resource.getEmail() != null ? resource.getEmail() : "N/A"));
                content.endText();
                
                // Current assignments
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.newLineAtOffset(50, 640);
                content.showText("Current Assignments:");
                content.endText();
                
                List<Assignment> resourceAssignments = schedulingService.getAssignmentsByResource(resource.getId())
                    .stream()
                    .filter(a -> !a.getEndDate().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());
                
                float yPosition = 610;
                for (Assignment assignment : resourceAssignments) {
                    if (yPosition < 100) break; // Prevent overflow
                    
                    Project project = schedulingService.getAllProjects().stream()
                        .filter(p -> p.getId().equals(assignment.getProjectId()))
                        .findFirst()
                        .orElse(null);
                    
                    if (project != null) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                        content.newLineAtOffset(70, yPosition);
                        content.showText("• " + project.getProjectId() + " - " + project.getDescription());
                        content.newLineAtOffset(20, -15);
                        content.showText("  " + assignment.getStartDate() + " to " + assignment.getEndDate());
                        content.endText();
                        yPosition -= 35;
                    }
                }
                
                // Utilization metrics
                double utilization = calculateResourceUtilization(resource, resourceAssignments, startDate, endDate);
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.newLineAtOffset(50, yPosition - 20);
                content.showText("Utilization Metrics:");
                content.endText();
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(70, yPosition - 45);
                content.showText("Current Utilization: " + String.format("%.1f%%", utilization));
                content.newLineAtOffset(0, -20);
                content.showText("Status: " + getUtilizationStatus(utilization));
                content.endText();
            }
        }
    }
    
    private void addRecommendationsPage(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.newLineAtOffset(50, 750);
            content.showText("Recommendations");
            content.endText();
            
            List<String> recommendations = generateRecommendations();
            float yPosition = 710;
            
            for (String recommendation : recommendations) {
                if (yPosition < 100) {
                    // Add new page if needed
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    yPosition = 750;
                }
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(60, yPosition);
                
                // Wrap long text
                String[] words = recommendation.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (line.length() + word.length() > 80) {
                        content.showText(line.toString());
                        content.newLineAtOffset(0, -15);
                        line = new StringBuilder(word + " ");
                    } else {
                        line.append(word).append(" ");
                    }
                }
                if (line.length() > 0) {
                    content.showText(line.toString());
                }
                
                content.endText();
                yPosition -= 40;
            }
        }
    }
    
    // Utility methods
    private double calculateAverageUtilization(List<Resource> resources, List<Assignment> assignments,
                                              LocalDate startDate, LocalDate endDate) {
        if (resources.isEmpty()) return 0.0;
        
        double totalUtilization = 0;
        for (Resource resource : resources) {
            List<Assignment> resourceAssignments = assignments.stream()
                .filter(a -> a.getResourceId().equals(resource.getId()))
                .collect(Collectors.toList());
            totalUtilization += calculateResourceUtilization(resource, resourceAssignments, startDate, endDate);
        }
        
        return totalUtilization / resources.size();
    }
    
    private double calculateResourceUtilization(Resource resource, List<Assignment> assignments,
                                               LocalDate startDate, LocalDate endDate) {
        // Get available days based on utilization settings
        long availableDays = utilizationService.calculateAvailableDays(startDate, endDate);
        if (availableDays == 0) return 0;
        
        // Get company holidays for working days calculation
        // Note: For now we'll pass null for holidays. In production, these would be loaded from the database
        List<CompanyHoliday> holidays = null;
        
        // Calculate assigned days considering utilization settings
        long assignedDays = 0;
        for (Assignment assignment : assignments) {
            LocalDate assignStart = assignment.getStartDate().isBefore(startDate) ? 
                                   startDate : assignment.getStartDate();
            LocalDate assignEnd = assignment.getEndDate().isAfter(endDate) ? 
                                 endDate : assignment.getEndDate();
            
            if (!assignStart.isAfter(endDate) && !assignEnd.isBefore(startDate)) {
                // Get the project to check if it counts as utilized
                try {
                    Optional<Project> projectOpt = schedulingService.getProjectById(assignment.getProjectId());
                    if (projectOpt.isPresent() && utilizationService.countsAsUtilized(projectOpt.get().getProjectId())) {
                        // Count working days if configured, otherwise calendar days
                        if (utilizationService.getSettings().getCalculationMethod() == 
                            UtilizationSettings.CalculationMethod.WORKING_DAYS) {
                            assignedDays += utilizationService.calculateWorkingDays(assignStart, assignEnd, holidays);
                        } else {
                            assignedDays += ChronoUnit.DAYS.between(assignStart, assignEnd) + 1;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not load project for assignment " + assignment.getId(), e);
                    // Fall back to counting the days
                    assignedDays += ChronoUnit.DAYS.between(assignStart, assignEnd) + 1;
                }
            }
        }
        
        return (assignedDays * 100.0) / availableDays;
    }
    
    private int calculateAssignedHours(List<Assignment> assignments, LocalDate startDate, LocalDate endDate) {
        double totalHours = 0;
        
        // Get company holidays for working days calculation
        // Note: For now we'll pass null for holidays. In production, these would be loaded from the database
        List<CompanyHoliday> holidays = null;
        
        for (Assignment assignment : assignments) {
            if (!assignment.getStartDate().isAfter(endDate) && !assignment.getEndDate().isBefore(startDate)) {
                LocalDate effectiveStart = assignment.getStartDate().isBefore(startDate) ? 
                                          startDate : assignment.getStartDate();
                LocalDate effectiveEnd = assignment.getEndDate().isAfter(endDate) ? 
                                        endDate : assignment.getEndDate();
                
                // Use working days if configured
                if (utilizationService.getSettings().getCalculationMethod() == 
                    UtilizationSettings.CalculationMethod.WORKING_DAYS) {
                    long days = utilizationService.calculateWorkingDays(effectiveStart, effectiveEnd, holidays);
                    totalHours += days * utilizationService.getSettings().getHoursPerDay();
                } else {
                    int days = (int) ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
                    totalHours += days * utilizationService.getSettings().getHoursPerDay();
                }
            }
        }
        return (int) totalHours;
    }
    
    private int calculateAvailableHours(LocalDate startDate, LocalDate endDate) {
        return (int) utilizationService.calculateAvailableHours(startDate, endDate);
    }
    
    private String getUtilizationStatus(double utilization) {
        UtilizationSettings settings = utilizationService.getSettings();
        if (utilization >= settings.getOverallocationAlert()) return "Over-allocated";
        if (utilization >= settings.getTargetUtilization()) return "Optimal";
        if (utilization >= settings.getMinimumUtilization()) return "Below Target";
        return "Under-utilized";
    }
    
    private int countOverUtilizedResources(List<Resource> resources, List<Assignment> assignments,
                                          LocalDate startDate, LocalDate endDate) {
        int count = 0;
        for (Resource resource : resources) {
            List<Assignment> resourceAssignments = assignments.stream()
                .filter(a -> a.getResourceId().equals(resource.getId()))
                .collect(Collectors.toList());
            if (calculateResourceUtilization(resource, resourceAssignments, startDate, endDate) > 80) {
                count++;
            }
        }
        return count;
    }
    
    private int countOptimalResources(List<Resource> resources, List<Assignment> assignments,
                                     LocalDate startDate, LocalDate endDate) {
        int count = 0;
        for (Resource resource : resources) {
            List<Assignment> resourceAssignments = assignments.stream()
                .filter(a -> a.getResourceId().equals(resource.getId()))
                .collect(Collectors.toList());
            double utilization = calculateResourceUtilization(resource, resourceAssignments, startDate, endDate);
            if (utilization >= 40 && utilization <= 80) {
                count++;
            }
        }
        return count;
    }
    
    private int countUnderUtilizedResources(List<Resource> resources, List<Assignment> assignments,
                                           LocalDate startDate, LocalDate endDate) {
        int count = 0;
        for (Resource resource : resources) {
            List<Assignment> resourceAssignments = assignments.stream()
                .filter(a -> a.getResourceId().equals(resource.getId()))
                .collect(Collectors.toList());
            if (calculateResourceUtilization(resource, resourceAssignments, startDate, endDate) < 40) {
                count++;
            }
        }
        return count;
    }
    
    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();
        List<Resource> resources = schedulingService.getAllResources();
        List<Assignment> assignments = schedulingService.getAssignmentRepository().findAll();
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        int overUtilized = countOverUtilizedResources(resources, assignments, startDate, endDate);
        int underUtilized = countUnderUtilizedResources(resources, assignments, startDate, endDate);
        
        if (overUtilized > 0) {
            recommendations.add("• " + overUtilized + " resources are over-utilized (>80%). " +
                              "Consider redistributing workload or hiring additional resources.");
        }
        
        if (underUtilized > 0) {
            recommendations.add("• " + underUtilized + " resources are under-utilized (<40%). " +
                              "Consider assigning additional projects or optimizing resource allocation.");
        }
        
        double avgUtilization = calculateAverageUtilization(resources, assignments, startDate, endDate);
        if (avgUtilization > 75) {
            recommendations.add("• Overall utilization is high (" + String.format("%.1f%%", avgUtilization) + "). " +
                              "Plan for capacity expansion to handle future growth.");
        } else if (avgUtilization < 50) {
            recommendations.add("• Overall utilization is low (" + String.format("%.1f%%", avgUtilization) + "). " +
                              "Focus on business development to increase project pipeline.");
        }
        
        recommendations.add("• Review travel time allocations to optimize on-site efficiency.");
        recommendations.add("• Consider skill development programs for under-utilized resources.");
        recommendations.add("• Implement resource leveling to balance workload across teams.");
        
        return recommendations;
    }
}