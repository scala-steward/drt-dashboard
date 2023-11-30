CREATE TABLE public.export(
  email text NOT NULL,
  terminals text NOT NULL,
  start_date varchar NOT NULL,
  end_date varchar NOT NULL,
  status text NOT NULL,
  created_at timestamp NOT NULL,
  PRIMARY KEY (email, created_at)
);
