CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE orders
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(15, 2) NOT NULL CHECK (total_amount >= 0),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    subtotal     NUMERIC(15, 2) NOT NULL
);

CREATE TABLE outbox_events
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_outbox_status ON outbox_events (status) WHERE status = 'PENDING';

CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE
    ON orders
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at();
