-- Add refund tracking fields for cancel-after-paid flow
-- SQL Server

ALTER TABLE orders ADD refund_status VARCHAR(30) NULL;
ALTER TABLE orders ADD refund_requested_at DATETIME2 NULL;
ALTER TABLE orders ADD refund_processed_at DATETIME2 NULL;
ALTER TABLE orders ADD refund_bank_account_number NVARCHAR(50) NULL;
ALTER TABLE orders ADD refund_bank_name NVARCHAR(100) NULL;
ALTER TABLE orders ADD refund_bank_account_holder NVARCHAR(100) NULL;
ALTER TABLE orders ADD refund_note NVARCHAR(MAX) NULL;
