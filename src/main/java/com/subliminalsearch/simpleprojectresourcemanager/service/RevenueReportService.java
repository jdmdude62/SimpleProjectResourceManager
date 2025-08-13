package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
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
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class RevenueReportService {
    private static final Logger logger = LoggerFactory.getLogger(RevenueReportService.class);
    private final SchedulingService schedulingService;
    
    public RevenueReportService(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }
    
    public File generateReport() throws IOException {
        return generateReport(LocalDate.now().minusMonths(1), LocalDate.now());
    }
    
    public File generateReport(LocalDate startDate, LocalDate endDate) throws IOException {
        File outputFile = new File(System.getProperty("java.io.tmpdir"), 
                                  "revenue_budget_" + System.currentTimeMillis() + ".pdf");
        
        try (PDDocument document = new PDDocument()) {
            // Add pages
            addCoverPage(document, startDate, endDate);
            addExecutiveSummaryPage(document, startDate, endDate);
            addProjectFinancialsPage(document, startDate, endDate);
            addCostBreakdownPage(document, startDate, endDate);
            addProfitabilityAnalysisPage(document, startDate, endDate);
            addBudgetVariancePage(document, startDate, endDate);
            
            document.save(outputFile);
        }
        
        return outputFile;
    }
    
    private void addCoverPage(PDDocument document, LocalDate startDate, LocalDate endDate) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float centerX = pageWidth / 2;
            
            // Title
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 28);
            content.beginText();
            content.newLineAtOffset(100, pageHeight - 100);
            content.showText("REVENUE & BUDGET");
            content.endText();
            
            content.beginText();
            content.newLineAtOffset(100, pageHeight - 130);
            content.showText("TRACKING REPORT");
            content.endText();
            
            // Period
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 16);
            content.beginText();
            content.newLineAtOffset(100, pageHeight - 200);
            content.showText("Reporting Period:");
            content.endText();
            
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            content.beginText();
            content.newLineAtOffset(100, pageHeight - 225);
            content.showText(startDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + " - " + 
                           endDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
            content.endText();
            
            // Generated date
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.beginText();
            content.newLineAtOffset(100, 50);
            content.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
            content.endText();
        }
    }
    
    private void addExecutiveSummaryPage(PDDocument document, LocalDate startDate, LocalDate endDate) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - 50;
            
            // Header
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
            content.beginText();
            content.newLineAtOffset(50, yPosition);
            content.showText("EXECUTIVE SUMMARY");
            content.endText();
            yPosition -= 40;
            
            // Get financial metrics with date filtering
            List<Project> projects = schedulingService.getAllProjects().stream()
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .filter(p -> {
                    // Include project if it overlaps with the selected date range
                    return !p.getEndDate().isBefore(startDate) && !p.getStartDate().isAfter(endDate);
                })
                .collect(java.util.stream.Collectors.toList());
            
            double totalRevenue = projects.stream()
                .mapToDouble(p -> p.getRevenueAmount() != null ? p.getRevenueAmount() : 0)
                .sum();
            
            double totalBudget = projects.stream()
                .mapToDouble(p -> p.getBudgetAmount() != null ? p.getBudgetAmount() : 0)
                .sum();
            
            double totalCost = projects.stream()
                .mapToDouble(p -> p.getTotalCost() != null ? p.getTotalCost() : 0)
                .sum();
            
            double totalProfit = totalRevenue - totalCost;
            double profitMargin = totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0;
            
            // Key metrics boxes
            float boxWidth = 240;
            float boxHeight = 80;
            float boxX = 50;
            
            // Revenue box
            drawMetricBox(content, boxX, yPosition - boxHeight, boxWidth, boxHeight,
                         "Total Revenue", String.format("$%,.0f", totalRevenue),
                         new Color(34, 139, 34));
            
            // Cost box
            drawMetricBox(content, boxX + boxWidth + 20, yPosition - boxHeight, boxWidth, boxHeight,
                         "Total Costs", String.format("$%,.0f", totalCost),
                         new Color(220, 53, 69));
            
            yPosition -= (boxHeight + 20);
            
            // Profit box
            drawMetricBox(content, boxX, yPosition - boxHeight, boxWidth, boxHeight,
                         "Net Profit", String.format("$%,.0f", totalProfit),
                         totalProfit >= 0 ? new Color(34, 139, 34) : new Color(220, 53, 69));
            
            // Margin box
            drawMetricBox(content, boxX + boxWidth + 20, yPosition - boxHeight, boxWidth, boxHeight,
                         "Profit Margin", String.format("%.1f%%", profitMargin),
                         profitMargin >= 20 ? new Color(34, 139, 34) : 
                         profitMargin >= 10 ? new Color(255, 193, 7) : new Color(220, 53, 69));
            
            yPosition -= (boxHeight + 40);
            
            // Get total count for context
            long totalProjectsWithFinancials = schedulingService.getAllProjects().stream()
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .count();
            
            // Summary text
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.beginText();
            content.newLineAtOffset(50, yPosition);
            content.showText("Analysis Period: " + projects.size() + " of " + totalProjectsWithFinancials + 
                           " projects with financial data (filtered by date range)");
            content.newLineAtOffset(0, -20);
            content.showText("Average Project Revenue: $" + String.format("%,.0f", totalRevenue / Math.max(1, projects.size())));
            content.newLineAtOffset(0, -20);
            content.showText("Average Project Cost: $" + String.format("%,.0f", totalCost / Math.max(1, projects.size())));
            content.endText();
            
            // Add pie chart for cost breakdown
            if (totalCost > 0) {
                BufferedImage pieChart = createCostBreakdownPieChart(projects);
                if (pieChart != null) {
                    PDImageXObject pdImage = LosslessFactory.createFromImage(document, pieChart);
                    content.drawImage(pdImage, 100, yPosition - 300, 400, 250);
                }
            }
        }
    }
    
    private void addProjectFinancialsPage(PDDocument document, LocalDate startDate, LocalDate endDate) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - 50;
            
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.beginText();
            content.newLineAtOffset(50, yPosition);
            content.showText("PROJECT FINANCIALS");
            content.endText();
            yPosition -= 40;
            
            // Table headers
            String[] headers = {"Project ID", "Revenue", "Budget", "Actual Cost", "Profit", "Margin %"};
            float[] columnWidths = {120, 80, 80, 80, 80, 60};
            
            drawTableHeader(content, 50, yPosition, headers, columnWidths);
            yPosition -= 25;
            
            // Get projects with financial data with date filtering
            List<Project> projects = schedulingService.getAllProjects().stream()
                .filter(p -> p.getBudgetAmount() != null || p.getRevenueAmount() != null)
                .filter(p -> !p.getEndDate().isBefore(startDate) && !p.getStartDate().isAfter(endDate))
                .sorted((p1, p2) -> {
                    Double r1 = p1.getRevenueAmount() != null ? p1.getRevenueAmount() : 0.0;
                    Double r2 = p2.getRevenueAmount() != null ? p2.getRevenueAmount() : 0.0;
                    return r2.compareTo(r1); // Sort by revenue descending
                })
                .limit(20) // Show top 20 projects
                .collect(java.util.stream.Collectors.toList());
            
            // Add project rows
            for (Project project : projects) {
                if (yPosition < 100) {
                    // Add new page if needed
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    yPosition = pageHeight - 50;
                }
                
                Double revenue = project.getRevenueAmount();
                Double budget = project.getBudgetAmount();
                Double cost = project.getTotalCost();
                Double profit = revenue != null && cost != null ? revenue - cost : null;
                Double margin = project.getProfitMargin();
                
                String[] rowData = {
                    project.getProjectId(),
                    revenue != null ? String.format("$%,.0f", revenue) : "-",
                    budget != null ? String.format("$%,.0f", budget) : "-",
                    cost != null ? String.format("$%,.0f", cost) : "-",
                    profit != null ? String.format("$%,.0f", profit) : "-",
                    margin != null ? String.format("%.1f%%", margin) : "-"
                };
                
                drawTableRow(content, 50, yPosition, rowData, columnWidths);
                yPosition -= 20;
            }
        }
    }
    
    private void addCostBreakdownPage(PDDocument document, LocalDate startDate, LocalDate endDate) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - 50;
            
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.beginText();
            content.newLineAtOffset(50, yPosition);
            content.showText("COST BREAKDOWN ANALYSIS");
            content.endText();
            yPosition -= 40;
            
            // Calculate totals by cost category with date filtering
            List<Project> projects = schedulingService.getAllProjects().stream()
                .filter(p -> p.getTotalCost() != null && p.getTotalCost() > 0)
                .filter(p -> !p.getEndDate().isBefore(startDate) && !p.getStartDate().isAfter(endDate))
                .collect(java.util.stream.Collectors.toList());
            
            double totalLabor = projects.stream()
                .mapToDouble(p -> p.getLaborCost() != null ? p.getLaborCost() : 0)
                .sum();
            
            double totalMaterial = projects.stream()
                .mapToDouble(p -> p.getMaterialCost() != null ? p.getMaterialCost() : 0)
                .sum();
            
            double totalTravel = projects.stream()
                .mapToDouble(p -> p.getTravelCost() != null ? p.getTravelCost() : 0)
                .sum();
            
            double totalOther = projects.stream()
                .mapToDouble(p -> p.getOtherCost() != null ? p.getOtherCost() : 0)
                .sum();
            
            double grandTotal = totalLabor + totalMaterial + totalTravel + totalOther;
            
            // Cost category boxes
            float boxWidth = 120;
            float boxHeight = 100;
            float startX = 50;
            
            drawCostCategoryBox(content, startX, yPosition - boxHeight, boxWidth, boxHeight,
                               "Labor", totalLabor, grandTotal, new Color(52, 152, 219));
            
            drawCostCategoryBox(content, startX + boxWidth + 15, yPosition - boxHeight, boxWidth, boxHeight,
                               "Materials", totalMaterial, grandTotal, new Color(46, 204, 113));
            
            drawCostCategoryBox(content, startX + 2 * (boxWidth + 15), yPosition - boxHeight, boxWidth, boxHeight,
                               "Travel", totalTravel, grandTotal, new Color(241, 196, 15));
            
            drawCostCategoryBox(content, startX + 3 * (boxWidth + 15), yPosition - boxHeight, boxWidth, boxHeight,
                               "Other", totalOther, grandTotal, new Color(155, 89, 182));
            
            yPosition -= (boxHeight + 40);
            
            // Add bar chart
            BufferedImage barChart = createCostTrendBarChart(projects);
            if (barChart != null) {
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, barChart);
                content.drawImage(pdImage, 50, yPosition - 300, 500, 250);
            }
        }
    }
    
    private void addProfitabilityAnalysisPage(PDDocument document, LocalDate startDate, LocalDate endDate) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - 50;
            
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.beginText();
            content.newLineAtOffset(50, yPosition);
            content.showText("PROFITABILITY ANALYSIS");
            content.endText();
            yPosition -= 40;
            
            // Get and sort projects by profit margin with date filtering
            List<Project> projects = schedulingService.getAllProjects().stream()
                .filter(p -> p.getProfitMargin() != null)
                .filter(p -> !p.getEndDate().isBefore(startDate) && !p.getStartDate().isAfter(endDate))
                .sorted((p1, p2) -> {
                    Double m1 = p1.getProfitMargin();
                    Double m2 = p2.getProfitMargin();
                    return m2.compareTo(m1);
                })
                .collect(java.util.stream.Collectors.toList());
            
            if (!projects.isEmpty()) {
                // Top performers
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.beginText();
                content.newLineAtOffset(50, yPosition);
                content.showText("Top Performing Projects:");
                content.endText();
                yPosition -= 25;
                
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                for (int i = 0; i < Math.min(5, projects.size()); i++) {
                    Project p = projects.get(i);
                    content.beginText();
                    content.newLineAtOffset(70, yPosition);
                    content.showText(String.format("%d. %s - Margin: %.1f%% (Revenue: $%,.0f, Cost: $%,.0f)",
                        i + 1, p.getProjectId(), p.getProfitMargin(),
                        p.getRevenueAmount(), p.getTotalCost()));
                    content.endText();
                    yPosition -= 20;
                }
                
                yPosition -= 20;
                
                // Projects needing attention (lowest margins)
                if (projects.size() > 5) {
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                    content.beginText();
                    content.newLineAtOffset(50, yPosition);
                    content.showText("Projects Needing Attention:");
                    content.endText();
                    yPosition -= 25;
                    
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    for (int i = Math.max(0, projects.size() - 5); i < projects.size(); i++) {
                        Project p = projects.get(i);
                        if (p.getProfitMargin() < 15) { // Only show if margin is concerning
                            content.beginText();
                            content.newLineAtOffset(70, yPosition);
                            content.showText(String.format("• %s - Margin: %.1f%% (Revenue: $%,.0f, Cost: $%,.0f)",
                                p.getProjectId(), p.getProfitMargin(),
                                p.getRevenueAmount(), p.getTotalCost()));
                            content.endText();
                            yPosition -= 20;
                        }
                    }
                }
                
                // Add profitability chart
                BufferedImage profitChart = createProfitabilityChart(projects);
                if (profitChart != null) {
                    PDImageXObject pdImage = LosslessFactory.createFromImage(document, profitChart);
                    content.drawImage(pdImage, 50, yPosition - 280, 500, 250);
                }
            }
        }
    }
    
    private void addBudgetVariancePage(PDDocument document, LocalDate startDate, LocalDate endDate) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            float pageHeight = page.getMediaBox().getHeight();
            float yPosition = pageHeight - 50;
            
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
            content.beginText();
            content.newLineAtOffset(50, yPosition);
            content.showText("BUDGET VARIANCE REPORT");
            content.endText();
            yPosition -= 40;
            
            // Get projects with budget variance with date filtering
            List<Project> projects = schedulingService.getAllProjects().stream()
                .filter(p -> p.getBudgetVariance() != null)
                .filter(p -> !p.getEndDate().isBefore(startDate) && !p.getStartDate().isAfter(endDate))
                .sorted((p1, p2) -> {
                    // Sort by variance amount (most over budget first)
                    Double v1 = p1.getBudgetVariance();
                    Double v2 = p2.getBudgetVariance();
                    return v1.compareTo(v2);
                })
                .collect(java.util.stream.Collectors.toList());
            
            // Summary statistics
            long overBudget = projects.stream().filter(p -> p.getBudgetVariance() < 0).count();
            long underBudget = projects.stream().filter(p -> p.getBudgetVariance() > 0).count();
            long onBudget = projects.stream().filter(p -> Math.abs(p.getBudgetVariance()) < 1000).count();
            
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.beginText();
            content.newLineAtOffset(50, yPosition);
            content.showText("Projects Over Budget: " + overBudget);
            content.newLineAtOffset(200, 0);
            content.showText("Projects Under Budget: " + underBudget);
            content.newLineAtOffset(200, 0);
            content.showText("Projects On Budget: " + onBudget);
            content.endText();
            yPosition -= 40;
            
            // Projects over budget
            if (overBudget > 0) {
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.setNonStrokingColor(new Color(220, 53, 69));
                content.beginText();
                content.newLineAtOffset(50, yPosition);
                content.showText("Projects Over Budget:");
                content.endText();
                content.setNonStrokingColor(Color.BLACK);
                yPosition -= 25;
                
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                for (Project p : projects) {
                    if (p.getBudgetVariance() < -1000 && yPosition > 100) {
                        content.beginText();
                        content.newLineAtOffset(70, yPosition);
                        content.showText(String.format("%s - Over by $%,.0f (Budget: $%,.0f, Actual: $%,.0f)",
                            p.getProjectId(), Math.abs(p.getBudgetVariance()),
                            p.getBudgetAmount(), p.getTotalCost()));
                        content.endText();
                        yPosition -= 20;
                    }
                }
            }
            
            yPosition -= 20;
            
            // Projects under budget
            if (underBudget > 0 && yPosition > 200) {
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                content.setNonStrokingColor(new Color(34, 139, 34));
                content.beginText();
                content.newLineAtOffset(50, yPosition);
                content.showText("Projects Under Budget:");
                content.endText();
                content.setNonStrokingColor(Color.BLACK);
                yPosition -= 25;
                
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                int count = 0;
                for (int i = projects.size() - 1; i >= 0 && count < 5 && yPosition > 100; i--) {
                    Project p = projects.get(i);
                    if (p.getBudgetVariance() > 1000) {
                        content.beginText();
                        content.newLineAtOffset(70, yPosition);
                        content.showText(String.format("%s - Under by $%,.0f (Budget: $%,.0f, Actual: $%,.0f)",
                            p.getProjectId(), p.getBudgetVariance(),
                            p.getBudgetAmount(), p.getTotalCost()));
                        content.endText();
                        yPosition -= 20;
                        count++;
                    }
                }
            }
        }
    }
    
    // Helper methods
    private void drawMetricBox(PDPageContentStream content, float x, float y, float width, float height,
                               String label, String value, Color color) throws IOException {
        // Draw box
        content.setNonStrokingColor(color);
        content.addRect(x, y, width, height);
        content.fill();
        
        // Draw label
        content.setNonStrokingColor(Color.WHITE);
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.beginText();
        content.newLineAtOffset(x + 10, y + height - 25);
        content.showText(label);
        content.endText();
        
        // Draw value
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
        content.beginText();
        content.newLineAtOffset(x + 10, y + 15);
        content.showText(value);
        content.endText();
        
        content.setNonStrokingColor(Color.BLACK);
    }
    
    private void drawCostCategoryBox(PDPageContentStream content, float x, float y, float width, float height,
                                     String category, double amount, double total, Color color) throws IOException {
        double percentage = total > 0 ? (amount / total) * 100 : 0;
        
        // Draw box
        content.setNonStrokingColor(color);
        content.addRect(x, y, width, height);
        content.fill();
        
        // Category name
        content.setNonStrokingColor(Color.WHITE);
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        content.beginText();
        content.newLineAtOffset(x + 10, y + height - 20);
        content.showText(category);
        content.endText();
        
        // Amount
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
        content.beginText();
        content.newLineAtOffset(x + 10, y + height - 45);
        content.showText(String.format("$%,.0f", amount));
        content.endText();
        
        // Percentage
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
        content.beginText();
        content.newLineAtOffset(x + 10, y + 10);
        content.showText(String.format("%.1f%%", percentage));
        content.endText();
        
        content.setNonStrokingColor(Color.BLACK);
    }
    
    private void drawTableHeader(PDPageContentStream content, float x, float y,
                                 String[] headers, float[] widths) throws IOException {
        content.setLineWidth(1f);
        float currentX = x;
        
        for (int i = 0; i < headers.length; i++) {
            // Draw cell
            content.addRect(currentX, y - 25, widths[i], 25);
            content.stroke();
            
            // Draw header text
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
            content.beginText();
            content.newLineAtOffset(currentX + 5, y - 18);
            content.showText(headers[i]);
            content.endText();
            
            currentX += widths[i];
        }
    }
    
    private void drawTableRow(PDPageContentStream content, float x, float y,
                             String[] data, float[] widths) throws IOException {
        content.setLineWidth(0.5f);
        float currentX = x;
        
        for (int i = 0; i < data.length; i++) {
            // Draw cell
            content.addRect(currentX, y - 20, widths[i], 20);
            content.stroke();
            
            // Draw cell text
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
            content.beginText();
            content.newLineAtOffset(currentX + 5, y - 15);
            
            // Truncate if needed
            String text = data[i];
            if (text.length() > 15 && widths[i] < 100) {
                text = text.substring(0, 12) + "...";
            }
            content.showText(text);
            content.endText();
            
            currentX += widths[i];
        }
    }
    
    private BufferedImage createCostBreakdownPieChart(List<Project> projects) {
        try {
            DefaultPieDataset dataset = new DefaultPieDataset();
            
            double totalLabor = projects.stream()
                .mapToDouble(p -> p.getLaborCost() != null ? p.getLaborCost() : 0).sum();
            double totalMaterial = projects.stream()
                .mapToDouble(p -> p.getMaterialCost() != null ? p.getMaterialCost() : 0).sum();
            double totalTravel = projects.stream()
                .mapToDouble(p -> p.getTravelCost() != null ? p.getTravelCost() : 0).sum();
            double totalOther = projects.stream()
                .mapToDouble(p -> p.getOtherCost() != null ? p.getOtherCost() : 0).sum();
            
            if (totalLabor > 0) dataset.setValue("Labor", totalLabor);
            if (totalMaterial > 0) dataset.setValue("Materials", totalMaterial);
            if (totalTravel > 0) dataset.setValue("Travel", totalTravel);
            if (totalOther > 0) dataset.setValue("Other", totalOther);
            
            JFreeChart chart = ChartFactory.createPieChart(
                "Cost Breakdown by Category",
                dataset,
                true,
                true,
                false
            );
            
            // Customize colors
            org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
            plot.setSectionPaint("Labor", new Color(52, 152, 219));
            plot.setSectionPaint("Materials", new Color(46, 204, 113));
            plot.setSectionPaint("Travel", new Color(241, 196, 15));
            plot.setSectionPaint("Other", new Color(155, 89, 182));
            
            return chart.createBufferedImage(400, 250);
        } catch (Exception e) {
            logger.error("Failed to create cost breakdown chart", e);
            return null;
        }
    }
    
    private BufferedImage createCostTrendBarChart(List<Project> projects) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Group projects by month
            Map<String, List<Project>> byMonth = projects.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> 
                    p.getStartDate().format(DateTimeFormatter.ofPattern("MMM yyyy"))));
            
            for (Map.Entry<String, List<Project>> entry : byMonth.entrySet()) {
                double monthCost = entry.getValue().stream()
                    .mapToDouble(p -> p.getTotalCost() != null ? p.getTotalCost() : 0)
                    .sum();
                dataset.addValue(monthCost, "Total Cost", entry.getKey());
            }
            
            JFreeChart chart = ChartFactory.createBarChart(
                "Monthly Cost Trend",
                "Month",
                "Cost ($)",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                false,
                true,
                false
            );
            
            return chart.createBufferedImage(500, 250);
        } catch (Exception e) {
            logger.error("Failed to create cost trend chart", e);
            return null;
        }
    }
    
    private BufferedImage createProfitabilityChart(List<Project> projects) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Show top 10 projects by profit margin
            projects.stream()
                .limit(10)
                .forEach(p -> {
                    dataset.addValue(p.getProfitMargin(), "Profit Margin %", p.getProjectId());
                });
            
            JFreeChart chart = ChartFactory.createBarChart(
                "Top Projects by Profit Margin",
                "Project",
                "Margin %",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                false,
                true,
                false
            );
            
            // Color bars based on margin level
            org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
            org.jfree.chart.renderer.category.BarRenderer renderer = 
                (org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(34, 139, 34));
            
            return chart.createBufferedImage(500, 250);
        } catch (Exception e) {
            logger.error("Failed to create profitability chart", e);
            return null;
        }
    }
}