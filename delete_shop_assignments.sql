-- Count existing SHOP assignments
SELECT COUNT(*) as shop_assignment_count
FROM assignments a
JOIN projects p ON a.project_id = p.id
WHERE p.project_id = 'SHOP';

-- Delete all SHOP assignments
DELETE FROM assignments
WHERE project_id IN (SELECT id FROM projects WHERE project_id = 'SHOP');

-- Verify deletion
SELECT COUNT(*) as remaining_count
FROM assignments a
JOIN projects p ON a.project_id = p.id
WHERE p.project_id = 'SHOP';