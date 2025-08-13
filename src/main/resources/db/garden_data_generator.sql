-- Garden Project Resource Manager - 12 Month Data Generation
-- Theme: Growing Gardens - From Planning to Harvest
-- Creates realistic project data with tasks, resources, assignments, and conflicts

-- Clear existing data
DELETE FROM assignments;
DELETE FROM tasks;
DELETE FROM projects;
DELETE FROM resources;
DELETE FROM project_managers;

-- Insert Project Managers
INSERT INTO project_managers (id, name, email, phone, department) VALUES
(1, 'Sarah Greenthumb', 'sarah.greenthumb@gardens.com', '555-0101', 'Urban Gardens Division'),
(2, 'Marcus Bloom', 'marcus.bloom@gardens.com', '555-0102', 'Commercial Landscapes'),
(3, 'Linda Harvest', 'linda.harvest@gardens.com', '555-0103', 'Community Gardens'),
(4, 'James Thorne', 'james.thorne@gardens.com', '555-0104', 'Botanical Projects');

-- Insert Garden Technicians/Resources
INSERT INTO resources (id, name, email, role, skills, hourly_rate, availability_status) VALUES
-- Senior Garden Designers
(1, 'Emma Landscape', 'emma.landscape@gardens.com', 'Senior Garden Designer', 'Landscape Design, CAD, Permaculture', 95.00, 'AVAILABLE'),
(2, 'Oliver Roots', 'oliver.roots@gardens.com', 'Senior Garden Designer', 'Native Plants, Water Features, Hardscaping', 95.00, 'AVAILABLE'),

-- Horticulturists
(3, 'Sophia Flora', 'sophia.flora@gardens.com', 'Lead Horticulturist', 'Plant Pathology, Soil Science, Organic Methods', 85.00, 'AVAILABLE'),
(4, 'Noah Seedling', 'noah.seedling@gardens.com', 'Horticulturist', 'Propagation, Greenhouse Management, Hydroponics', 75.00, 'AVAILABLE'),
(5, 'Ava Petal', 'ava.petal@gardens.com', 'Horticulturist', 'Flower Cultivation, Color Design, Seasonal Planning', 75.00, 'AVAILABLE'),

-- Landscapers/Installers
(6, 'Liam Stone', 'liam.stone@gardens.com', 'Lead Landscaper', 'Hardscaping, Irrigation, Heavy Equipment', 70.00, 'AVAILABLE'),
(7, 'Isabella Terra', 'isabella.terra@gardens.com', 'Landscaper', 'Planting, Mulching, Bed Preparation', 60.00, 'AVAILABLE'),
(8, 'Mason Grove', 'mason.grove@gardens.com', 'Landscaper', 'Tree Care, Pruning, Equipment Operation', 60.00, 'AVAILABLE'),
(9, 'Mia Meadow', 'mia.meadow@gardens.com', 'Landscaper', 'Turf Management, Edging, Maintenance', 60.00, 'AVAILABLE'),

-- Irrigation Specialists
(10, 'Ethan Waters', 'ethan.waters@gardens.com', 'Irrigation Specialist', 'Drip Systems, Smart Controllers, Water Conservation', 80.00, 'AVAILABLE'),
(11, 'Charlotte Rain', 'charlotte.rain@gardens.com', 'Irrigation Technician', 'Sprinkler Systems, Repairs, Winterization', 65.00, 'AVAILABLE'),

-- Arborists
(12, 'William Oak', 'william.oak@gardens.com', 'Certified Arborist', 'Tree Health, Pruning, Disease Treatment', 90.00, 'AVAILABLE'),
(13, 'Amelia Birch', 'amelia.birch@gardens.com', 'Arborist Assistant', 'Tree Climbing, Safety, Cabling', 70.00, 'AVAILABLE'),

-- Garden Maintenance
(14, 'Benjamin Blade', 'benjamin.blade@gardens.com', 'Maintenance Lead', 'Weekly Care, Equipment, Scheduling', 65.00, 'AVAILABLE'),
(15, 'Harper Hedge', 'harper.hedge@gardens.com', 'Maintenance Tech', 'Pruning, Weeding, Fertilization', 55.00, 'AVAILABLE'),

-- Specialty Experts
(16, 'Lucas Vine', 'lucas.vine@gardens.com', 'Vertical Garden Specialist', 'Green Walls, Trellises, Climbing Plants', 85.00, 'AVAILABLE'),
(17, 'Evelyn Shade', 'evelyn.shade@gardens.com', 'Shade Garden Expert', 'Understory Plants, Ferns, Moss Gardens', 80.00, 'AVAILABLE'),
(18, 'Jackson Harvest', 'jackson.harvest@gardens.com', 'Edible Garden Specialist', 'Vegetables, Herbs, Fruit Trees', 80.00, 'AVAILABLE'),
(19, 'Scarlett Rose', 'scarlett.rose@gardens.com', 'Rose Garden Specialist', 'Rose Cultivation, Disease Management, Pruning', 85.00, 'AVAILABLE'),
(20, 'Henry Pond', 'henry.pond@gardens.com', 'Water Feature Specialist', 'Ponds, Fountains, Aquatic Plants', 90.00, 'AVAILABLE');

