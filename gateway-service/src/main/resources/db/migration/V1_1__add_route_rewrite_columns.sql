ALTER TABLE gateway_routes
    ADD COLUMN IF NOT EXISTS rewrite_regex VARCHAR(500),
    ADD COLUMN IF NOT EXISTS rewrite_replacement VARCHAR(500);
