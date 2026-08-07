-- V1__init_notifications_schema.sql
-- notifications_db: Notification Service schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notifications
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255) NOT NULL UNIQUE, -- idempotency key (Kafka topic + partition + offset OR event UUID)
    type            VARCHAR(50)  NOT NULL,         -- e.g. USER_REGISTERED, ORDER_CREATED
    recipient_email VARCHAR(255) NOT NULL,
    subject         VARCHAR(500),
    body            TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    retry_count     INT          NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ
);

CREATE INDEX idx_notifications_event_id ON notifications (event_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_type ON notifications (type);
