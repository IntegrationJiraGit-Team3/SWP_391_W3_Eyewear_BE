# Business Rules & Logic

This document defines the core business rules and logic for the Glasses Shop application, inferred from the data model and requirements.

## 1. User Management

### 1.1. Accounts & Roles
- **Uniqueness**: Every user must have a unique email address.
- **Roles**:
    - `CUSTOMER`: Standard user who can browse and purchase.
    - `STAFF`: Can manage orders and products.
    - `ADMIN`: Full system access, including staff management.
- **One User, One Cart**: Each user account is associated with exactly one active shopping cart.

### 1.2. Addresses
- A user can store multiple addresses (shipping/billing).
- An address belongs to only one user.

## 2. Product Catalog

### 2.1. Product Structure
- **Hierarchy**: `Product` -> `ProductVariant` -> `ProductImage`.
- **Variants**: A product (e.g., "Ray-Ban Aviator") can have multiple variants differing by:
    - Color
    - Material
    - Frame Size
- **Pricing**: Base price is defined at the **Variant** level, not the Product level.
- **Stock**: Inventory is tracked at the **Variant** level.

### 2.2. Prescription Support
- Only products marked `is_prescription_supported = true` can be ordered with prescription lenses.
- **Lens Options**: Customers can select lenses (e.g., "Single Vision", "Blue Light Filter") which add to the base variant price.

## 3. Order Processing

### 3.1. Cart & Checkout
- **Item Computation**:
    - `Item Price` = `Variant Price` + `Lens Option Price` (if applicable).
    - `Total` = Sum of all Item Prices.
- **Prescriptions**: If a cart item requires a prescription, the user must provide prescription details (sphere, cylinder, axis, etc.) before checkout.

### 3.2. Promotions
- **Validity**: Promotion codes must be checked for:
    - Validity period (`start_date` <= now <= `end_date`).
    - `is_active` status.
- **Application**: Discount is applied either as a fixed amount or a percentage of the order total.

### 3.3. Order Lifecycle
- **Status Flow**: `PENDING` -> `PAID` -> `PROCESSING` -> `SHIPPED` -> `DELIVERED` -> `COMPLETED`.
- **Cancellation**: Orders can only be cancelled if they are in `PENDING` or `PAID` state, before shipping.

### 3.4. Returns
- **Window**: Returns can be requested within a specific timeframe after `DELIVERED` status (e.g., 30 days).
- **Approval**: Return requests (`PENDING`) must be approved (`APPROVED`) by STAFF before a refund is processed.

## 4. Notifications & Reviews
- **Alerts**: Users receive notifications for order status updates and promotions.
- **Reviews**: Only users who have purchased a product can verify-review it (logic to be enforced in service layer).
