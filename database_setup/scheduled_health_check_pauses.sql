CREATE TABLE public.scheduled_health_check_pause(
  starts_at timestamp NOT NULL,
  ends_at timestamp NOT NULL,
  ports text,
  created_at timestamp NOT NULL,
  PRIMARY KEY (starts_at, ends_at)
);
