
A comprehensive e-commerce platform for purchasing glasses, frames, and accessories, built with Spring Boot.

## Overview

This application manages the end-to-end process of an online optical shop, including user management, product catalog with variants (frames, lens options), prescription handling, shopping cart, and order processing.

## Key Features

- **User Accounts**: Registration, login, and profile management with multiple addresses.
- **Product Catalog**:
    - Browse products by category (Frames, Lenses, Accessories).
    - View product variants (Color, Size, Material).
    - **3D Model Support**: Data structure supports 3D model references for virtual try-on.
- **Prescriptions**: Users can attach prescription details to compatible products.
- **Shopping Cart**: Manage items, apply lens options, and view total costs.
- **Order Processing**: Checkout flow, order status tracking (Pending -> Completed), and return requests.
- **Discount System**: Apply promotion codes for percentage or fixed-value discounts.

## Technology Stack

- **Language**: Java 25
- **Framework**: Spring Boot 4.x
    - Spring Data JPA
    - Spring WebMVC
    - Spring Security (Implied by User/Role structure, pending implementation)
- **Database**: Microsoft SQL Server
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven

## Documentation

- **[Database Schema](docs/database_schema.md)**: Detailed entity-relationship diagrams and table definitions.
- **[Business Rules](docs/business_rules.md)**: specific logic for pricing, orders, and user roles.
- **[Gap Analysis](gap_analysis.md)**: Current state vs. requirements analysis.

## Getting Started

### Prerequisites

- JDK 25 or higher
- Maven 3.8+
- Microsoft SQL Server

### Installation to Run

1. **Clone the repository**
   ```bash
   git clone https://github.com/nminhnhanz/SWP302_NHOM5_5.git
   cd swp-glasseshop
   ```

2. **Configure Database**
   Update `src/main/resources/application.properties` (or `application.yml`) with your database credentials:
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=glasseshop
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Build the Application**
   ```bash
   ./mvnw clean install
   ```

4. **Run the Application**
   ```bash
   ./mvnw spring-boot:run
   ```

## Project Structure

```
src/main/java/com/fpt/glasseshop
├── config/       # Configuration classes
├── controller/   # Web controllers
├── entity/       # JPA Entities (Database model)
├── repository/   # Data Access Layer
├── service/      # Business Logic
└── ...
```

## License

Private Repository. All rights reserved.
