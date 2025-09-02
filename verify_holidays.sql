-- Check if table exists
SELECT name FROM sqlite_master WHERE type='table' AND name='company_holidays';

-- Count all holidays
SELECT COUNT(*) as total_holidays FROM company_holidays;

-- Count active holidays
SELECT COUNT(*) as active_holidays FROM company_holidays WHERE active = 1;

-- List all holidays
SELECT id, name, date, type, active, description 
FROM company_holidays 
ORDER BY date;

-- Check for August holidays
SELECT id, name, date, active 
FROM company_holidays 
WHERE date LIKE '2025-08%'
ORDER BY date;