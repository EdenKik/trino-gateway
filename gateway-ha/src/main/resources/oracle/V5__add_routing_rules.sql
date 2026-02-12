CREATE TABLE routing_rules (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(128) UNIQUE NOT NULL,
    description VARCHAR(256),
    priority NUMBER DEFAULT 0 NOT NULL,
    condition VARCHAR(512) NOT NULL,
    actions CLOB NOT NULL,
    engine VARCHAR(50) DEFAULT 'MVEL' NOT NULL,
    CHECK (priority >= 0)
);
