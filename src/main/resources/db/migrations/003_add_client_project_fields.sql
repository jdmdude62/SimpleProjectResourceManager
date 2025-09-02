-- Add client project ID and description fields to projects table
ALTER TABLE projects ADD COLUMN client_project_id TEXT;
ALTER TABLE projects ADD COLUMN client_project_description TEXT;