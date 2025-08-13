package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class SimpleGardenLoader {
    
    public static void main(String[] args) {
        System.out.println("=== Simple Garden Data Loader ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            
            // First, check the schema
            System.out.println("Checking table schemas...\n");
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Check resources table
            ResultSet cols = metaData.getColumns(null, null, "resources", null);
            System.out.println("Resources table columns:");
            while (cols.next()) {
                System.out.println("  - " + cols.getString("COLUMN_NAME"));
            }
            cols.close();
            
            // Check projects table  
            cols = metaData.getColumns(null, null, "projects", null);
            System.out.println("\nProjects table columns:");
            while (cols.next()) {
                System.out.println("  - " + cols.getString("COLUMN_NAME"));
            }
            cols.close();
            
            // Clear existing data
            System.out.println("\nClearing existing data...");
            stmt.executeUpdate("DELETE FROM assignments");
            stmt.executeUpdate("DELETE FROM tasks");
            stmt.executeUpdate("DELETE FROM projects");
            stmt.executeUpdate("DELETE FROM resources");
            stmt.executeUpdate("DELETE FROM project_managers");
            System.out.println("  ✓ All existing data cleared");
            
            // Load garden data
            System.out.println("\nLoading garden demo data...");
            
            // Project managers
            stmt.executeUpdate("INSERT INTO project_managers (id, name, email, phone) VALUES " +
                "(1, 'Sarah Green', 'sarah.green@gardens.com', '555-0101')," +
                "(2, 'Michael Bloom', 'michael.bloom@gardens.com', '555-0102')," +
                "(3, 'Jennifer Rose', 'jennifer.rose@gardens.com', '555-0103')," +
                "(4, 'David Meadows', 'david.meadows@gardens.com', '555-0104')");
            System.out.println("  ✓ 4 Project Managers added");
            
            // Resources - minimal columns only
            stmt.executeUpdate("INSERT INTO resources (id, name, email, phone) VALUES " +
                "(1, 'Tom Hardy - Landscape Architect', 'tom.hardy@gardens.com', '555-1001')," +
                "(2, 'Emma Stone - Garden Designer', 'emma.stone@gardens.com', '555-1002')," +
                "(3, 'Chris Pine - Horticulturist', 'chris.pine@gardens.com', '555-1003')," +
                "(4, 'Maya Petal - Irrigation Specialist', 'maya.petal@gardens.com', '555-1004')," +
                "(5, 'Alex Rivers - Soil Engineer', 'alex.rivers@gardens.com', '555-1005')," +
                "(6, 'Lisa Fern - Master Gardener', 'lisa.fern@gardens.com', '555-1006')," +
                "(7, 'James Oak - Arborist', 'james.oak@gardens.com', '555-1007')," +
                "(8, 'Sophie Vine - Permaculture Expert', 'sophie.vine@gardens.com', '555-1008')," +
                "(9, 'Robert Moss - Hardscape Specialist', 'robert.moss@gardens.com', '555-1009')," +
                "(10, 'Diana Lily - Plant Pathologist', 'diana.lily@gardens.com', '555-1010')," +
                "(11, 'Sam Willow - Greenhouse Manager', 'sam.willow@gardens.com', '555-1011')," +
                "(12, 'Nina Cypress - Water Feature Expert', 'nina.cypress@gardens.com', '555-1012')," +
                "(13, 'Oliver Branch - Tree Care', 'oliver.branch@gardens.com', '555-1013')," +
                "(14, 'Zara Bloom - Floral Designer', 'zara.bloom@gardens.com', '555-1014')," +
                "(15, 'Marcus Root - Composting Expert', 'marcus.root@gardens.com', '555-1015')," +
                "(16, 'Ivy Forest - Native Plants', 'ivy.forest@gardens.com', '555-1016')," +
                "(17, 'Reed Waters - Aquaponics', 'reed.waters@gardens.com', '555-1017')," +
                "(18, 'Flora Meadow - Bee Keeping', 'flora.meadow@gardens.com', '555-1018')," +
                "(19, 'Clay Potts - Ceramics', 'clay.potts@gardens.com', '555-1019')," +
                "(20, 'Sage Herbs - Medicinal Gardens', 'sage.herbs@gardens.com', '555-1020')");
            System.out.println("  ✓ 20 Resources added");
            
            // Projects - use project_id column for name
            String[] projects = {
                "(1, 'Urban Rooftop Garden', 'Transform 5000 sq ft rooftop into productive garden', 1, '2025-01-06', '2025-04-30', 'ACTIVE')",
                "(2, 'Community Herb Spiral', 'Create educational herb garden for local school', 2, '2025-01-15', '2025-02-28', 'ACTIVE')",
                "(3, 'Japanese Zen Garden', 'Traditional Japanese garden with koi pond', 3, '2025-02-01', '2025-05-31', 'PLANNED')",
                "(4, 'Pollinator Paradise', 'Native plant garden to support local pollinators', 4, '2025-02-15', '2025-04-15', 'PLANNED')",
                "(5, 'Victorian Rose Garden', 'Restoration of historic rose garden', 1, '2025-03-01', '2025-06-30', 'PLANNED')",
                "(6, 'Edible Forest Garden', 'Food forest with fruit trees and perennials', 2, '2025-03-15', '2025-07-31', 'PLANNED')",
                "(7, 'Desert Xeriscape', 'Water-wise landscaping for arid climate', 3, '2025-04-01', '2025-06-15', 'PLANNED')",
                "(8, 'Healing Garden', 'Therapeutic garden for hospital patients', 4, '2025-04-15', '2025-07-15', 'PLANNED')",
                "(9, 'School Garden Lab', 'Outdoor classroom with raised beds', 1, '2025-05-01', '2025-06-30', 'PLANNED')",
                "(10, 'Rain Garden System', 'Bioswales and rain gardens for stormwater', 2, '2025-05-15', '2025-08-31', 'PLANNED')",
                "(11, 'Mediterranean Courtyard', 'Drought-tolerant Mediterranean landscape', 3, '2025-06-01', '2025-08-15', 'PLANNED')",
                "(12, 'Butterfly Sanctuary', 'Native habitat for monarch butterflies', 4, '2025-06-15', '2025-09-30', 'PLANNED')",
                "(13, 'Senior Center Garden', 'Accessible raised bed gardens for seniors', 1, '2025-07-01', '2025-08-31', 'PLANNED')",
                "(14, 'Greenhouse Complex', 'Year-round growing facility with automation', 2, '2025-07-15', '2025-11-30', 'PLANNED')",
                "(15, 'Wildflower Meadow', 'Convert lawn to native wildflower habitat', 3, '2025-08-01', '2025-09-30', 'PLANNED')",
                "(16, 'Sensory Garden', 'Garden designed for sensory experiences', 4, '2025-08-15', '2025-10-31', 'PLANNED')",
                "(17, 'Vertical Garden Wall', 'Living wall installation on building facade', 1, '2025-09-01', '2025-10-15', 'PLANNED')",
                "(18, 'Orchard Renovation', 'Restore and expand heritage fruit orchard', 2, '2025-09-15', '2025-12-15', 'PLANNED')",
                "(19, 'Shade Garden Oasis', 'Transform shaded area into lush garden', 3, '2025-10-01', '2025-11-30', 'PLANNED')",
                "(20, 'Aquaponics System', 'Integrated fish and vegetable production', 4, '2025-10-15', '2026-01-31', 'PLANNED')",
                "(21, 'Native Prairie', 'Restore 10 acres to native prairie', 1, '2025-11-01', '2026-03-31', 'PLANNED')",
                "(22, 'Winter Garden Design', 'Four-season interest garden', 2, '2025-11-15', '2026-01-15', 'PLANNED')",
                "(23, 'Discovery Garden', 'Interactive garden for children', 3, '2025-12-01', '2026-03-31', 'PLANNED')",
                "(24, 'Permaculture Demo', 'Showcase permaculture principles', 4, '2025-01-01', '2025-12-31', 'ACTIVE')",
                "(25, 'Memorial Garden', 'Contemplative space for reflection', 1, '2025-01-15', '2025-03-31', 'ACTIVE')"
            };
            
            for (String project : projects) {
                stmt.executeUpdate("INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status) VALUES " + project);
            }
            System.out.println("  ✓ 25 Projects added");
            
            // Assignments
            stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, notes) VALUES " +
                "(1, 1, '2025-01-06', '2025-01-31', 'Lead design for rooftop')," +
                "(2, 2, '2025-01-15', '2025-01-31', 'Herb spiral design')," +
                "(25, 3, '2025-01-15', '2025-02-15', 'Memorial garden plants')," +
                "(24, 8, '2025-01-01', '2025-12-31', 'Year-long permaculture project')," +
                "(3, 1, '2025-02-01', '2025-02-28', 'Japanese garden design')," +
                "(4, 16, '2025-02-15', '2025-03-31', 'Native plant selection')," +
                "(5, 2, '2025-03-01', '2025-04-30', 'Rose garden restoration')," +
                "(6, 8, '2025-03-15', '2025-07-31', 'Food forest design')," +
                "(7, 3, '2025-04-01', '2025-06-15', 'Desert plant selection')," +
                "(8, 10, '2025-04-15', '2025-07-15', 'Healing garden health')," +
                "(9, 6, '2025-05-01', '2025-06-30', 'School garden teaching')," +
                "(10, 5, '2025-05-15', '2025-08-31', 'Rain garden engineering')");
            System.out.println("  ✓ 12 Assignments added");
            
            conn.commit();
            
            // Verify data
            System.out.println("\nVerifying loaded data...");
            
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM project_managers");
            if (rs.next()) System.out.println("  Project Managers: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM resources");
            if (rs.next()) System.out.println("  Resources: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) System.out.println("  Projects: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
            if (rs.next()) System.out.println("  Assignments: " + rs.getInt(1));
            
            System.out.println("\n=== Garden Data Loaded Successfully! ===");
            System.out.println("Please restart the application to see the garden projects.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dataSource.close();
        }
    }
}