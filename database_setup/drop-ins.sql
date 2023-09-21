CREATE TABLE drop_in
(
    id                 SERIAL PRIMARY KEY,
    last_updated_at   timestamp NOT NULL,
    title              VARCHAR(255),
    start_time         timestamp NOT NULL,
    end_time           timestamp NOT NULL,
    meeting_link       VARCHAR(500),
    is_published       BOOLEAN   NOT NULL DEFAULT FALSE
);

CREATE TABLE drop_in_registration
(
    email         text      NOT NULL,
    drop_in_id    integer   NOT NULL,
    registered_at timestamp NOT NULL,
    email_sent_at timestamp,
    PRIMARY KEY (email, drop_in_id)
);
