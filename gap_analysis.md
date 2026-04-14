
# Gap Analysis & Improvement Recommendations

Based on the detailed requirements provided and the current project structure, here is an analysis of what is implemented and what is missing or can be improved.

## 1. Requirement Coverage Analysis

| Actor | Requirement | Current Status | Gaps / Improvements Needed |
| :--- | :--- | :--- | :--- |
| **Customer** | View/filter glasses, 2D/3D images | Partially Implemented | `Product` and `ProductVariant` exist. <br/>**Gap**: `image_url` is a single string. Need a `ProductImage` table to support multiple images (2D/3D angles). <br/>**Gap**: No `Review` or `Rating` entity. |
| **Customer** | Order (Available, Pre-order, Custom Lens) | Partially Implemented | `Order`, `PreOrder`, `Prescription` entities exist. <br/>**Gap**: Business logic in Service layer to handle different workflows is missing. |
| **Customer** | Manage Account, Order History, Returns | Partially Implemented | `UserAccount` exists. <br/>**Gap**: No `ReturnRequest` or `Warranty` entities to handle post-purchase flows. |
| **Sales Staff**| Check prescription, process orders | Missing Logic | Data structure supports it (`Prescription` entity). <br/>**Gap**: No workflow/status management for "Prescription Verification". Need specific status flags or a separate `OrderHistory` log to track staff actions. |
| **Operations** | Package, Ship, Inventory | Partially Implemented | `Shipment` entity exists. Inventory is tracked in `ProductVariant`. <br/>**Gap**: No "Batch" or detailed inventory logging (entry/exit logs). |
| **Manager** | Manage Policies, Promotions, Revenue | Missing | **Gap**: No `Promotion`, `DiscountCode`, or `Policy` entities. Revenue is derived from `Payment`, but might need aggregation tables for performance. |
| **Admin** | Configure System | Missing | **Gap**: No `SystemConfig` table for dynamic settings (e.g., banner images, global alerts). |

## 2. Structural Improvements

### A. Database Entity Enhancements
1.  **Product Images**: Create `ProductImage` (One-to-Many with Variant) to store multiple URLs per variant, differentiating between "Front", "Side", "3D_Model".
2.  **Returns & Warranty**: Create `ReturnRequest` and `WarrantyClaim` entities.
3.  **Promotions**: Create `Promotion` (e.g., "% off", "fixed amount") and `Voucher`.
4.  **Feedback**: Create `Review` entity linked to `Product`.
5.  **Notifications**: Create `Notification` entity to store system messages for users (e.g., "Order Shipped").

### B. Architecture & Safety
1.  **Security Layer (Critical)**:
    -   Currently have no security configuration.
    -   **Action**: Implement Spring Security with JWT or Session-based auth.
    -   **Action**: Create `Role` and `Permission` management if simple strings are insufficient.
2.  **Service Layer**:
    -   Currently empty.
    -   **Action**: Need `OrderService` (complex logic for splitting orders), `ProductService` (filtering), `UserService` (auth).
3.  **API Layer (Controllers)**:
    -   Currently empty.
    -   **Action**: specific REST controllers for each actor type (e.g., `AdminProductController` vs `PublicProductController`).

### C. Specific "Glasses" Nuances
-   **Prescription Validation**: The `Prescription` entity is good, but you might need a flag `is_verified` (boolean) to allow Sales Staff to lock it after checking.
-   **Combos**: Request mentions "Manage prices of... combos".
    -   **Action**: Consider a `Combo` or `Bundle` entity, or a linkage between `Product` (Frame) and `LensOption` that offers a discount.

## 3. Recommended Roadmap

1.  **Phase 1: Security Foundation** (Setup Spring Security, Auth Controller).
2.  **Phase 2: Core Service Logic** (Implement Product matching, Cart logic, basic Ordering).
3.  **Phase 3: Expanded Entities** (Add Promotions, Returns, multiple Images).
4.  **Phase 4: Staff Workflows** (Dashboard services for processing orders).

