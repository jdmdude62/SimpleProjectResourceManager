-- Add ldap_username column to resources table if it doesn't exist
ALTER TABLE resources ADD COLUMN ldap_username TEXT;