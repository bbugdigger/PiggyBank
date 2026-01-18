-- Migration: Add normalBalance to accounts table
-- This migration adds the normal_balance column to existing accounts
-- and populates it based on account type

-- First, add the column as nullable
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS normal_balance VARCHAR(10);

-- Update all existing accounts to set their normal balance based on type
UPDATE accounts SET normal_balance = 'DEBIT' WHERE type IN ('ASSET', 'EXPENSE');
UPDATE accounts SET normal_balance = 'CREDIT' WHERE type IN ('LIABILITY', 'EQUITY', 'INCOME');

-- Now make it NOT NULL (PostgreSQL requires altering the column)
ALTER TABLE accounts ALTER COLUMN normal_balance SET NOT NULL;

-- Add void columns to transactions if they don't exist
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS num VARCHAR(50);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS voided BOOLEAN DEFAULT false;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS void_reason VARCHAR(255);

-- Add reconcile_status to splits if it doesn't exist
ALTER TABLE splits ADD COLUMN IF NOT EXISTS reconcile_status VARCHAR(15) DEFAULT 'NEW';
