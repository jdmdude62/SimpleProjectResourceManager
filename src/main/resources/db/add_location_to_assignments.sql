-- Add location field to assignments table for multi-location/phase project support
ALTER TABLE assignments ADD COLUMN location VARCHAR(255);

-- Add index for location-based queries
CREATE INDEX idx_assignments_location ON assignments(location);

-- Update existing assignments to have null location (no default needed)
-- Location will be optional and can be filled in as needed