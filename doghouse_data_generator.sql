-- Doghouse Installation Business Sample Data
-- Partnership with temperature-controlled 2-story doghouse builder
-- 10 field technicians rotating between in-shop builds and on-site installations

-- Add 10 new field service technicians
INSERT INTO resources (id, name, email, phone, department, type, skill_level, hourly_rate, utilization_target) VALUES
(51, 'Tech Team Alpha - Jake Morrison', 'jake.morrison@company.com', '555-0151', 'Field Services', 'FIELD_TECHNICIAN', 'SENIOR', 85.00, 85.0),
(52, 'Tech Team Alpha - Maria Santos', 'maria.santos@company.com', '555-0152', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0),
(53, 'Tech Team Alpha - David Chen', 'david.chen@company.com', '555-0153', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0),
(54, 'Tech Team Alpha - Sarah Johnson', 'sarah.johnson@company.com', '555-0154', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0),
(55, 'Tech Team Alpha - Mike Williams', 'mike.williams@company.com', '555-0155', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0),
(56, 'Tech Team Bravo - Tom Anderson', 'tom.anderson@company.com', '555-0156', 'Field Services', 'FIELD_TECHNICIAN', 'SENIOR', 85.00, 85.0),
(57, 'Tech Team Bravo - Lisa Park', 'lisa.park@company.com', '555-0157', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0),
(58, 'Tech Team Bravo - James Wilson', 'james.wilson@company.com', '555-0158', 'Field Services', 'FIELD_TECHNICIAN', 'MID', 75.00, 85.0),
(59, 'Tech Team Bravo - Emily Davis', 'emily.davis@company.com', '555-0159', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0),
(60, 'Tech Team Bravo - Chris Martinez', 'chris.martinez@company.com', '555-0160', 'Field Services', 'FIELD_TECHNICIAN', 'JUNIOR', 65.00, 85.0);

-- Create Doghouse Projects for 2025
-- Each project represents either a build week or an installation
-- Projects alternate between Team Alpha and Team Bravo
-- Build projects are 4 days (Mon-Thu), Install projects are 3 days (Mon-Wed)

