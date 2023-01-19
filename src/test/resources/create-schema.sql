CREATE TABLE IF NOT EXISTS user_test_init
(
    id text NOT NULL,
    username text NOT NULL,
    email text NOT NULL,
    latest_login timestamp NOT NULL,
    inactive_email_sent timestamp,
    revoked_access timestamp,
    PRIMARY KEY (id)
);