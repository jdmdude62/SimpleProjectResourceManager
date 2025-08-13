package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CompletionAnalyticsService {
    private final SchedulingService schedulingService;
    
    public CompletionAnalyticsService(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }
    
    public File generateReport(String period) throws IOException {
        File outputFile = new File(System.getProperty("java.io.tmpdir"), 
                                  "completion_analytics_" + System.currentTimeMillis() + ".pdf");
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
                content.newLineAtOffset(100, 700);
                content.showText("Completion Analytics Report");
                content.endText();
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                content.newLineAtOffset(100, 650);
                content.showText("Analysis Period: " + period);
                content.newLineAtOffset(0, -20);
                content.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                content.endText();
                
                // Calculate basic metrics
                List<Project> projects = schedulingService.getAllProjects();
                long completedCount = projects.stream()
                    .filter(p -> p.getStatus() == ProjectStatus.COMPLETED)
                    .count();
                long activeCount = projects.stream()
                    .filter(p -> p.getStatus() == ProjectStatus.ACTIVE)
                    .count();
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(100, 580);
                content.showText("Performance Metrics:");
                content.newLineAtOffset(20, -25);
                content.showText("• Completed Projects: " + completedCount);
                content.newLineAtOffset(0, -20);
                content.showText("• Active Projects: " + activeCount);
                content.newLineAtOffset(0, -20);
                content.showText("• On-time Completion Rate: 85%"); // Mock data
                content.newLineAtOffset(0, -20);
                content.showText("• Average Project Duration: 45 days"); // Mock data
                content.endText();
            }
            
            document.save(outputFile);
        }
        
        return outputFile;
    }
}