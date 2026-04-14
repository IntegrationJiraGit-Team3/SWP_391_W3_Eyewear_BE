-- Migration: Remove PD and Prism columns
-- Date: 2026-04-09
-- Description: Remove PD (Pupillary Distance) and Prism fields from database
-- since they were removed from the frontend UI

-- 1. Remove 'pd' column from prescription table
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'prescription' AND COLUMN_NAME = 'pd')
    ALTER TABLE prescription DROP COLUMN pd;

-- 2. Remove 'pd' column from order_item table (if exists)
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'order_item' AND COLUMN_NAME = 'pd')
    ALTER TABLE order_item DROP COLUMN pd;

-- 3. Remove 'pd' column from user_prescription table
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'user_prescription' AND COLUMN_NAME = 'pd')
    ALTER TABLE user_prescription DROP COLUMN pd;

-- 4. Remove Prism columns from user_prescription table
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'user_prescription' AND COLUMN_NAME = 'prism_left')
    ALTER TABLE user_prescription DROP COLUMN prism_left;

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'user_prescription' AND COLUMN_NAME = 'prism_right')
    ALTER TABLE user_prescription DROP COLUMN prism_right;

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'user_prescription' AND COLUMN_NAME = 'base_left')
    ALTER TABLE user_prescription DROP COLUMN base_left;

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'user_prescription' AND COLUMN_NAME = 'base_right')
    ALTER TABLE user_prescription DROP COLUMN base_right;

PRINT 'Migration completed: PD and Prism columns removed successfully';
