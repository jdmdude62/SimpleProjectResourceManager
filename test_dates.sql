-- Check date formats
SELECT 
    id,
    project_id,
    start_date,
    end_date,
    typeof(start_date) as start_type,
    typeof(end_date) as end_type
FROM projects
WHERE project_id LIKE 'GAR-%'
LIMIT 5;

-- Check for January 2025 projects
SELECT 
    id,
    project_id,
    start_date,
    end_date
FROM projects
WHERE date(start_date) <= date('2025-01-31') 
  AND date(end_date) >= date('2025-01-01')
LIMIT 10;