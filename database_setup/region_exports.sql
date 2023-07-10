CREATE TABLE public.region_exports(
  email text NOT NULL,
  start_date date NOT NULL,
  end_date date NOT NULL,
  status text NOT NULL,
  created_at timestamp NOT NULL,
  PRIMARY KEY (email, created_at)
);

