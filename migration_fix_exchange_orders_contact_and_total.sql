/*
  Repair legacy EXCHANGE orders that were created without
  full_name / phone / address and normalize their totals to 0.

  This script is safe to run multiple times:
  - it only targets orders with payment_method = 'EXCHANGE'
  - it fills missing contact values
  - it normalizes EXCHANGE totals to 0 according to business rules
*/

-- Preview rows that are likely affected
SELECT
    new_o.order_id AS exchange_order_id,
    new_o.order_code AS exchange_order_code,
    new_o.full_name AS exchange_full_name,
    new_o.phone AS exchange_phone,
    new_o.address AS exchange_address,
    new_o.total_price AS exchange_total_price,
    new_o.final_price AS exchange_final_price,
    old_o.order_id AS source_order_id,
    old_o.order_code AS source_order_code,
    old_o.full_name AS source_full_name,
    old_o.phone AS source_phone,
    old_o.address AS source_address,
    old_o.total_price AS source_total_price,
    old_o.final_price AS source_final_price
FROM return_request rr
JOIN order_item old_oi
    ON old_oi.order_item_id = rr.order_item_id
JOIN orders old_o
    ON old_o.order_id = old_oi.order_id
JOIN orders new_o
    ON new_o.order_id = rr.replacement_order_id
WHERE rr.request_type = 'EXCHANGE'
  AND UPPER(ISNULL(new_o.payment_method, '')) = 'EXCHANGE'
  AND (
      NULLIF(LTRIM(RTRIM(ISNULL(new_o.full_name, ''))), '') IS NULL
      OR NULLIF(LTRIM(RTRIM(ISNULL(new_o.phone, ''))), '') IS NULL
      OR NULLIF(LTRIM(RTRIM(ISNULL(new_o.address, ''))), '') IS NULL
      OR new_o.final_price IS NULL
      OR new_o.final_price <> 0
      OR new_o.total_price IS NULL
      OR new_o.total_price <> 0
  );

-- Repair missing contact data and normalize EXCHANGE amount fields to zero
UPDATE new_o
SET
    new_o.full_name = COALESCE(
        NULLIF(LTRIM(RTRIM(new_o.full_name)), ''),
        NULLIF(LTRIM(RTRIM(old_o.full_name)), ''),
        NULLIF(LTRIM(RTRIM(ua.name)), ''),
        new_o.full_name
    ),
    new_o.phone = COALESCE(
        NULLIF(LTRIM(RTRIM(new_o.phone)), ''),
        NULLIF(LTRIM(RTRIM(old_o.phone)), ''),
        NULLIF(LTRIM(RTRIM(ua.phone)), ''),
        new_o.phone
    ),
    new_o.address = COALESCE(
        NULLIF(LTRIM(RTRIM(new_o.address)), ''),
        NULLIF(LTRIM(RTRIM(old_o.address)), ''),
        new_o.address
    ),
    new_o.total_price = 0,
    new_o.shipping_fee = 0,
    new_o.voucher_discount = 0,
    new_o.final_price = 0,
    new_o.deposit_amount = 0,
    new_o.deposit_type = COALESCE(NULLIF(new_o.deposit_type, ''), 'FULL'),
    new_o.deposit_payment_method = COALESCE(
        NULLIF(new_o.deposit_payment_method, ''),
        'EXCHANGE'
    ),
    new_o.remaining_payment_method = NULL,
    new_o.remaining_payment_status = 'PAID'
FROM orders new_o
JOIN return_request rr
    ON rr.replacement_order_id = new_o.order_id
JOIN order_item old_oi
    ON old_oi.order_item_id = rr.order_item_id
JOIN orders old_o
    ON old_o.order_id = old_oi.order_id
LEFT JOIN user_account ua
    ON ua.user_id = new_o.user_id
WHERE rr.request_type = 'EXCHANGE'
  AND UPPER(ISNULL(new_o.payment_method, '')) = 'EXCHANGE'
  AND (
      NULLIF(LTRIM(RTRIM(ISNULL(new_o.full_name, ''))), '') IS NULL
      OR NULLIF(LTRIM(RTRIM(ISNULL(new_o.phone, ''))), '') IS NULL
      OR NULLIF(LTRIM(RTRIM(ISNULL(new_o.address, ''))), '') IS NULL
      OR new_o.final_price IS NULL
      OR new_o.final_price <> 0
      OR new_o.total_price IS NULL
      OR new_o.total_price <> 0
  );

-- Verify after repair
SELECT
    new_o.order_id,
    new_o.order_code,
    new_o.full_name,
    new_o.phone,
    new_o.address,
    new_o.total_price,
    new_o.final_price
FROM orders new_o
WHERE UPPER(ISNULL(new_o.payment_method, '')) = 'EXCHANGE'
ORDER BY new_o.order_id DESC;
