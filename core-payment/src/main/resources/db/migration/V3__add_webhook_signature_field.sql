-- V3: Add webhook signature field to outbox_events
ALTER TABLE outbox_events ADD COLUMN signature VARCHAR(256) NOT NULL DEFAULT '';

-- Update existing records to have empty signature (will be populated on next webhook dispatch)
UPDATE outbox_events SET signature = '' WHERE signature IS NULL;

-- Remove default constraint for future inserts
ALTER TABLE outbox_events ALTER COLUMN signature DROP DEFAULT;
