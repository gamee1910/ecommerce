-- V1__init_products_schema.sql
-- products_db: Product Service schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE categories
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL UNIQUE,
    slug       VARCHAR(255) NOT NULL UNIQUE,
    parent_id  UUID         REFERENCES categories (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE products
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    name           VARCHAR(255)   NOT NULL,
    slug           VARCHAR(255)   NOT NULL UNIQUE,
    description    TEXT,
    price          NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category_id    UUID           REFERENCES categories (id) ON DELETE SET NULL,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_price ON products (price);
CREATE INDEX idx_products_is_active ON products (is_active);
-- Full-text search index
CREATE INDEX idx_products_name_fts ON products USING GIN (to_tsvector('english', name));

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE
    ON products
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at();

-- Seed data
INSERT INTO categories (name, slug)
VALUES ('Electronics', 'electronics'),
       ('Clothing', 'clothing'),
       ('Books', 'books');

INSERT INTO categories (name, slug, parent_id)
VALUES ('Smartphones', 'smartphones', (SELECT id FROM categories WHERE slug = 'electronics')),
       ('Laptops', 'laptops', (SELECT id FROM categories WHERE slug = 'electronics'));