-- PROJECTS BY MONTH
-- January 2025 - Planning & Design Phase
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(1, 'Urban Rooftop Garden', 'Transform 5000 sq ft rooftop into productive garden', 1, '2025-01-06', '2025-04-30', 125000, 'ACTIVE', 'HIGH', 'Downtown Office Tower', 0, 'Metro Development Corp'),
(2, 'Community Seed Library', 'Establish seed saving and sharing program', 3, '2025-01-13', '2025-03-15', 35000, 'ACTIVE', 'MEDIUM', 'Public Library', 0, 'City Library System'),
(3, 'Winter Garden Prep', 'Prepare greenhouse for spring propagation', 2, '2025-01-20', '2025-02-10', 18000, 'ACTIVE', 'HIGH', 'Main Greenhouse', 0, 'Internal Project');

-- February 2025 - Early Spring Prep
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(4, 'School Butterfly Garden', 'Native plant garden to attract butterflies', 3, '2025-02-03', '2025-05-30', 45000, 'PLANNED', 'MEDIUM', 'Elementary School', 1, 'Sunshine Elementary'),
(5, 'Corporate Zen Garden', 'Japanese-inspired meditation garden', 1, '2025-02-10', '2025-06-15', 95000, 'PLANNED', 'HIGH', 'Tech Campus', 2, 'TechCorp Industries'),
(6, 'Spring Soil Amendment', 'Test and amend soil in all garden beds', 4, '2025-02-17', '2025-03-10', 22000, 'PLANNED', 'HIGH', 'Multiple Sites', 1, 'Garden Maintenance Contract'),
(7, 'Greenhouse Expansion', 'Add 2000 sq ft to propagation facility', 2, '2025-02-24', '2025-04-20', 85000, 'PLANNED', 'HIGH', 'Main Facility', 0, 'Internal Expansion');

-- March 2025 - Spring Planting Season
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(8, 'Heritage Rose Garden', 'Plant 50 varieties of heritage roses', 4, '2025-03-03', '2025-05-15', 65000, 'PLANNED', 'MEDIUM', 'Historic Estate', 3, 'Heritage Foundation'),
(9, 'Edible Forest Garden', 'Permaculture food forest installation', 1, '2025-03-10', '2025-07-31', 110000, 'PLANNED', 'HIGH', 'Community Park', 1, 'Parks Department'),
(10, 'Spring Bulb Festival', 'Plant 10,000 bulbs for fall display', 3, '2025-03-17', '2025-04-10', 28000, 'PLANNED', 'MEDIUM', 'City Center', 0, 'Downtown Association'),
(11, 'Pollinator Highway', 'Create pollinator corridor along highway', 2, '2025-03-24', '2025-06-30', 155000, 'PLANNED', 'HIGH', 'Highway Median', 2, 'State DOT');

-- April 2025 - Peak Spring Activity
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(12, 'Healing Garden', 'Therapeutic garden for hospital patients', 3, '2025-04-01', '2025-07-15', 88000, 'PLANNED', 'HIGH', 'Medical Center', 1, 'Regional Hospital'),
(13, 'Vineyard Installation', 'Plant 5-acre organic vineyard', 4, '2025-04-07', '2025-06-20', 125000, 'PLANNED', 'HIGH', 'Rural Property', 4, 'Sunset Winery'),
(14, 'Rain Garden Network', 'Install bioswales and rain gardens', 1, '2025-04-14', '2025-06-10', 72000, 'PLANNED', 'MEDIUM', 'Subdivision', 2, 'Green Homes Development'),
(15, 'Native Prairie Restoration', 'Convert lawn to native prairie', 2, '2025-04-21', '2025-08-30', 95000, 'PLANNED', 'MEDIUM', 'Corporate Campus', 3, 'EcoTech Solutions'),
(16, 'Children''s Garden', 'Interactive educational garden', 3, '2025-04-28', '2025-07-10', 58000, 'PLANNED', 'MEDIUM', 'Museum Grounds', 1, 'Children''s Museum');

-- May 2025 - Late Spring
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(17, 'Memorial Garden', 'Contemplative garden with water features', 4, '2025-05-05', '2025-08-15', 145000, 'PLANNED', 'HIGH', 'Cemetery', 2, 'Memorial Park'),
(18, 'Urban Farm Setup', 'Commercial urban farming operation', 1, '2025-05-12', '2025-09-30', 185000, 'PLANNED', 'HIGH', 'Warehouse District', 1, 'Fresh Greens Co'),
(19, 'Shade Garden Paradise', 'Transform shaded areas into lush gardens', 2, '2025-05-19', '2025-07-31', 52000, 'PLANNED', 'MEDIUM', 'Wooded Property', 3, 'Private Estate'),
(20, 'Herb Spiral Collection', 'Build 10 herb spirals for community', 3, '2025-05-26', '2025-06-30', 32000, 'PLANNED', 'LOW', 'Community Gardens', 1, 'Neighborhood Association');

