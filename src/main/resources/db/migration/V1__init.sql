CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(255)
);
