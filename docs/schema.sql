-- Database Schema for Glasses Shop
-- Generated from JPA Entities

CREATE TABLE user_account (
    user_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255),
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    role VARCHAR(50),
    password_hash VARCHAR(255),
    account_status VARCHAR(50),
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE address (
    address_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    street NVARCHAR(255),
    city NVARCHAR(100),
    state NVARCHAR(100),
    zip_code VARCHAR(20),
    country NVARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);

CREATE TABLE product (
    product_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_type VARCHAR(50), -- FRAME, LENS, ACCESSORY
    name NVARCHAR(255),
    brand NVARCHAR(255),
    description NVARCHAR(MAX),
    is_prescription_supported BIT,
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_variant (
    variant_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT,
    price DECIMAL(19, 2),
    stock_quantity INT,
    frame_size VARCHAR(50),
    color NVARCHAR(50),
    material NVARCHAR(50),
    image_url NVARCHAR(MAX),
    status VARCHAR(50),
    FOREIGN KEY (product_id) REFERENCES product(product_id)
);

CREATE TABLE lens_option (
    lens_option_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    type NVARCHAR(100),
    thickness NVARCHAR(100),
    coating NVARCHAR(100),
    color NVARCHAR(100),
    price DECIMAL(19, 2)
);

CREATE TABLE cart (
    cart_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    created_at DATETIME2,
    updated_at DATETIME2,
    FOREIGN KEY (user_id) REFERENCES user_account(user_id)
);

CREATE TABLE cart_item (
    cart_item_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id BIGINT,
    variant_id BIGINT,
    lens_option_id BIGINT,
    quantity INT,
    FOREIGN KEY (cart_id) REFERENCES cart(cart_id),
    FOREIGN KEY (variant_id) REFERENCES product_variant(variant_id),
    FOREIGN KEY (lens_option_id) REFERENCES lens_option(lens_option_id)
);

CREATE TABLE orders (
    order_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    order_date DATETIME2,
    status VARCHAR(50),
    total_price DECIMAL(19, 2),
    shipping_address_id BIGINT,
    billing_address_id BIGINT,
    payment_status VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES user_account(user_id),
    FOREIGN KEY (shipping_address_id) REFERENCES address(address_id),
    FOREIGN KEY (billing_address_id) REFERENCES address(address_id)
);

CREATE TABLE order_item (
    order_item_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT,
    variant_id BIGINT,
    lens_option_id BIGINT,
    quantity INT,
    unit_price DECIMAL(19, 2),
    fulfillment_type VARCHAR(50),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (variant_id) REFERENCES product_variant(variant_id),
    FOREIGN KEY (lens_option_id) REFERENCES lens_option(lens_option_id)
);

CREATE TABLE pre_order (
    preorder_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_item_id BIGINT UNIQUE,
    expected_arrival DATE,
    supplier_name NVARCHAR(255),
    status VARCHAR(50),
    FOREIGN KEY (order_item_id) REFERENCES order_item(order_item_id)
);

CREATE TABLE prescription (
    prescription_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_item_id BIGINT UNIQUE,
    sph_left DECIMAL(5, 2),
    sph_right DECIMAL(5, 2),
    cyl_left DECIMAL(5, 2),
    cyl_right DECIMAL(5, 2),
    axis_left INT,
    axis_right INT,
    pd DECIMAL(5, 2),
    doctor_name NVARCHAR(255),
    expiration_date DATE,
    status VARCHAR(50),
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_item_id) REFERENCES order_item(order_item_id)
);

CREATE TABLE payment (
    payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT,
    payment_method VARCHAR(50),
    amount DECIMAL(19, 2),
    status VARCHAR(50),
    transaction_reference VARCHAR(255),
    paid_at DATETIME2,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE TABLE shipment (
    shipment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT,
    carrier NVARCHAR(100),
    tracking_number VARCHAR(100),
    shipped_date DATE,
    delivered_date DATE,
    status VARCHAR(50),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
