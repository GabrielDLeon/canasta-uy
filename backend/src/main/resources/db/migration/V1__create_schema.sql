CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    product_id INT PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    specification VARCHAR(500),
    category_id INT NOT NULL,
    brand VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
    CONSTRAINT uk_product_name UNIQUE(name)
);

CREATE TABLE prices (
    product_id INT NOT NULL,
    date DATE NOT NULL,
    price_min NUMERIC(10, 2) NOT NULL,
    price_max NUMERIC(10, 2) NOT NULL,
    price_avg NUMERIC(10, 2) NOT NULL,
    price_median NUMERIC(10, 2) NOT NULL,
    price_std NUMERIC(10, 2),
    store_count INT DEFAULT 0,
    offer_count INT DEFAULT 0,
    offer_percentage NUMERIC(5, 2) DEFAULT 0.0,
    PRIMARY KEY (product_id, date),
    CONSTRAINT fk_prices_product FOREIGN KEY (product_id) REFERENCES products(product_id),
    CONSTRAINT ck_price_range CHECK (price_min <= price_avg AND price_avg <= price_max),
    CONSTRAINT ck_offer_percentage CHECK (offer_percentage >= 0 AND offer_percentage <= 100),
    CONSTRAINT ck_prices_positive CHECK (price_min >= 0 AND price_max >= 0)
);

CREATE TABLE clients (
    client_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    api_key VARCHAR(64) NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