-- January 2025 Doghouse Projects
INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES
-- Week 1 (Jan 6-10): Team Alpha builds, Team Bravo installs
(101, 'DH-2025-001-BUILD', 'Doghouse Build - Batch 001 (5 units) - Workshop', 2, '2025-01-06 08:00:00.000', '2025-01-09 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'First batch of temperature-controlled doghouses for Q1'),
(102, 'DH-2025-001-INSTALL', 'Doghouse Install - Johnson @ Oak Street', 3, '2025-01-06 08:00:00.000', '2025-01-08 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),
(103, 'DH-2025-002-INSTALL', 'Doghouse Install - Smith @ Elm Avenue', 3, '2025-01-08 08:00:00.000', '2025-01-10 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),

-- Week 2 (Jan 13-17): Team Bravo builds, Team Alpha installs
(104, 'DH-2025-002-BUILD', 'Doghouse Build - Batch 002 (5 units) - Workshop', 2, '2025-01-13 08:00:00.000', '2025-01-16 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'Second batch for January deliveries'),
(105, 'DH-2025-003-INSTALL', 'Doghouse Install - Davis @ Pine Road', 3, '2025-01-13 08:00:00.000', '2025-01-15 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),
(106, 'DH-2025-004-INSTALL', 'Doghouse Install - Wilson @ Maple Lane', 3, '2025-01-15 08:00:00.000', '2025-01-17 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),

-- Week 3 (Jan 20-24): Team Alpha builds, Team Bravo installs
(107, 'DH-2025-003-BUILD', 'Doghouse Build - Batch 003 (5 units) - Workshop', 2, '2025-01-20 08:00:00.000', '2025-01-23 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'Third batch with upgraded insulation'),
(108, 'DH-2025-005-INSTALL', 'Doghouse Install - Brown @ Cedar Street', 3, '2025-01-20 08:00:00.000', '2025-01-22 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),
(109, 'DH-2025-006-INSTALL', 'Doghouse Install - Taylor @ Birch Avenue', 3, '2025-01-22 08:00:00.000', '2025-01-24 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),

-- Week 4 (Jan 27-31): Team Bravo builds, Team Alpha installs
(110, 'DH-2025-004-BUILD', 'Doghouse Build - Batch 004 (5 units) - Workshop', 2, '2025-01-27 08:00:00.000', '2025-01-30 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'End of January production run'),
(111, 'DH-2025-007-INSTALL', 'Doghouse Install - Martinez @ Willow Way', 3, '2025-01-27 08:00:00.000', '2025-01-29 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),
(112, 'DH-2025-008-INSTALL', 'Doghouse Install - Anderson @ Ash Court', 3, '2025-01-29 08:00:00.000', '2025-01-31 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC');

-- February 2025 Doghouse Projects (continuing the pattern)
INSERT INTO projects (id, project_id, description, project_manager_id, start_date, end_date, status, budget, priority, completion_percentage, notes) VALUES
-- Week 1 (Feb 3-7): Team Alpha builds, Team Bravo installs
(113, 'DH-2025-005-BUILD', 'Doghouse Build - Batch 005 (5 units) - Workshop', 2, '2025-02-03 08:00:00.000', '2025-02-06 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'February production start - enhanced weatherproofing'),
(114, 'DH-2025-009-INSTALL', 'Doghouse Install - White @ Spruce Drive', 3, '2025-02-03 08:00:00.000', '2025-02-05 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),
(115, 'DH-2025-010-INSTALL', 'Doghouse Install - Harris @ Fir Lane', 3, '2025-02-05 08:00:00.000', '2025-02-07 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),

-- Week 2 (Feb 10-14): Team Bravo builds, Team Alpha installs
(116, 'DH-2025-006-BUILD', 'Doghouse Build - Batch 006 (5 units) - Workshop', 2, '2025-02-10 08:00:00.000', '2025-02-13 17:00:00.000', 'PLANNED', 12500.00, 'HIGH', 0, 'Valentine special edition with heart-shaped windows'),
(117, 'DH-2025-011-INSTALL', 'Doghouse Install - Clark @ Sycamore Street', 3, '2025-02-10 08:00:00.000', '2025-02-12 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, '2-story deluxe model with HVAC'),
(118, 'DH-2025-012-INSTALL', 'Doghouse Install - Lewis @ Hickory Road', 3, '2025-02-12 08:00:00.000', '2025-02-14 17:00:00.000', 'PLANNED', 4500.00, 'HIGH', 0, 'Valentine special with custom paint');

-- Create Assignments for January Doghouse Projects
-- Team Alpha (IDs 51-55) and Team Bravo (IDs 56-60) rotate weekly

-- Week 1: Alpha builds, Bravo installs
INSERT INTO assignments (project_id, resource_id, start_date, end_date, travel_out_days, travel_back_days, is_override, notes, created_at, updated_at) VALUES
-- Alpha team building
(101, 51, '2025-01-06', '2025-01-09', 0, 0, false, 'Team lead - workshop build', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 52, '2025-01-06', '2025-01-09', 0, 0, false, 'Build specialist - 1st floor assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 53, '2025-01-06', '2025-01-09', 0, 0, false, 'Build specialist - 2nd floor assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 54, '2025-01-06', '2025-01-09', 0, 0, false, 'HVAC installation', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 55, '2025-01-06', '2025-01-09', 0, 0, false, 'Roofing and palletizing', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
-- Bravo team installing (split between two sites)
(102, 56, '2025-01-06', '2025-01-08', 1, 0, false, 'Installation lead - Johnson site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 57, '2025-01-06', '2025-01-08', 1, 0, false, 'Foundation and assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 58, '2025-01-06', '2025-01-08', 1, 0, false, 'HVAC connection', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(103, 59, '2025-01-08', '2025-01-10', 1, 0, false, 'Installation support - Smith site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(103, 60, '2025-01-08', '2025-01-10', 1, 0, false, 'Commissioning and signoff', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),

-- Week 2: Bravo builds, Alpha installs
-- Bravo team building
(104, 56, '2025-01-13', '2025-01-16', 0, 0, false, 'Team lead - workshop build', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(104, 57, '2025-01-13', '2025-01-16', 0, 0, false, 'Build specialist - 1st floor assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(104, 58, '2025-01-13', '2025-01-16', 0, 0, false, 'Build specialist - 2nd floor assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(104, 59, '2025-01-13', '2025-01-16', 0, 0, false, 'HVAC installation', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(104, 60, '2025-01-13', '2025-01-16', 0, 0, false, 'Roofing and palletizing', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
-- Alpha team installing
(105, 51, '2025-01-13', '2025-01-15', 1, 0, false, 'Installation lead - Davis site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(105, 52, '2025-01-13', '2025-01-15', 1, 0, false, 'Foundation and assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(105, 53, '2025-01-13', '2025-01-15', 1, 0, false, 'HVAC connection', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(106, 54, '2025-01-15', '2025-01-17', 1, 0, false, 'Installation support - Wilson site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(106, 55, '2025-01-15', '2025-01-17', 1, 0, false, 'Commissioning and signoff', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),

-- Week 3: Alpha builds, Bravo installs
-- Alpha team building
(107, 51, '2025-01-20', '2025-01-23', 0, 0, false, 'Team lead - workshop build', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(107, 52, '2025-01-20', '2025-01-23', 0, 0, false, 'Build specialist - enhanced insulation', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(107, 53, '2025-01-20', '2025-01-23', 0, 0, false, 'Build specialist - 2nd floor assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(107, 54, '2025-01-20', '2025-01-23', 0, 0, false, 'HVAC installation', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(107, 55, '2025-01-20', '2025-01-23', 0, 0, false, 'Roofing and palletizing', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
-- Bravo team installing
(108, 56, '2025-01-20', '2025-01-22', 1, 0, false, 'Installation lead - Brown site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(108, 57, '2025-01-20', '2025-01-22', 1, 0, false, 'Foundation and assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(108, 58, '2025-01-20', '2025-01-22', 1, 0, false, 'HVAC connection', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(109, 59, '2025-01-22', '2025-01-24', 1, 0, false, 'Installation support - Taylor site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(109, 60, '2025-01-22', '2025-01-24', 1, 0, false, 'Commissioning and signoff', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),

-- Week 4: Bravo builds, Alpha installs
-- Bravo team building
(110, 56, '2025-01-27', '2025-01-30', 0, 0, false, 'Team lead - workshop build', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(110, 57, '2025-01-27', '2025-01-30', 0, 0, false, 'Build specialist - 1st floor assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(110, 58, '2025-01-27', '2025-01-30', 0, 0, false, 'Build specialist - 2nd floor assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(110, 59, '2025-01-27', '2025-01-30', 0, 0, false, 'HVAC installation', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(110, 60, '2025-01-27', '2025-01-30', 0, 0, false, 'Roofing and palletizing', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
-- Alpha team installing
(111, 51, '2025-01-27', '2025-01-29', 1, 0, false, 'Installation lead - Martinez site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(111, 52, '2025-01-27', '2025-01-29', 1, 0, false, 'Foundation and assembly', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(111, 53, '2025-01-27', '2025-01-29', 1, 0, false, 'HVAC connection', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(112, 54, '2025-01-29', '2025-01-31', 1, 0, false, 'Installation support - Anderson site', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(112, 55, '2025-01-29', '2025-01-31', 1, 0, false, 'Commissioning and signoff', '2025-01-01 00:00:00', '2025-01-01 00:00:00');

-- Create Tasks for Doghouse Projects
-- In-shop build tasks (4-day cycle)
INSERT INTO tasks (project_id, name, description, status, priority, estimated_hours, actual_hours, assigned_to, due_date, created_at, updated_at) VALUES
-- Build Batch 001 tasks
(101, 'Build 1st Floor Frame', 'Construct first floor frame and flooring for 5 units', 'PLANNED', 'HIGH', 16.0, 0.0, 52, '2025-01-07', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 'Build 2nd Floor Frame', 'Construct second floor frame and flooring for 5 units', 'PLANNED', 'HIGH', 16.0, 0.0, 53, '2025-01-08', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 'Install HVAC Systems', 'Install heating and cooling units in all 5 doghouses', 'PLANNED', 'HIGH', 20.0, 0.0, 54, '2025-01-08', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 'Install Roofing', 'Install insulated roofing on all units', 'PLANNED', 'HIGH', 12.0, 0.0, 55, '2025-01-09', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(101, 'Palletize for Shipping', 'Secure units on pallets for transport', 'PLANNED', 'HIGH', 8.0, 0.0, 55, '2025-01-09', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),

-- Installation 001 tasks (3-day cycle)
(102, 'Unpack and Stage', 'Unpack doghouse components and stage for assembly', 'PLANNED', 'HIGH', 3.0, 0.0, 56, '2025-01-06', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 'Set Foundation', 'Level ground and set concrete foundation blocks', 'PLANNED', 'HIGH', 4.0, 0.0, 57, '2025-01-06', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 'Assemble Structure', 'Assemble 1st and 2nd floors on foundation', 'PLANNED', 'HIGH', 6.0, 0.0, 57, '2025-01-07', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 'Pour Concrete Stoop', 'Pour and finish concrete entry stoop', 'PLANNED', 'HIGH', 3.0, 0.0, 57, '2025-01-07', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 'Connect HVAC Power', 'Wire HVAC to customer power supply', 'PLANNED', 'HIGH', 4.0, 0.0, 58, '2025-01-08', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 'Commission System', 'Test all systems and ensure proper operation', 'PLANNED', 'HIGH', 2.0, 0.0, 58, '2025-01-08', '2025-01-01 00:00:00', '2025-01-01 00:00:00'),
(102, 'Customer Signoff', 'Get customer approval and deliver invoice', 'PLANNED', 'HIGH', 1.0, 0.0, 56, '2025-01-08', '2025-01-01 00:00:00', '2025-01-01 00:00:00');

-- Summary of the Doghouse Installation business model:
-- - 10 field technicians split into Team Alpha (IDs 51-55) and Team Bravo (IDs 56-60)
-- - Teams rotate weekly between in-shop builds and on-site installations
-- - Build projects: 4 days (Mon-Thu) producing 5 units
-- - Installation projects: 3 days (Mon-Wed) per customer site
-- - Travel days included for on-site work (1 day out)
-- - Comprehensive task breakdown for both build and installation phases
-- - All projects linked to appropriate project managers
-- - Budget tracking: $2,500 per unit build cost, $4,500 per installation revenue