-- V4: Add webhook_secret column to merchants table for signing webhooks
ALTER TABLE merchants
ADD COLUMN webhook_secret VARCHAR(255);