package com.subliminalsearch.simpleprojectresourcemanager.service;

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

public class GeographicReportService {
    private final SchedulingService schedulingService;
    
    public GeographicReportService(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }
    
    public File generateReport(boolean showHeatMap, boolean showTravel) throws IOException {
        File outputFile = new File(System.getProperty("java.io.tmpdir"), 
                                  "geographic_report_" + System.currentTimeMillis() + ".pdf");
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
                content.newLineAtOffset(100, 700);
                content.showText("Geographic Distribution Report");
                content.endText();
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                content.newLineAtOffset(100, 650);
                content.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
                content.endText();
                
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(100, 600);
                content.showText("Geographic analysis based on project locations:");
                content.newLineAtOffset(0, -30);
                content.showText("• Regional distribution analysis");
                if (showHeatMap) {
                    content.newLineAtOffset(0, -20);
                    content.showText("• Concentration heat map");
                }
                if (showTravel) {
                    content.newLineAtOffset(0, -20);
                    content.showText("• Travel time and cost analysis");
                }
                content.endText();
            }
            
            document.save(outputFile);
        }
        
        return outputFile;
    }
}