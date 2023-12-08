CREATE TABLE ab_feature
(
    email         text NOT NULL,
    function_name text NOT NULL,
    presented_at  timestamp NOT NULL,
    ab_version     text,
    PRIMARY KEY (email, function_name)
);
