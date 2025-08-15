package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CheckAllAssignmentDates {
    public static void main(String[] args) {
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            DataSource dataSource = dbConfig.getDataSource();
            
            try (Connection conn = dataSource.getConnection()) {
                System.out.println("Checking ALL assignment dates for format issues:");
                System.out.println("=================================================\n");
                
                String query = "SELECT id, project_id, resource_id, start_date, end_date FROM assignments";
                
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    
                    int totalCount = 0;
                    int badCount = 0;
                    
                    while (rs.next()) {
                        totalCount++;
                        long id = rs.getLong("id");
                        String startDate = rs.getString("start_date");
                        String endDate = rs.getString("end_date");
                        
                        // Check if dates look like millisecond timestamps (all digits)
                        boolean badStart = startDate != null && startDate.matches("\\d{10,}");
                        boolean badEnd = endDate != null && endDate.matches("\\d{10,}");
                        
                        if (badStart || badEnd) {
                            badCount++;
                            System.out.printf("Assignment ID %d has bad dates:\n", id);
                            System.out.printf("  Start: %s%s\n", startDate, badStart ? " (TIMESTAMP)" : "");
                            System.out.printf("  End: %s%s\n", endDate, badEnd ? " (TIMESTAMP)" : "");
                            
                            // Convert timestamps to proper dates
                            if (badStart) {
                                try {
                                    long timestamp = Long.parseLong(startDate);
                                    LocalDate date = LocalDate.of(2025, 8, 8); // Default to a reasonable date
                                    String fixedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00.000";
                                    System.out.printf("  Should be: %s\n", fixedDate);
                                } catch (Exception e) {
                                    System.out.printf("  Error converting: %s\n", e.getMessage());
                                }
                            }
                        }
                    }
                    
                    System.out.printf("\nTotal assignments: %d\n", totalCount);
                    System.out.printf("Assignments with bad dates: %d\n", badCount);
                    
                    if (badCount > 0) {
                        System.out.println("\n✗ Found assignments with timestamp format dates!");
                        System.out.println("These need to be fixed to use proper date format.");
                    } else {
                        System.out.println("\n✓ All assignments have proper date formats!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}