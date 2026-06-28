CREATE TABLE orders (
    id UUID PRIMARY KEY,
    owner_subject VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(128),
    tenant_partition VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('CREATED', 'PROCESSING', 'COMPLETED', 'CANCELLED')),
    payment_status VARCHAR(20) NOT NULL
        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED')),
    fulfillment_status VARCHAR(20) NOT NULL
        CHECK (fulfillment_status IN ('UNFULFILLED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    version BIGINT NOT NULL CHECK (version >= 1),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_orders_owner_created
    ON orders(tenant_partition, owner_subject, created_at DESC, id DESC);

CREATE INDEX idx_orders_status_created
    ON orders(status, created_at DESC, id DESC);

CREATE TABLE order_items (
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_index INTEGER NOT NULL CHECK (item_index >= 0),
    sku VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, item_index)
);

CREATE TABLE order_idempotency (
    route_id VARCHAR(128) NOT NULL,
    tenant_partition VARCHAR(128) NOT NULL,
    owner_subject VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (route_id, tenant_partition, owner_subject, idempotency_key)
);

CREATE INDEX idx_order_idempotency_expiry
    ON order_idempotency(expires_at);
