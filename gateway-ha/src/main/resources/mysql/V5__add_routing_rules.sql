CREATE TABLE IF NOT EXISTS routing_rules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) UNIQUE NOT NULL,
    description VARCHAR(256),
    priority INT NOT NULL DEFAULT 0,
    condition VARCHAR(512) NOT NULL,
    actions JSON NOT NULL,
    engine VARCHAR(50) NOT NULL DEFAULT 'MVEL',
    CHECK (priority >= 0)
);
