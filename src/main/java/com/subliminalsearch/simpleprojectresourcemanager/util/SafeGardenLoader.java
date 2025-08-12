package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;

public class SafeGardenLoader {
    
    public static void main(String[] args) {
        System.out.println("=== Safe Garden Data Loader ===\n");
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.SimpleProjectResourceManager/scheduler.db");
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            
            // Clear existing data
            System.out.println("Step 1: Clearing existing data...\n");
            stmt.executeUpdate("DELETE FROM assignments");
            stmt.executeUpdate("DELETE FROM tasks");
            stmt.executeUpdate("DELETE FROM projects");
            stmt.executeUpdate("DELETE FROM resources");
            stmt.executeUpdate("DELETE FROM project_managers");
            System.out.println("  ✓ All existing data cleared");
            
            // Load garden data
            System.out.println("\nStep 2: Loading garden demo data...\n");
            
            // Project managers
            stmt.executeUpdate("INSERT INTO project_managers (id, name, email, phone) VALUES " +
                "(1, 'Sarah Green', 'sarah.green@gardens.com', '555-0101')," +
                "(2, 'Michael Bloom', 'michael.bloom@gardens.com', '555-0102')," +
                "(3, 'Jennifer Rose', 'jennifer.rose@gardens.com', '555-0103')," +
                "(4, 'David Meadows', 'david.meadows@gardens.com', '555-0104')");
            System.out.println("  ✓ 4 Project Managers added");
            
            // Resources (without role column)
            stmt.executeUpdate("INSERT INTO resources (id, name, email, phone, hourly_rate, max_hours_per_week) VALUES " +
                "(1, 'Tom Hardy - Landscape Architect', 'tom.hardy@gardens.com', '555-1001', 125.00, 40)," +
                "(2, 'Emma Stone - Garden Designer', 'emma.stone@gardens.com', '555-1002', 95.00, 40)," +
                "(3, 'Chris Pine - Horticulturist', 'chris.pine@gardens.com', '555-1003', 85.00, 40)," +
                "(4, 'Maya Petal - Irrigation Specialist', 'maya.petal@gardens.com', '555-1004', 90.00, 40)," +
                "(5, 'Alex Rivers - Soil Engineer', 'alex.rivers@gardens.com', '555-1005', 110.00, 40)," +
                "(6, 'Lisa Fern - Master Gardener', 'lisa.fern@gardens.com', '555-1006', 75.00, 40)," +
                "(7, 'James Oak - Arborist', 'james.oak@gardens.com', '555-1007', 95.00, 40)," +
                "(8, 'Sophie Vine - Permaculture Expert', 'sophie.vine@gardens.com', '555-1008', 105.00, 40)," +
                "(9, 'Robert Moss - Hardscape Specialist', 'robert.moss@gardens.com', '555-1009', 100.00, 40)," +
                "(10, 'Diana Lily - Plant Pathologist', 'diana.lily@gardens.com', '555-1010', 115.00, 40)," +
                "(11, 'Sam Willow - Greenhouse Manager', 'sam.willow@gardens.com', '555-1011', 80.00, 40)," +
                "(12, 'Nina Cypress - Water Feature Expert', 'nina.cypress@gardens.com', '555-1012', 105.00, 40)," +
                "(13, 'Oliver Branch - Tree Care Specialist', 'oliver.branch@gardens.com', '555-1013', 90.00, 40)," +
                "(14, 'Zara Bloom - Floral Designer', 'zara.bloom@gardens.com', '555-1014', 85.00, 40)," +
                "(15, 'Marcus Root - Composting Expert', 'marcus.root@gardens.com', '555-1015', 75.00, 40)," +
                "(16, 'Ivy Forest - Native Plant Specialist', 'ivy.forest@gardens.com', '555-1016', 95.00, 40)," +
                "(17, 'Reed Waters - Aquaponics Engineer', 'reed.waters@gardens.com', '555-1017', 115.00, 40)," +
                "(18, 'Flora Meadow - Bee Keeping Expert', 'flora.meadow@gardens.com', '555-1018', 80.00, 40)," +
                "(19, 'Clay Potts - Ceramics & Planters', 'clay.potts@gardens.com', '555-1019', 70.00, 40)," +
                "(20, 'Sage Herbs - Medicinal Garden Expert', 'sage.herbs@gardens.com', '555-1020', 100.00, 40)");
            System.out.println("  ✓ 20 Resources added");
            
            // Projects (47 total)
            String[] projectData = {
                "(1, 'Urban Rooftop Garden', 'Transform 5000 sq ft rooftop into productive garden', 1, '2025-01-06', '2025-04-30', 125000, 'ACTIVE', 'HIGH', 'Downtown Office Tower', 0, 'Metro Development Corp')",
                "(2, 'Community Herb Spiral', 'Create educational herb garden for local school', 2, '2025-01-15', '2025-02-28', 35000, 'ACTIVE', 'MEDIUM', 'Riverside Elementary School', 1, 'School District 42')",
                "(3, 'Japanese Zen Garden', 'Traditional Japanese garden with koi pond', 3, '2025-02-01', '2025-05-31', 85000, 'PLANNED', 'HIGH', 'Private Estate - Hills', 2, 'The Yamamoto Family')",
                "(4, 'Pollinator Paradise', 'Native plant garden to support local pollinators', 4, '2025-02-15', '2025-04-15', 45000, 'PLANNED', 'MEDIUM', 'City Park North', 0, 'Parks Department')",
                "(5, 'Victorian Rose Garden', 'Restoration of historic rose garden', 1, '2025-03-01', '2025-06-30', 95000, 'PLANNED', 'HIGH', 'Heritage Museum', 1, 'Historical Society')",
                "(6, 'Edible Forest Garden', 'Food forest with fruit trees and perennials', 2, '2025-03-15', '2025-07-31', 78000, 'PLANNED', 'MEDIUM', 'Community Center', 0, 'Green Future Foundation')",
                "(7, 'Desert Xeriscape', 'Water-wise landscaping for arid climate', 3, '2025-04-01', '2025-06-15', 56000, 'PLANNED', 'LOW', 'Desert Ridge HOA', 3, 'Desert Ridge Association')",
                "(8, 'Healing Garden', 'Therapeutic garden for hospital patients', 4, '2025-04-15', '2025-07-15', 68000, 'PLANNED', 'HIGH', 'General Hospital', 0, 'Regional Medical Center')",
                "(9, 'School Garden Lab', 'Outdoor classroom with raised beds', 1, '2025-05-01', '2025-06-30', 42000, 'PLANNED', 'MEDIUM', 'Oakwood Middle School', 1, 'Oakwood School Board')",
                "(10, 'Rain Garden System', 'Bioswales and rain gardens for stormwater', 2, '2025-05-15', '2025-08-31', 89000, 'PLANNED', 'HIGH', 'Tech Campus', 0, 'InnovateTech Inc')",
                "(11, 'Mediterranean Courtyard', 'Drought-tolerant Mediterranean landscape', 3, '2025-06-01', '2025-08-15', 52000, 'PLANNED', 'MEDIUM', 'Villa Rosa', 2, 'The Martinez Family')",
                "(12, 'Butterfly Sanctuary', 'Native habitat for monarch butterflies', 4, '2025-06-15', '2025-09-30', 71000, 'PLANNED', 'HIGH', 'Nature Preserve', 1, 'Wildlife Conservation')",
                "(13, 'Senior Center Garden', 'Accessible raised bed gardens for seniors', 1, '2025-07-01', '2025-08-31', 38000, 'PLANNED', 'MEDIUM', 'Sunset Senior Center', 0, 'Senior Services Dept')",
                "(14, 'Greenhouse Complex', 'Year-round growing facility with automation', 2, '2025-07-15', '2025-11-30', 145000, 'PLANNED', 'HIGH', 'Agricultural Center', 1, 'County Extension Office')",
                "(15, 'Wildflower Meadow', 'Convert lawn to native wildflower habitat', 3, '2025-08-01', '2025-09-30', 32000, 'PLANNED', 'LOW', 'Corporate Campus', 0, 'TechCorp Headquarters')",
                "(16, 'Sensory Garden', 'Garden designed for sensory experiences', 4, '2025-08-15', '2025-10-31', 58000, 'PLANNED', 'HIGH', 'Childrens Hospital', 0, 'Healthcare Foundation')",
                "(17, 'Vertical Garden Wall', 'Living wall installation on building facade', 1, '2025-09-01', '2025-10-15', 67000, 'PLANNED', 'MEDIUM', 'Art Museum', 0, 'Museum Board')",
                "(18, 'Orchard Renovation', 'Restore and expand heritage fruit orchard', 2, '2025-09-15', '2025-12-15', 92000, 'PLANNED', 'HIGH', 'Historic Farm', 2, 'Preservation Society')",
                "(19, 'Shade Garden Oasis', 'Transform shaded area into lush garden', 3, '2025-10-01', '2025-11-30', 41000, 'PLANNED', 'MEDIUM', 'Private Residence', 1, 'The Johnson Estate')",
                "(20, 'Aquaponics System', 'Integrated fish and vegetable production', 4, '2025-10-15', '2026-01-31', 118000, 'PLANNED', 'HIGH', 'Education Center', 0, 'Science Academy')",
                "(21, 'Native Prairie Restoration', 'Restore 10 acres to native prairie', 1, '2025-11-01', '2026-03-31', 156000, 'PLANNED', 'HIGH', 'Conservation Area', 3, 'Nature Conservancy')",
                "(22, 'Winter Garden Design', 'Four-season interest garden with winter focus', 2, '2025-11-15', '2026-01-15', 48000, 'PLANNED', 'MEDIUM', 'Botanical Garden', 0, 'Garden Society')",
                "(23, 'Childrens Discovery Garden', 'Interactive garden for children', 3, '2025-12-01', '2026-03-31', 87000, 'PLANNED', 'HIGH', 'Childrens Museum', 0, 'Museum Foundation')",
                "(24, 'Permaculture Demonstration', 'Showcase permaculture principles', 4, '2025-01-01', '2025-12-31', 125000, 'ACTIVE', 'HIGH', 'Sustainability Center', 1, 'Eco Institute')",
                "(25, 'Memorial Garden', 'Contemplative space for reflection', 1, '2025-01-15', '2025-03-31', 62000, 'ACTIVE', 'MEDIUM', 'Veterans Park', 0, 'Veterans Association')",
                "(26, 'Tropical Greenhouse', 'Climate-controlled tropical plant collection', 2, '2025-02-01', '2025-05-31', 135000, 'PLANNED', 'HIGH', 'University Campus', 0, 'State University')",
                "(27, 'Food Security Garden', 'Community garden for food production', 3, '2025-02-15', '2025-04-30', 43000, 'PLANNED', 'HIGH', 'Urban Farm Site', 0, 'Food Bank Network')",
                "(28, 'Coastal Dune Garden', 'Salt-tolerant plants for coastal environment', 4, '2025-03-01', '2025-05-31', 76000, 'PLANNED', 'MEDIUM', 'Beach Resort', 4, 'Seaside Resort LLC')",
                "(29, 'Formal Garden Restoration', 'Restore 1920s formal garden design', 1, '2025-03-15', '2025-07-31', 198000, 'PLANNED', 'HIGH', 'Historic Mansion', 2, 'Heritage Trust')",
                "(30, 'Bee Hotel Installation', 'Native bee habitat structures', 2, '2025-04-01', '2025-05-15', 28000, 'PLANNED', 'LOW', 'Community Gardens', 0, 'Pollinator Partnership')",
                "(31, 'Sculpture Garden', 'Landscape design for outdoor art display', 3, '2025-04-15', '2025-07-31', 112000, 'PLANNED', 'HIGH', 'Art Park', 1, 'Arts Council')",
                "(32, 'Bog Garden Creation', 'Wetland garden with native bog plants', 4, '2025-05-01', '2025-07-31', 54000, 'PLANNED', 'MEDIUM', 'Nature Center', 0, 'Environmental Ed Fund')",
                "(33, 'Vineyard Installation', 'Small-scale vineyard with tasting garden', 1, '2025-05-15', '2025-09-30', 167000, 'PLANNED', 'HIGH', 'Winery Estate', 3, 'Valley Vineyards')",
                "(34, 'Moonlight Garden', 'White flowers and night-blooming plants', 2, '2025-06-01', '2025-07-31', 39000, 'PLANNED', 'LOW', 'Hotel Gardens', 1, 'Luxury Hotel Group')",
                "(35, 'Alpine Rock Garden', 'Mountain plant collection with rockwork', 3, '2025-06-15', '2025-09-15', 83000, 'PLANNED', 'MEDIUM', 'Mountain Lodge', 4, 'Alpine Resorts Inc')",
                "(36, 'Ethnobotanical Garden', 'Plants with cultural and medicinal uses', 4, '2025-07-01', '2025-10-31', 97000, 'PLANNED', 'HIGH', 'Cultural Center', 0, 'Indigenous Heritage')",
                "(37, 'Fragrance Garden', 'Garden designed for scent experiences', 1, '2025-07-15', '2025-09-15', 44000, 'PLANNED', 'MEDIUM', 'Spa Resort', 2, 'Wellness Retreats')",
                "(38, 'Bird Sanctuary Garden', 'Native plants to attract and support birds', 2, '2025-08-01', '2025-10-31', 61000, 'PLANNED', 'HIGH', 'Audubon Center', 0, 'Audubon Society')",
                "(39, 'Maze Garden Design', 'Hedge maze with central garden feature', 3, '2025-08-15', '2025-11-30', 129000, 'PLANNED', 'MEDIUM', 'Tourist Attraction', 1, 'Tourism Board')",
                "(40, 'Cactus Garden', 'Desert plant collection and display', 4, '2025-09-01', '2025-11-15', 73000, 'PLANNED', 'MEDIUM', 'Desert Museum', 2, 'Natural History Museum')",
                "(41, 'Woodland Garden Path', 'Shaded trail with native woodland plants', 1, '2025-09-15', '2025-11-30', 56000, 'PLANNED', 'LOW', 'State Park', 3, 'Parks Service')",
                "(42, 'Cut Flower Production', 'Commercial cut flower growing beds', 2, '2025-10-01', '2025-12-31', 88000, 'PLANNED', 'HIGH', 'Flower Farm', 1, 'Blooms Wholesale')",
                "(43, 'Meditation Garden', 'Quiet space for contemplation', 3, '2025-10-15', '2025-12-15', 47000, 'PLANNED', 'MEDIUM', 'Retreat Center', 2, 'Mindfulness Institute')",
                "(44, 'Victory Garden Revival', 'Historical vegetable garden recreation', 4, '2025-11-01', '2026-01-31', 52000, 'PLANNED', 'MEDIUM', 'History Museum', 0, 'Historical Society')",
                "(45, 'Green Roof System', 'Extensive green roof installation', 1, '2025-11-15', '2026-02-28', 142000, 'PLANNED', 'HIGH', 'Office Building', 0, 'Property Management')",
                "(46, 'Biophilic Office Design', 'Indoor/outdoor garden integration', 2, '2025-12-01', '2026-03-31', 178000, 'PLANNED', 'HIGH', 'Tech Headquarters', 0, 'Innovation Labs')",
                "(47, 'Climate Garden', 'Demonstrate climate change adaptations', 3, '2025-12-15', '2026-04-30', 95000, 'PLANNED', 'HIGH', 'Science Center', 0, 'Climate Alliance')"
            };
            
            for (String project : projectData) {
                stmt.executeUpdate("INSERT INTO projects (id, title, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES " + project);
            }
            System.out.println("  ✓ 47 Projects added");
            
            // Assignments with conflicts
            stmt.executeUpdate("INSERT INTO assignments (project_id, resource_id, start_date, end_date, notes) VALUES " +
                // January assignments
                "(1, 1, '2025-01-06', '2025-01-31', 'Lead design for rooftop')," +
                "(2, 2, '2025-01-15', '2025-01-31', 'Herb spiral design')," +
                "(25, 3, '2025-01-15', '2025-02-15', 'Memorial garden plants')," +
                "(24, 8, '2025-01-01', '2025-12-31', 'Year-long permaculture project')," +
                // February conflicts
                "(3, 1, '2025-02-01', '2025-02-28', 'Japanese garden design')," +  // Conflict: Tom on 2 projects
                "(26, 1, '2025-02-15', '2025-03-15', 'Tropical greenhouse')," +    // Overlap with above
                "(4, 16, '2025-02-15', '2025-03-31', 'Native plant selection')," +
                "(27, 6, '2025-02-15', '2025-04-30', 'Food garden setup')," +
                // March busy period
                "(5, 2, '2025-03-01', '2025-04-30', 'Rose garden restoration')," +
                "(28, 9, '2025-03-01', '2025-05-31', 'Coastal hardscaping')," +
                "(29, 1, '2025-03-15', '2025-07-31', 'Formal garden lead')," +     // Tom triple-booked
                "(6, 8, '2025-03-15', '2025-07-31', 'Food forest design')," +      // Sophie double-booked
                // April conflicts
                "(7, 3, '2025-04-01', '2025-06-15', 'Desert plant selection')," +
                "(30, 18, '2025-04-01', '2025-05-15', 'Bee hotel setup')," +
                "(31, 9, '2025-04-15', '2025-07-31', 'Sculpture garden paths')," + // Robert double-booked
                "(8, 10, '2025-04-15', '2025-07-15', 'Healing garden health')," +
                // May crunch
                "(9, 6, '2025-05-01', '2025-06-30', 'School garden teaching')," +  // Lisa triple-booked
                "(32, 12, '2025-05-01', '2025-07-31', 'Bog water features')," +
                "(33, 7, '2025-05-15', '2025-09-30', 'Vineyard trees')," +
                "(10, 5, '2025-05-15', '2025-08-31', 'Rain garden engineering')," +
                // Resource 1 (Tom) severe overallocation
                "(34, 1, '2025-06-01', '2025-07-31', 'Moonlight garden')," +       // Tom 4x booked
                "(37, 1, '2025-07-15', '2025-09-15', 'Fragrance garden')," +       // Tom 5x booked
                "(41, 1, '2025-09-15', '2025-11-30', 'Woodland paths')," +
                "(45, 1, '2025-11-15', '2026-02-28', 'Green roof project')");
            System.out.println("  ✓ 24 Assignments added (with conflicts)");
            
            conn.commit();
            
            // Verify data
            System.out.println("\nStep 3: Verifying loaded data...\n");
            
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM project_managers");
            if (rs.next()) System.out.println("  Project Managers: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM resources");
            if (rs.next()) System.out.println("  Resources: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM projects");
            if (rs.next()) System.out.println("  Projects: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM assignments");
            if (rs.next()) System.out.println("  Assignments: " + rs.getInt(1));
            
            // Show some sample projects
            System.out.println("\nSample Projects:");
            rs = stmt.executeQuery("SELECT title, start_date, status FROM projects ORDER BY start_date LIMIT 5");
            while (rs.next()) {
                System.out.println("  • " + rs.getString("title") + " (" + rs.getString("start_date") + ") - " + rs.getString("status"));
            }
            
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