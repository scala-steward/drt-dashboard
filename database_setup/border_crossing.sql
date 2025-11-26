CREATE TABLE public.border_crossing (
    port        VARCHAR NOT NULL,
    terminal    VARCHAR NOT NULL,
    date_utc    VARCHAR NOT NULL,
    gate_type   VARCHAR NOT NULL,
    hour        SMALLINT NOT NULL,
    passengers  INTEGER NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (port, terminal, date_utc, gate_type, hour)
);

CREATE INDEX idx_border_crossing_date ON public.border_crossing (date_utc);

CREATE INDEX idx_border_crossing_port_date ON public.border_crossing (port, date_utc);

CREATE INDEX idx_border_crossing_port_terminal_date ON public.border_crossing (port, terminal, date_utc);

CREATE INDEX idx_border_crossing_port_terminal_date_hour ON public.border_crossing (port, terminal, date_utc, hour);
