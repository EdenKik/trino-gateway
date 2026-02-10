CREATE TABLE routing_rules (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(128) UNIQUE NOT NULL,
    description VARCHAR(256),
    priority NUMBER NOT NULL DEFAULT 0,
    condition VARCHAR(512) NOT NULL,
    actions CLOB NOT NULL,
    engine VARCHAR(50) NOT NULL DEFAULT 'MVEL',
    CHECK (priority >= 0)
);
