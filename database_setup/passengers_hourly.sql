CREATE TABLE passengers_hourly
(
    date_utc   varchar(10) NOT NULL,
    hour       smallint    NOT NULL,
    port       varchar(3)  NOT NULL,
    terminal   varchar(3)  NOT NULL,
    passengers smallint    NOT NULL,
    created_at timestamp   NOT NULL,
    updated_at timestamp   NOT NULL,
    PRIMARY KEY (date_utc, hour, port, terminal)
);

CREATE INDEX date_hour_port ON public.passengers_hourly (date_utc, hour, port);
CREATE INDEX date_port ON public.passengers_hourly (date_utc, port);
CREATE INDEX date_port_terminal ON public.passengers_hourly (date_utc, port, terminal);