-- June 2025 - Early Summer
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(21, 'Drought-Tolerant Landscape', 'Xeriscape conversion project', 1, '2025-06-02', '2025-08-20', 78000, 'PLANNED', 'HIGH', 'Office Park', 2, 'Desert Properties LLC'),
(22, 'Tropical Garden Oasis', 'Indoor tropical garden installation', 4, '2025-06-09', '2025-08-10', 92000, 'PLANNED', 'MEDIUM', 'Hotel Atrium', 1, 'Grand Hotel'),
(23, 'Vegetable Production Beds', 'Install 50 raised beds for vegetables', 2, '2025-06-16', '2025-07-20', 45000, 'PLANNED', 'MEDIUM', 'Farm Site', 4, 'Local Food Coop'),
(24, 'Sensory Garden', 'Garden engaging all five senses', 3, '2025-06-23', '2025-09-15', 68000, 'PLANNED', 'MEDIUM', 'Senior Center', 1, 'Sunset Senior Living');

-- July 2025 - Mid Summer
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(25, 'Irrigation Modernization', 'Smart irrigation system upgrade', 2, '2025-07-07', '2025-08-30', 115000, 'PLANNED', 'HIGH', 'Golf Course', 3, 'Country Club'),
(26, 'Moonlight Garden', 'White flowers and night-fragrant plants', 4, '2025-07-14', '2025-09-10', 42000, 'PLANNED', 'LOW', 'Resort', 5, 'Mountain Resort'),
(27, 'Orchard Establishment', 'Plant 100 fruit trees', 1, '2025-07-21', '2025-10-15', 88000, 'PLANNED', 'MEDIUM', 'Farm', 4, 'Heritage Orchard'),
(28, 'Green Wall Installation', 'Living walls for parking garage', 3, '2025-07-28', '2025-09-20', 125000, 'PLANNED', 'HIGH', 'Parking Structure', 1, 'City Parking Authority');

-- August 2025 - Late Summer
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(29, 'Fall Garden Preparation', 'Prepare beds for autumn planting', 2, '2025-08-04', '2025-09-05', 28000, 'PLANNED', 'HIGH', 'Multiple Sites', 2, 'Maintenance Contracts'),
(30, 'Harvest Festival Setup', 'Create harvest-themed displays', 3, '2025-08-11', '2025-09-25', 35000, 'PLANNED', 'MEDIUM', 'Fairgrounds', 3, 'County Fair'),
(31, 'Wildflower Meadow', 'Convert field to wildflower meadow', 4, '2025-08-18', '2025-10-30', 62000, 'PLANNED', 'MEDIUM', 'Nature Preserve', 4, 'Conservation Trust'),
(32, 'Water Garden Complex', 'Series of connected water gardens', 1, '2025-08-25', '2025-11-15', 155000, 'PLANNED', 'HIGH', 'Resort Property', 5, 'Lakeside Resort');

-- September 2025 - Early Fall
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(33, 'Fall Bulb Installation', 'Plant 15,000 spring bulbs', 3, '2025-09-01', '2025-10-15', 48000, 'PLANNED', 'HIGH', 'Public Gardens', 1, 'Botanical Society'),
(34, 'Pumpkin Patch Creation', 'Educational pumpkin growing area', 2, '2025-09-08', '2025-10-31', 32000, 'PLANNED', 'MEDIUM', 'School Farm', 2, 'Agricultural School'),
(35, 'Tree Planting Campaign', 'Plant 500 trees citywide', 4, '2025-09-15', '2025-11-30', 185000, 'PLANNED', 'HIGH', 'Citywide', 1, 'Urban Forest Initiative'),
(36, 'Japanese Maple Collection', 'Specialty maple garden', 1, '2025-09-22', '2025-11-10', 78000, 'PLANNED', 'MEDIUM', 'Arboretum', 3, 'Tree Foundation');

-- October 2025 - Mid Fall
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(37, 'Winter Garden Design', 'Gardens with winter interest', 4, '2025-10-06', '2025-12-15', 68000, 'PLANNED', 'MEDIUM', 'Estate Gardens', 4, 'Private Client'),
(38, 'Greenhouse Winterization', 'Prepare greenhouses for winter', 2, '2025-10-13', '2025-11-10', 25000, 'PLANNED', 'HIGH', 'All Facilities', 2, 'Internal Maintenance'),
(39, 'Holiday Light Garden', 'Design garden for holiday lights', 3, '2025-10-20', '2025-11-25', 42000, 'PLANNED', 'MEDIUM', 'Town Square', 0, 'Chamber of Commerce'),
(40, 'Compost System Setup', 'Industrial composting operation', 1, '2025-10-27', '2025-12-20', 95000, 'PLANNED', 'MEDIUM', 'Waste Facility', 3, 'Waste Management');

