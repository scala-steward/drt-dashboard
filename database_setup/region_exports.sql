CREATE TABLE public.region_export(
  email text NOT NULL,
  region text NOT NULL,
  start_date varchar NOT NULL,
  end_date varchar NOT NULL,
  status text NOT NULL,
  created_at timestamp NOT NULL,
  PRIMARY KEY (email, region, created_at)
);

