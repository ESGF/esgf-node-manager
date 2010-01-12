--
-- PostgreSQL database for esg node
--

SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;


CREATE TABLE  access_logging (
    id uuid NOT NULL,
    userid character varying NOT NULL,
    email character varying NOT NULL,
    url character varying NOT NULL,
    remote_addr character varying NOT NULL,
    file_id character varying,
    date_fetched timestamp with time zone DEFAULT now(),
    success boolean DEFAULT true
);

CREATE TABLE notification_run_log (
    id uuid NOT NULL,
    notify_time timestamp with time zone DEFAULT now(),
    success boolean DEFAULT true
);
