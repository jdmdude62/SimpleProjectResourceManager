-- Add technician unavailability and company holiday management
-- Migration 002: Add unavailability and holidays tables
-- Created: 2025-08-07

PRAGMA foreign_keys = ON;

-- Technician Unavailability table
CREATE TABLE technician_unavailability (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_id INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL, -- VACATION, SICK_LEAVE, TRAINING, etc.
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(200),
    description TEXT,
    approved BOOLEAN DEFAULT FALSE,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(100), -- e.g., 'WEEKLY:FRIDAY', 'MONTHLY:LAST_FRIDAY'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
);

-- Company Holidays table
CREATE TABLE company_holidays (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    type VARCHAR(50) NOT NULL, -- FEDERAL, COMPANY, FLOATING, etc.
    description TEXT,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_rule VARCHAR(100), -- e.g., 'ANNUAL:THIRD_MONDAY_JANUARY'
    working_holiday_allowed BOOLEAN DEFAULT FALSE, -- Can people work through this holiday?
    department VARCHAR(50), -- null means company-wide
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Holiday Override table - tracks when resources are assigned to work during holidays
CREATE TABLE holiday_work_overrides (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    holiday_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    assignment_id INTEGER, -- Which assignment caused the override
    override_reason VARCHAR(200) NOT NULL,
    authorized_by VARCHAR(100),
    authorized_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (holiday_id) REFERENCES company_holidays(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_unavailability_resource ON technician_unavailability(resource_id);
CREATE INDEX idx_unavailability_dates ON technician_unavailability(start_date, end_date);
CREATE INDEX idx_unavailability_type ON technician_unavailability(type);
CREATE INDEX idx_unavailability_approved ON technician_unavailability(approved);

CREATE INDEX idx_holidays_date ON company_holidays(date);
CREATE INDEX idx_holidays_type ON company_holidays(type);
CREATE INDEX idx_holidays_department ON company_holidays(department);
CREATE INDEX idx_holidays_active ON company_holidays(active);

CREATE INDEX idx_holiday_overrides_holiday ON holiday_work_overrides(holiday_id);
CREATE INDEX idx_holiday_overrides_resource ON holiday_work_overrides(resource_id);
CREATE INDEX idx_holiday_overrides_assignment ON holiday_work_overrides(assignment_id);

-- Sample federal holidays for 2025 (commented out for now - can be added via UI)
-- INSERT INTO company_holidays (name, date, type, description, is_recurring, working_holiday_allowed) VALUES 
--     ('New Years Day', '2025-01-01', 'FEDERAL', 'Federal Holiday - New Years Day', 1, 0),
--     ('Independence Day', '2025-07-04', 'FEDERAL', 'Federal Holiday - July 4th', 1, 1),
--     ('Christmas Day', '2025-12-25', 'FEDERAL', 'Federal Holiday - December 25th', 1, 0);