-- November 2025 - Late Fall
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(41, 'Indoor Garden Rooms', 'Convert spaces to garden rooms', 1, '2025-11-03', '2026-01-15', 125000, 'PLANNED', 'HIGH', 'Office Building', 1, 'Corporate Headquarters'),
(42, 'Winter Vegetable Beds', 'Cold frames and winter crops', 3, '2025-11-10', '2025-12-20', 38000, 'PLANNED', 'MEDIUM', 'Community Garden', 1, 'Food Bank'),
(43, 'Tool Maintenance Program', 'Service all garden equipment', 2, '2025-11-17', '2025-12-10', 18000, 'PLANNED', 'HIGH', 'Maintenance Shop', 0, 'Internal Service'),
(44, 'Seed Inventory & Order', 'Catalog and order for next year', 4, '2025-11-24', '2025-12-31', 22000, 'PLANNED', 'MEDIUM', 'Seed Storage', 0, 'Supply Planning');

-- December 2025 - Winter
INSERT INTO projects (id, name, description, project_manager_id, start_date, end_date, budget, status, priority, location, travel_days, client_name) VALUES
(45, 'Holiday Wreath Workshop', 'Community wreath-making program', 3, '2025-12-01', '2025-12-20', 15000, 'PLANNED', 'LOW', 'Community Center', 0, 'Parks & Rec'),
(46, 'Year-End Garden Review', 'Document and evaluate all gardens', 4, '2025-12-08', '2025-12-31', 25000, 'PLANNED', 'MEDIUM', 'All Sites', 3, 'Annual Review'),
(47, '2026 Garden Planning', 'Design next year''s garden projects', 1, '2025-12-15', '2026-01-31', 45000, 'PLANNED', 'HIGH', 'Office', 0, 'Strategic Planning');

-- TASKS for select projects (showing task hierarchy)
-- Urban Rooftop Garden (Project 1)
INSERT INTO tasks (id, project_id, name, description, estimated_hours, actual_hours, status, assigned_to, start_date, due_date, priority, completion_percentage, parent_task_id) VALUES
(1, 1, 'Site Analysis', 'Complete rooftop assessment', 40, 0, 'PENDING', 1, '2025-01-06', '2025-01-17', 'HIGH', 0, NULL),
(2, 1, 'Structural Evaluation', 'Load bearing capacity study', 24, 0, 'PENDING', 6, '2025-01-06', '2025-01-10', 'HIGH', 0, 1),
(3, 1, 'Wind Study', 'Analyze wind patterns', 16, 0, 'PENDING', 2, '2025-01-08', '2025-01-12', 'HIGH', 0, 1),
(4, 1, 'Design Phase', 'Create garden design', 80, 0, 'PENDING', 1, '2025-01-20', '2025-02-14', 'HIGH', 0, NULL),
(5, 1, 'Layout Planning', 'Design bed layouts', 32, 0, 'PENDING', 1, '2025-01-20', '2025-01-31', 'HIGH', 0, 4),
(6, 1, 'Plant Selection', 'Choose appropriate plants', 24, 0, 'PENDING', 3, '2025-01-25', '2025-02-05', 'MEDIUM', 0, 4),
(7, 1, 'Irrigation Design', 'Design watering system', 24, 0, 'PENDING', 10, '2025-02-01', '2025-02-10', 'HIGH', 0, 4),
(8, 1, 'Installation', 'Build rooftop garden', 240, 0, 'PENDING', 6, '2025-02-17', '2025-04-15', 'HIGH', 0, NULL),
(9, 1, 'Container Setup', 'Install planters and beds', 80, 0, 'PENDING', 6, '2025-02-17', '2025-03-07', 'HIGH', 0, 8),
(10, 1, 'Soil Preparation', 'Mix and place growing media', 40, 0, 'PENDING', 7, '2025-03-03', '2025-03-14', 'HIGH', 0, 8),
(11, 1, 'Planting', 'Plant all vegetation', 80, 0, 'PENDING', 3, '2025-03-17', '2025-04-10', 'HIGH', 0, 8),
(12, 1, 'Support Structures', 'Install trellises and supports', 40, 0, 'PENDING', 8, '2025-03-24', '2025-04-05', 'MEDIUM', 0, 8);

