--
-- PostgreSQL database for esg node
--

SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

CREATE TABLE  access_logging (
    id int NOT NULL,
    user_id character varying NOT NULL,
    email character varying NOT NULL,
    url character varying NOT NULL,
    file_id character varying,
    remote_addr character varying NOT NULL,
    user_agent character varying,
    date_fetched double precision NOT NULL,
    success boolean DEFAULT false,
    duration double precision DEFAULT 0
);

CREATE INDEX idx_access_logging_url ON access_logging USING btree (url);
CREATE SEQUENCE seq_access_logging;
ALTER  SEQUENCE seq_access_logging OWNED BY access_logging.id;

CREATE TABLE notification_run_log (
    id character varying NOT NULL,
    notify_time double precision NOT NULL
);

CREATE TABLE monitor_run_log (
    id character varying NOT NULL,
    last_run_time double precision NOT NULL
);

CREATE TABLE metrics_run_log (
    id character varying NOT NULL,
    last_run_time double precision NOT NULL
);
