-- Prices indexes
CREATE INDEX idx_prices_product ON prices(product_id);
CREATE INDEX idx_prices_date ON prices(date);
CREATE INDEX idx_prices_product_date ON prices(product_id, date);

-- Products indexes
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand ON products(brand);

-- Clients indexes
CREATE INDEX idx_clients_api_key ON clients(api_key);
CREATE INDEX idx_clients_email ON clients(email);