-- Edible Forest Garden (Project 9)
INSERT INTO tasks (id, project_id, name, description, estimated_hours, actual_hours, status, assigned_to, start_date, due_date, priority, completion_percentage, parent_task_id) VALUES
(13, 9, 'Permaculture Design', 'Create food forest layout', 60, 0, 'PENDING', 18, '2025-03-10', '2025-03-28', 'HIGH', 0, NULL),
(14, 9, 'Canopy Layer Planning', 'Select and place tall trees', 20, 0, 'PENDING', 18, '2025-03-10', '2025-03-17', 'HIGH', 0, 13),
(15, 9, 'Understory Design', 'Plan smaller trees and shrubs', 20, 0, 'PENDING', 18, '2025-03-14', '2025-03-21', 'HIGH', 0, 13),
(16, 9, 'Ground Cover Selection', 'Choose herbs and groundcovers', 20, 0, 'PENDING', 5, '2025-03-18', '2025-03-25', 'MEDIUM', 0, 13),
(17, 9, 'Guild Creation', 'Design plant guilds', 40, 0, 'PENDING', 3, '2025-03-31', '2025-04-18', 'HIGH', 0, NULL),
(18, 9, 'Nitrogen Fixers', 'Place nitrogen-fixing plants', 16, 0, 'PENDING', 3, '2025-03-31', '2025-04-07', 'HIGH', 0, 17),
(19, 9, 'Pest Deterrents', 'Add pest-deterring plants', 12, 0, 'PENDING', 5, '2025-04-07', '2025-04-14', 'MEDIUM', 0, 17),
(20, 9, 'Beneficial Attractors', 'Plants for beneficial insects', 12, 0, 'PENDING', 5, '2025-04-10', '2025-04-18', 'MEDIUM', 0, 17);

-- ASSIGNMENTS (Creating conflicts and resource allocation)
-- January Assignments
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(1, 1, 1, '2025-01-06', '2025-02-14', 75, 'Lead Designer', 'Urban rooftop design lead'),
(2, 1, 2, '2025-01-08', '2025-01-17', 50, 'Design Consultant', 'Wind and structure consultation'),
(3, 1, 6, '2025-01-06', '2025-01-17', 100, 'Structural Specialist', 'Load analysis'),
(4, 2, 3, '2025-01-13', '2025-03-15', 50, 'Seed Expert', 'Seed library setup'),
(5, 2, 4, '2025-01-13', '2025-02-28', 75, 'Propagation Lead', 'Seed starting systems'),
(6, 3, 7, '2025-01-20', '2025-02-10', 100, 'Greenhouse Prep', 'Winter preparation work'),
(7, 3, 14, '2025-01-20', '2025-02-10', 100, 'Maintenance Lead', 'Equipment winterization');

-- February Assignments (Starting conflicts)
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(8, 4, 5, '2025-02-03', '2025-05-30', 75, 'Butterfly Expert', 'Native plant specialist'),
(9, 4, 17, '2025-02-10', '2025-04-30', 50, 'Shade Plant Expert', 'Understory planning'),
(10, 5, 1, '2025-02-10', '2025-03-31', 100, 'Zen Garden Designer', 'Japanese garden design'),
-- CONFLICT: Emma (1) is at 75% on project 1 until Feb 14, now 100% on project 5 starting Feb 10
(11, 5, 20, '2025-02-15', '2025-05-15', 75, 'Water Feature Lead', 'Koi pond and streams'),
(12, 6, 3, '2025-02-17', '2025-03-10', 100, 'Soil Scientist', 'Soil testing and amendment'),
-- CONFLICT: Sophia (3) is at 50% on project 2 until Mar 15, now 100% on project 6
(13, 7, 6, '2025-02-24', '2025-04-20', 100, 'Construction Lead', 'Greenhouse expansion');

-- March Assignments (Peak conflict period)
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(14, 8, 19, '2025-03-03', '2025-05-15', 100, 'Rose Specialist', 'Heritage rose expert'),
(15, 9, 18, '2025-03-10', '2025-07-31', 100, 'Permaculture Lead', 'Food forest design'),
(16, 9, 3, '2025-03-15', '2025-06-30', 75, 'Plant Health', 'Soil and plant health'),
-- CONFLICT: Sophia (3) was on project 6 until Mar 10, immediately starts project 9
(17, 10, 7, '2025-03-17', '2025-04-10', 100, 'Bulb Planting Lead', 'Mass bulb installation'),
(18, 10, 8, '2025-03-17', '2025-04-10', 100, 'Planting Assistant', 'Bulb planting team'),
(19, 10, 9, '2025-03-17', '2025-04-10', 100, 'Planting Assistant', 'Bulb planting team'),
(20, 11, 5, '2025-03-24', '2025-06-30', 100, 'Wildflower Expert', 'Pollinator plants'),
-- CONFLICT: Ava (5) is at 75% on project 4, now needs 100% on project 11

-- April Assignments (Multiple conflicts)
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(21, 12, 3, '2025-04-01', '2025-07-15', 50, 'Therapeutic Plants', 'Healing garden plants'),
-- CONFLICT: Sophia (3) is already at 75% on project 9
(22, 12, 17, '2025-04-01', '2025-06-30', 75, 'Sensory Plants', 'Shade and texture plants'),
(23, 13, 12, '2025-04-07', '2025-06-20', 100, 'Vineyard Consultant', 'Vine selection and training'),
(24, 13, 6, '2025-04-21', '2025-06-20', 100, 'Trellis Installation', 'Support structures'),
-- CONFLICT: Liam (6) is on project 7 until Apr 20, starts project 13 on Apr 21 (1 day gap)
(25, 14, 10, '2025-04-14', '2025-06-10', 100, 'Drainage Expert', 'Rain garden hydrology'),
(26, 14, 11, '2025-04-14', '2025-06-10', 100, 'Irrigation Support', 'Bioswale plumbing'),
(27, 15, 4, '2025-04-21', '2025-08-30', 75, 'Native Plant Propagation', 'Prairie plant growing'),
(28, 16, 7, '2025-04-28', '2025-07-10', 100, 'Interactive Features', 'Children''s garden elements'),
-- CONFLICT: Isabella (7) just finished project 10 on Apr 10, but assigned to project 16 on Apr 28

