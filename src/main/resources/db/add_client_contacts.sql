-- Add client contact fields to projects table
-- Backup existing data first before running this script

ALTER TABLE projects ADD COLUMN contact_name TEXT;
ALTER TABLE projects ADD COLUMN contact_email TEXT;  -- Can contain semicolon-separated emails
ALTER TABLE projects ADD COLUMN contact_phone TEXT;
ALTER TABLE projects ADD COLUMN contact_company TEXT;
ALTER TABLE projects ADD COLUMN contact_role TEXT;   -- e.g., "Project Manager", "Owner", etc.
ALTER TABLE projects ADD COLUMN send_reports BOOLEAN DEFAULT 1;  -- Whether to send automated reports
ALTER TABLE projects ADD COLUMN report_frequency TEXT DEFAULT 'WEEKLY'; -- DAILY, WEEKLY, BIWEEKLY, MONTHLY
ALTER TABLE projects ADD COLUMN last_report_sent TEXT; -- Timestamp of last report

-- Create a separate table for client communication history
CREATE TABLE IF NOT EXISTS client_communications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id TEXT NOT NULL,
    communication_type TEXT NOT NULL, -- 'REPORT', 'EMAIL', 'CALL', 'MEETING'
    subject TEXT,
    content TEXT,
    sent_date TEXT NOT NULL,
    sent_to TEXT,  -- Email addresses
    sent_by TEXT,  -- User who sent it
    status TEXT,   -- 'SENT', 'FAILED', 'PENDING'
    FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_communications_project ON client_communications(project_id);
CREATE INDEX IF NOT EXISTS idx_communications_date ON client_communications(sent_date);