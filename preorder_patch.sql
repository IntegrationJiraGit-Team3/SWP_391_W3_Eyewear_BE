ALTER TABLE orders
ADD remaining_payment_status VARCHAR(50) NULL;

ALTER TABLE order_item
ADD stock_deducted BIT NULL DEFAULT 0;

UPDATE order_item
SET stock_deducted = 0
WHERE stock_deducted IS NULL;

UPDATE orders
SET remaining_payment_status = 'NOT_REQUIRED'
WHERE remaining_payment_status IS NULL;