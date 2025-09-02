-- List Tech Team resources before deletion
SELECT id, name, active, resource_type_id FROM resources WHERE name LIKE 'Tech Team%' ORDER BY name;

-- Delete Tech Team resources
DELETE FROM resources WHERE name LIKE 'Tech Team%';

-- Verify deletion
SELECT COUNT(*) AS remaining_tech_team FROM resources WHERE name LIKE 'Tech Team%';