-- May-June Assignments (Summer rush)
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(29, 17, 20, '2025-05-05', '2025-08-15', 100, 'Memorial Water Features', 'Fountains and reflection pools'),
(30, 18, 18, '2025-05-12', '2025-09-30', 100, 'Urban Farm Lead', 'Vertical farming systems'),
(31, 18, 4, '2025-05-12', '2025-08-31', 50, 'Hydroponic Systems', 'Indoor growing setup'),
(32, 19, 17, '2025-05-19', '2025-07-31', 100, 'Shade Garden Design', 'Full shade solutions'),
-- CONFLICT: Evelyn (17) is on project 12 until Jun 30, overlaps with project 19
(33, 20, 5, '2025-05-26', '2025-06-30', 50, 'Herb Selection', 'Culinary and medicinal herbs'),
(34, 21, 10, '2025-06-02', '2025-08-20', 100, 'Xeriscaping', 'Drought-tolerant irrigation'),
-- CONFLICT: Ethan (10) is on project 14 until Jun 10, overlaps with project 21
(35, 22, 16, '2025-06-09', '2025-08-10', 100, 'Tropical Installation', 'Vertical tropical garden'),
(36, 23, 8, '2025-06-16', '2025-07-20', 100, 'Bed Construction', 'Raised bed building'),
(37, 24, 3, '2025-06-23', '2025-09-15', 75, 'Sensory Plant Expert', 'Touch and smell plants');
-- CONFLICT: Sophia (3) is on multiple projects simultaneously

-- July-August Assignments (Mid-summer conflicts)
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(38, 25, 10, '2025-07-07', '2025-08-30', 100, 'Smart Irrigation Lead', 'Modernization project'),
-- CONFLICT: Ethan (10) is still on project 21 until Aug 20
(39, 26, 5, '2025-07-14', '2025-09-10', 75, 'Night Garden Plants', 'Fragrant evening plants'),
(40, 27, 12, '2025-07-21', '2025-10-15', 100, 'Orchard Establishment', 'Fruit tree expert'),
(41, 27, 13, '2025-07-21', '2025-10-15', 100, 'Tree Planting', 'Orchard assistant'),
(42, 28, 16, '2025-07-28', '2025-09-20', 100, 'Green Wall Expert', 'Living wall systems'),
-- CONFLICT: Lucas (16) is on project 22 until Aug 10, overlaps with project 28
(43, 29, 14, '2025-08-04', '2025-09-05', 100, 'Fall Prep Lead', 'Seasonal transition'),
(44, 29, 15, '2025-08-04', '2025-09-05', 100, 'Maintenance Support', 'Bed preparation'),
(45, 30, 18, '2025-08-11', '2025-09-25', 50, 'Harvest Displays', 'Produce arrangements'),
(46, 31, 5, '2025-08-18', '2025-10-30', 100, 'Wildflower Expert', 'Meadow establishment'),
-- CONFLICT: Ava (5) is on project 26 until Sep 10, overlaps with project 31
(47, 32, 20, '2025-08-25', '2025-11-15', 100, 'Water Garden Complex', 'Connected water features');
-- CONFLICT: Henry (20) is on project 17 until Aug 15, quick turnaround to project 32

-- September-October Assignments (Fall rush)
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(48, 33, 7, '2025-09-01', '2025-10-15', 100, 'Bulb Installation Lead', 'Fall bulb planting'),
(49, 33, 8, '2025-09-01', '2025-10-15', 100, 'Planting Team', 'Mass bulb planting'),
(50, 33, 9, '2025-09-01', '2025-10-15', 100, 'Planting Team', 'Mass bulb planting'),
(51, 34, 18, '2025-09-08', '2025-10-31', 100, 'Pumpkin Expert', 'Pumpkin patch creation'),
-- CONFLICT: Jackson (18) is at 50% on project 30, now needs 100% on project 34
(52, 35, 12, '2025-09-15', '2025-11-30', 100, 'Tree Campaign Lead', 'City tree planting'),
-- CONFLICT: William (12) is on project 27 until Oct 15, overlaps with project 35
(53, 35, 13, '2025-09-15', '2025-11-30', 100, 'Tree Planting Team', 'Urban forest crew'),
-- CONFLICT: Amelia (13) is also on project 27 until Oct 15
(54, 36, 1, '2025-09-22', '2025-11-10', 100, 'Maple Garden Designer', 'Japanese maple collection'),
(55, 37, 2, '2025-10-06', '2025-12-15', 100, 'Winter Interest Designer', 'Four-season garden'),
(56, 38, 14, '2025-10-13', '2025-11-10', 100, 'Winterization Lead', 'Greenhouse prep'),
(57, 39, 1, '2025-10-20', '2025-11-25', 50, 'Holiday Design', 'Light display planning'),
-- CONFLICT: Emma (1) is on project 36 until Nov 10, overlaps with project 39
(58, 40, 3, '2025-10-27', '2025-12-20', 75, 'Compost Science', 'Composting systems');

