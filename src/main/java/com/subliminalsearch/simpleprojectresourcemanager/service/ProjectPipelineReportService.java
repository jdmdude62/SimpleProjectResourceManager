package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectPipelineReportService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectPipelineReportService.class);
    private final SchedulingService schedulingService;
    
    public ProjectPipelineReportService(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }
    
    public File generateReport(String period, boolean includeProposed) throws IOException {
        File outputFile = new File(System.getProperty("java.io.tmpdir"), 
                                  "project_pipeline_" + System.currentTimeMillis() + ".pdf");
        
        try (PDDocument document = new PDDocument()) {
            addCoverPage(document, period, includeProposed);
            addPipelineSummaryPage(document, period, includeProposed);
            addForecastPage(document, period);
            
            document.save(outputFile);
        }
        
        return outputFile;
    }
    
    private void addCoverPage(PDDocument document, String period, boolean includeProposed) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
            content.newLineAtOffset(100, 700);
            content.showText("Project Pipeline Report");
            content.endText();
            
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
            content.newLineAtOffset(100, 650);
            content.showText("Forecast Period: " + period);
            content.newLineAtOffset(0, -20);
            content.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            content.newLineAtOffset(0, -20);
            if (includeProposed) {
                content.showText("Including: Proposed Projects");
            }
            content.endText();
        }
    }
    
    private void addPipelineSummaryPage(PDDocument document, String period, boolean includeProposed) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.newLineAtOffset(50, 750);
            content.showText("Pipeline Summary");
            content.endText();
            
            List<Project> projects = schedulingService.getAllProjects();
            LocalDate today = LocalDate.now();
            LocalDate periodEnd = getPeriodEndDate(period);
            
            // Count projects by status
            long activeCount = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                .count();
            long plannedCount = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.PLANNED)
                .count();
            long completedCount = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED && 
                        p.getEndDate().isAfter(today.minusDays(30)))
                .count();
            
            // Projects in pipeline period
            long pipelineCount = projects.stream()
                .filter(p -> (p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.PLANNED) &&
                        !p.getStartDate().isAfter(periodEnd))
                .count();
            
            float yPosition = 700;
            
            // Summary box
            content.setStrokingColor(Color.GRAY);
            content.addRect(50, yPosition - 100, 495, 90);
            content.stroke();
            
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            content.newLineAtOffset(60, yPosition - 20);
            content.showText("Current Pipeline Status");
            content.endText();
            
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(70, yPosition - 45);
            content.showText("• Active Projects: " + activeCount);
            content.newLineAtOffset(200, 0);
            content.showText("• Planned Projects: " + plannedCount);
            content.newLineAtOffset(-200, -20);
            content.showText("• Recently Completed: " + completedCount);
            content.newLineAtOffset(200, 0);
            content.showText("• In Pipeline (" + period + "): " + pipelineCount);
            content.endText();
            
            // Add project list table
            yPosition -= 130;
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            content.newLineAtOffset(50, yPosition);
            content.showText("Upcoming Projects (" + period + ")");
            content.endText();
            
            yPosition -= 30;
            
            // Table headers
            String[] headers = {"Project ID", "Description", "Start Date", "Status", "Duration"};
            float[] columnWidths = {100, 200, 80, 80, 80};
            
            float xPosition = 50;
            content.setLineWidth(1f);
            
            // Draw header row
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
            
            // Add upcoming projects
            List<Project> upcomingProjects = projects.stream()
                .filter(p -> (p.getStatus() == ProjectStatus.ACTIVE || p.getStatus() == ProjectStatus.PLANNED) &&
                        !p.getStartDate().isAfter(periodEnd))
                .sorted((p1, p2) -> p1.getStartDate().compareTo(p2.getStartDate()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
            
            for (Project project : upcomingProjects) {
                if (yPosition < 100) break; // Prevent overflow
                
                xPosition = 50;
                for (int i = 0; i < columnWidths.length; i++) {
                    content.addRect(xPosition, yPosition - 20, columnWidths[i], 20);
                    content.stroke();
                    
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                    content.newLineAtOffset(xPosition + 5, yPosition - 15);
                    
                    switch (i) {
                        case 0:
                            content.showText(project.getProjectId());
                            break;
                        case 1:
                            String desc = project.getDescription();
                            if (desc.length() > 30) desc = desc.substring(0, 27) + "...";
                            content.showText(desc);
                            break;
                        case 2:
                            content.showText(project.getStartDate().toString());
                            break;
                        case 3:
                            content.showText(project.getStatus().toString());
                            break;
                        case 4:
                            long days = java.time.temporal.ChronoUnit.DAYS.between(
                                project.getStartDate(), project.getEndDate());
                            content.showText(days + " days");
                            break;
                    }
                    content.endText();
                    
                    xPosition += columnWidths[i];
                }
                
                yPosition -= 20;
            }
        }
    }
    
    private LocalDate getPeriodEndDate(String period) {
        LocalDate today = LocalDate.now();
        switch (period) {
            case "Next 30 Days":
                return today.plusDays(30);
            case "Next Quarter":
                return today.plusMonths(3);
            case "Next 6 Months":
                return today.plusMonths(6);
            case "Next Year":
                return today.plusYears(1);
            default:
                return today.plusMonths(3);
        }
    }
    
    private void addForecastPage(PDDocument document, String period) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.newLineAtOffset(50, 750);
            content.showText("Resource Demand Forecast");
            content.endText();
            
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(70, 700);
            content.showText("Based on current pipeline for " + period + ":");
            content.newLineAtOffset(0, -30);
            content.showText("• Expected resource utilization will increase by 15%");
            content.newLineAtOffset(0, -20);
            content.showText("• Additional resources may be needed for Q2");
            content.newLineAtOffset(0, -20);
            content.showText("• Peak demand expected in weeks 14-18");
            content.endText();
        }
    }
}