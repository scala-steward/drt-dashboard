CREATE TABLE public.user_access_requests(
  email text NOT NULL,
  all_ports boolean,
  ports text,
  regions text,
  staff_editing boolean NOT NULL,
  line_manager text,
  agree_declaration boolean NOT NULL,
  account_type text NOT NULL,
  port_or_region_text text,
  staff_text text,
  status text,
  request_time timestamp NOT NULL,
  PRIMARY KEY (email, request_time)
);

CREATE INDEX email ON public.user_access_requests (email);
CREATE INDEX status ON public.user_access_requests (status);
CREATE INDEX request_time ON public.user_access_requests (request_time ASC);