-- November-December Assignments (Winter season)
INSERT INTO assignments (id, project_id, resource_id, start_date, end_date, allocation_percentage, role, notes) VALUES
(59, 41, 16, '2025-11-03', '2026-01-15', 100, 'Indoor Garden Rooms', 'Interior vertical gardens'),
(60, 42, 4, '2025-11-10', '2025-12-20', 100, 'Winter Vegetables', 'Cold frame crops'),
(61, 43, 14, '2025-11-17', '2025-12-10', 100, 'Tool Maintenance', 'Equipment service'),
-- CONFLICT: Benjamin (14) just finished project 38 on Nov 10, quick turnaround
(62, 44, 3, '2025-11-24', '2025-12-31', 50, 'Seed Planning', 'Next year seed orders'),
(63, 45, 15, '2025-12-01', '2025-12-20', 50, 'Wreath Workshop', 'Community program'),
(64, 46, 2, '2025-12-08', '2025-12-31', 100, 'Year-End Review', 'Garden documentation'),
-- CONFLICT: Oliver (2) is on project 37 until Dec 15, overlaps with project 46
(65, 47, 1, '2025-12-15', '2026-01-31', 100, '2026 Planning', 'Next year designs');
-- CONFLICT: Emma (1) is on project 39 until Nov 25, then project 47 starting Dec 15

-- Add Resource Unavailability (table may not exist)
-- INSERT INTO resource_unavailability (id, resource_id, start_date, end_date, reason, notes) VALUES
(1, 1, '2025-07-01', '2025-07-07', 'VACATION', 'Summer vacation - Emma'),
(2, 3, '2025-08-15', '2025-08-22', 'VACATION', 'Family vacation - Sophia'),
(3, 5, '2025-04-15', '2025-04-18', 'TRAINING', 'Pollinator habitat certification'),
(4, 10, '2025-09-20', '2025-09-27', 'VACATION', 'Anniversary trip - Ethan'),
(5, 12, '2025-06-10', '2025-06-14', 'TRAINING', 'ISA Arborist certification'),
(6, 14, '2025-12-23', '2025-12-31', 'VACATION', 'Holiday break - Benjamin'),
(7, 18, '2025-11-22', '2025-11-29', 'VACATION', 'Thanksgiving week - Jackson'),
(8, 20, '2025-05-26', '2025-05-30', 'SICK', 'Medical leave - Henry');

-- Update project notes with special requirements
UPDATE projects SET notes = 'Requires structural engineering approval. Wind study critical.' WHERE id = 1;
UPDATE projects SET notes = 'Community engagement required. Weekend workshops planned.' WHERE id = 2;
UPDATE projects SET notes = 'Temperature control critical. Monitor daily during transition.' WHERE id = 3;
UPDATE projects SET notes = 'School schedule dependent. Must complete before summer break.' WHERE id = 4;
UPDATE projects SET notes = 'CEO personal project. Weekly progress updates required.' WHERE id = 5;
UPDATE projects SET notes = 'Multiple site coordination. Travel between locations.' WHERE id = 6;
UPDATE projects SET notes = 'Weather dependent. May need schedule adjustment.' WHERE id = 10;
UPDATE projects SET notes = 'State contract. Strict compliance requirements.' WHERE id = 11;
UPDATE projects SET notes = 'Therapeutic design. Coordinate with medical staff.' WHERE id = 12;
UPDATE projects SET notes = 'Organic certification required. No chemicals.' WHERE id = 13;
UPDATE projects SET notes = 'Grant funded. Quarterly reporting required.' WHERE id = 15;
UPDATE projects SET notes = 'High visibility location. Media coverage expected.' WHERE id = 17;
UPDATE projects SET notes = 'Prototype project. Document all processes.' WHERE id = 18;
UPDATE projects SET notes = 'Historic property. Preservation guidelines apply.' WHERE id = 19;
UPDATE projects SET notes = 'Water restrictions apply. Drought contingency needed.' WHERE id = 21;
UPDATE projects SET notes = 'Climate controlled environment. HVAC coordination.' WHERE id = 22;
UPDATE projects SET notes = 'Food safety protocols required.' WHERE id = 23;
UPDATE projects SET notes = 'Accessibility requirements. ADA compliant.' WHERE id = 24;
UPDATE projects SET notes = 'Tournament schedule conflict. Work around events.' WHERE id = 25;
UPDATE projects SET notes = 'Elevation challenges. Special equipment needed.' WHERE id = 26;
UPDATE projects SET notes = 'Heritage varieties only. Source verification required.' WHERE id = 27;
UPDATE projects SET notes = 'Structural load limits. Engineering approval required.' WHERE id = 28;
UPDATE projects SET notes = 'Time-sensitive planting window.' WHERE id = 33;
UPDATE projects SET notes = 'Educational component required. Signage needed.' WHERE id = 34;
UPDATE projects SET notes = 'Political visibility. Mayor attending launch.' WHERE id = 35;
UPDATE projects SET notes = 'Specimen quality critical. Premium plants only.' WHERE id = 36;
UPDATE projects SET notes = 'Four-season interest required. Year-round appeal.' WHERE id = 37;
UPDATE projects SET notes = 'All facilities must remain operational.' WHERE id = 38;
UPDATE projects SET notes = 'Coordinate with holiday events team.' WHERE id = 39;
UPDATE projects SET notes = 'Environmental permits required.' WHERE id = 40;
UPDATE projects SET notes = 'Budget constraints. Value engineering needed.' WHERE id = 42;
UPDATE projects SET notes = 'Safety certification required for all tools.' WHERE id = 43;
UPDATE projects SET notes = 'Strategic planning session. Executive attendance.' WHERE id = 47;

