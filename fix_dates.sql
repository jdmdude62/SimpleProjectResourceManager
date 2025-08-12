-- Fix date format in projects table
UPDATE projects 
SET start_date = start_date || ' 00:00:00.000',
    end_date = end_date || ' 00:00:00.000'
WHERE start_date NOT LIKE '% %';

-- Fix date format in assignments table
UPDATE assignments 
SET start_date = start_date || ' 00:00:00.000',
    end_date = end_date || ' 00:00:00.000'
WHERE start_date NOT LIKE '% %';