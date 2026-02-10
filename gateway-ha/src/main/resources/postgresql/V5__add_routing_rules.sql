CREATE TABLE IF NOT EXISTS routing_rules (
    id SERIAL PRIMARY KEY,
    name VARCHAR(128) UNIQUE NOT NULL,
    description VARCHAR(256),
    priority INT NOT NULL DEFAULT 0,
    condition VARCHAR(512) NOT NULL,
    actions VARCHAR[] NOT NULL,
    engine VARCHAR(50) NOT NULL DEFAULT 'MVEL',
    CHECK (priority >= 0)
);