-- Add project dependencies (table may not exist)
-- Commented out for compatibility

-- Create project milestones (table may not exist)
-- INSERT INTO project_milestones (id, project_id, name, target_date, status, description) VALUES
(1, 1, 'Design Approval', '2025-02-14', 'PENDING', 'Client sign-off on rooftop design'),
(2, 1, 'Structure Complete', '2025-03-15', 'PENDING', 'All containers and structures installed'),
(3, 1, 'First Harvest', '2025-06-01', 'PENDING', 'First vegetables ready for harvest'),
(4, 5, 'Meditation Space Complete', '2025-04-30', 'PENDING', 'Zen garden meditation area finished'),
(5, 9, 'Canopy Layer Planted', '2025-05-15', 'PENDING', 'All fruit trees installed'),
(6, 9, 'Guild System Established', '2025-06-30', 'PENDING', 'Companion planting complete'),
(7, 11, 'Phase 1 Complete', '2025-05-01', 'PENDING', 'First 5 miles planted'),
(8, 18, 'Growing System Operational', '2025-07-01', 'PENDING', 'Hydroponic system running'),
(9, 18, 'First Commercial Harvest', '2025-08-15', 'PENDING', 'Ready for market sales'),
(10, 25, 'System Automated', '2025-08-15', 'PENDING', 'Smart controllers online'),
(11, 27, 'Trees Established', '2025-09-30', 'PENDING', 'All trees planted and staked'),
(12, 35, '50% Complete', '2025-10-31', 'PENDING', '250 trees planted'),
(13, 35, 'Project Complete', '2025-11-30', 'PENDING', 'All 500 trees planted'),
(14, 41, 'First Room Complete', '2025-12-01', 'PENDING', 'Prototype garden room finished'),
(15, 47, 'Budget Approved', '2026-01-15', 'PENDING', '2026 budget finalized');

-- Add some completed tasks for projects that have started
UPDATE tasks SET status = 'COMPLETED', actual_hours = estimated_hours, completion_percentage = 100 
WHERE project_id IN (1, 2, 3) AND due_date < '2025-02-01';

UPDATE tasks SET status = 'IN_PROGRESS', actual_hours = estimated_hours * 0.5, completion_percentage = 50 
WHERE project_id IN (1, 2, 3) AND due_date >= '2025-02-01' AND due_date < '2025-02-15';

-- Update some project statuses to show variety
UPDATE projects SET status = 'COMPLETED' WHERE id IN (3);
UPDATE projects SET status = 'ON_HOLD' WHERE id = 40;
UPDATE projects SET status = 'AT_RISK' WHERE id IN (11, 21, 32);

-- Add budget allocations (table may not exist)
-- INSERT INTO budget_allocations (id, project_id, category, amount, spent, committed, notes) VALUES
(1, 1, 'Materials', 45000, 12000, 28000, 'Containers, soil, irrigation'),
(2, 1, 'Labor', 65000, 18000, 35000, 'Design and installation'),
(3, 1, 'Equipment', 15000, 5000, 8000, 'Specialized rooftop equipment'),
(4, 9, 'Plants', 35000, 0, 25000, 'Trees, shrubs, perennials'),
(5, 9, 'Site Prep', 25000, 0, 15000, 'Grading and soil improvement'),
(6, 9, 'Labor', 50000, 0, 35000, 'Installation and establishment'),
(7, 18, 'Infrastructure', 85000, 0, 60000, 'Greenhouse and systems'),
(8, 18, 'Technology', 45000, 0, 30000, 'Automation and controls'),
(9, 18, 'Operating', 55000, 0, 20000, 'First year operations'),
(10, 35, 'Trees', 95000, 0, 75000, '500 trees with stakes'),
(11, 35, 'Labor', 70000, 0, 50000, 'Planting and establishment'),
(12, 35, 'Maintenance', 20000, 0, 10000, 'First year care');