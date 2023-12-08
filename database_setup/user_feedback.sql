CREATE TABLE user_feedback
(
    email  text NOT NULL,
    created_at timestamp NOT NULL,
    feedback_type text,
    bf_role     text,
    drt_quality text,
    drt_likes   text,
    drt_improvements text,
    participation_interest text,
    ab_version text,
    PRIMARY KEY (email, created_at)
);
