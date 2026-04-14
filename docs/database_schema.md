# Database Schema Documentation

This document outlines the database schema for the Glasses Shop application, automatically generated from the JPA Entities.

## Entity Relationship Diagram
## 1. User Management

### `user_account`
Stores user authentication and profile information.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `user_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `name` | `VARCHAR(255)` | | Full name |
| `email` | `VARCHAR(255)` | `UNIQUE` | User email for login |
| `phone` | `VARCHAR(255)` | | Phone number |
| `role` | `VARCHAR(255)` | | `ADMIN`, `CUSTOMER`, `STAFF` |
| `password_hash` | `VARCHAR(255)` | | Hashed password |
| `account_status` | `VARCHAR(255)` | | `ACTIVE`, `LOCKED`, etc. |
| `created_at` | `DATETIME` | | Account creation timestamp |

### `address`
Stores user shipping and billing addresses.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `address_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `user_id` | `BIGINT` | `FK -> user_account` | Owner of the address |
| `street` | `VARCHAR(255)` | | Street address |
| `city` | `VARCHAR(255)` | | City |
| `state` | `VARCHAR(255)` | | State/Province |
| `zip_code` | `VARCHAR(255)` | | Postal code |
| `country` | `VARCHAR(255)` | | Country |

### `notification` (New)
System alerts for users.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `notification_id`| `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `user_id` | `BIGINT` | `FK -> user_account` | Recipient |
| `title` | `VARCHAR` | | Short title |
| `message` | `TEXT` | | Notification body |
| `is_read` | `BIT` | | Read status |

---

## 2. Product Catalog

### `product`
Core product information.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `product_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `product_type` | `VARCHAR(255)` | | Enum: `FRAME`, `LENS`, `ACCESSORY` |
| `name` | `VARCHAR(255)` | | Product name |
| `brand` | `VARCHAR(255)` | | Brand name |
| `description` | `TEXT` | | Detailed description |
| `is_prescription_supported` | `BOOLEAN` | | If true, supports prescription lenses |
| `created_at` | `DATETIME` | | Record creation timestamp |

### `product_variant`
Specific variations of a product (e.g., color, size).

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `variant_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `product_id` | `BIGINT` | `FK -> product` | Parent product |
| `price` | `DECIMAL` | | Base price of this variant |
| `stock_quantity`| `INT` | | Current stock level |
| `frame_size` | `VARCHAR(255)` | | e.g., "Small", "Medium" |
| `color` | `VARCHAR(255)` | | e.g., "Black", "Gold" |
| `material` | `VARCHAR(255)` | | e.g., "Metal", "Plastic" |
| `image_url` | `TEXT` | | **Legacy** URL to product image |
| `status` | `VARCHAR(255)` | | `AVAILABLE`, `OUT_OF_STOCK` |

### `product_image` (New)
Multiple images per variant, including 3D models.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `image_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `variant_id` | `BIGINT` | `FK -> product_variant`| Parent variant |
| `image_url` | `TEXT` | | URL to resource |
| `image_type` | `VARCHAR` | | `FRONT`, `SIDE`, `ANGLE`, `MODEL_3D` |
| `display_order` | `INT` | | Order in carousel |

### `lens_option`
Available options for prescription lenses.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `lens_option_id`| `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `type` | `VARCHAR(255)` | | e.g., "Single Vision" |
| `thickness` | `VARCHAR(255)` | | e.g., "1.67 High Index" |
| `coating` | `VARCHAR(255)` | | e.g., "Anti-Reflective" |
| `color` | `VARCHAR(255)` | | e.g., "Clear", "Transitions" |
| `price` | `DECIMAL` | | Additional cost for this lens |

### `review` (New)
Customer reviews and ratings.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `review_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `product_id` | `BIGINT` | `FK -> product` | Reviewed product |
| `user_id` | `BIGINT` | `FK -> user_account` | Reviewer |
| `rating` | `INT` | | 1-5 Stars |
| `comment` | `TEXT` | | Textual review |
| `created_at` | `DATETIME` | | Timestamp |

### `promotion` (New)
Discounts and vouchers.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `promotion_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `code` | `VARCHAR` | `UNIQUE` | Promo code |
| `discount_type` | `VARCHAR` | | `PERCENTAGE`, `FIXED_AMOUNT` |
| `discount_value`| `DECIMAL` | | Amount/Percent off |
| `start/end_date`| `DATETIME` | | Validity period |
| `is_active` | `BIT` | | On/Off toggle |

---

## 3. Shopping Cart

*(Same as previous)*

---

## 4. Order Processing

### `orders`
Placed orders.

*(Same as previous)*

### `return_request` (New)
Handles returns, refunds, and warranties.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `request_id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | Unique identifier |
| `order_id` | `BIGINT` | `FK -> orders` | Related order |
| `reason` | `TEXT` | | Customer reason |
| `status` | `VARCHAR` | | `PENDING`, `APPROVED`, etc. |
| `requested_at` | `DATETIME` | | Timestamp |

### `order_item`, `pre_order`, `prescription`, `payment`, `shipment`
*(Same as previous)*
