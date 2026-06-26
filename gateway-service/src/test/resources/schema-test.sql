DROP ALL OBJECTS;

CREATE TABLE gateway_routes (
    id VARCHAR(120) PRIMARY KEY, category VARCHAR(20) NOT NULL, path_patterns TEXT NOT NULL,
    methods TEXT NOT NULL, target_uri VARCHAR(1000) NOT NULL, strip_prefix INTEGER NOT NULL,
    rewrite_regex VARCHAR(500), rewrite_replacement VARCHAR(500),
    route_order INTEGER NOT NULL, enabled BOOLEAN NOT NULL, authentication_types TEXT NOT NULL,
    required_roles TEXT NOT NULL, required_scopes TEXT NOT NULL, signing_required BOOLEAN NOT NULL,
    rate_limit_policy_id VARCHAR(120), ip_policy_id VARCHAR(120), risk_policy_id VARCHAR(120),
    connect_timeout_ms INTEGER NOT NULL, response_timeout_ms INTEGER NOT NULL,
    retry_enabled BOOLEAN NOT NULL, retry_max_attempts INTEGER NOT NULL, retry_methods TEXT NOT NULL,
    circuit_breaker_enabled BOOLEAN NOT NULL, circuit_breaker_name VARCHAR(120),
    audit_mode VARCHAR(40) NOT NULL, version BIGINT NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE api_clients (
    id UUID PRIMARY KEY, name VARCHAR(160) NOT NULL UNIQUE, owner VARCHAR(200) NOT NULL,
    tenant_id VARCHAR(120), status VARCHAR(20) NOT NULL, version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE api_keys (
    id UUID PRIMARY KEY, client_id UUID NOT NULL, prefix VARCHAR(32) NOT NULL UNIQUE,
    verifier VARCHAR(128) NOT NULL, pepper_version VARCHAR(20) NOT NULL, scopes TEXT NOT NULL,
    allowed_routes TEXT NOT NULL, status VARCHAR(20) NOT NULL, valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE, rotated_from UUID, last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE rate_limit_policies (
    id VARCHAR(120) PRIMARY KEY, subject_type VARCHAR(30) NOT NULL, route_id VARCHAR(120),
    method VARCHAR(10), capacity INTEGER NOT NULL, refill_tokens INTEGER NOT NULL,
    refill_period_seconds INTEGER NOT NULL, priority INTEGER NOT NULL, redis_outage_mode VARCHAR(20) NOT NULL,
    response_headers_enabled BOOLEAN NOT NULL, enabled BOOLEAN NOT NULL, version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE ip_rules (
    id VARCHAR(120) PRIMARY KEY, network VARCHAR(100) NOT NULL, action VARCHAR(20) NOT NULL,
    route_id VARCHAR(120), priority INTEGER NOT NULL, reason VARCHAR(500) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL, expires_at TIMESTAMP WITH TIME ZONE,
    enabled BOOLEAN NOT NULL, version BIGINT NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE risk_rules (
    id VARCHAR(120) PRIMARY KEY, signal VARCHAR(80) NOT NULL, threshold_value INTEGER NOT NULL,
    weight INTEGER NOT NULL, action VARCHAR(20) NOT NULL, route_id VARCHAR(120),
    enabled BOOLEAN NOT NULL, version BIGINT NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE audit_events (
    id UUID PRIMARY KEY, event_time TIMESTAMP WITH TIME ZONE NOT NULL, request_id VARCHAR(160) NOT NULL,
    trace_id VARCHAR(160), event_type VARCHAR(80) NOT NULL, decision VARCHAR(30) NOT NULL,
    reason_code VARCHAR(100) NOT NULL, route_id VARCHAR(120), method VARCHAR(10), path VARCHAR(1000),
    actor_type VARCHAR(30), subject_ref VARCHAR(200), source_ip VARCHAR(100), status INTEGER NOT NULL,
    latency_ms BIGINT NOT NULL, instance_id VARCHAR(160) NOT NULL, environment VARCHAR(80) NOT NULL
);
CREATE TABLE admin_action_logs (
    id UUID PRIMARY KEY, event_time TIMESTAMP WITH TIME ZONE NOT NULL, actor VARCHAR(200) NOT NULL,
    action VARCHAR(100) NOT NULL, target_type VARCHAR(80) NOT NULL, target_id VARCHAR(160) NOT NULL,
    result VARCHAR(30) NOT NULL, change_summary TEXT NOT NULL, request_id VARCHAR(160)
);
