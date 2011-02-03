--
-- PostgreSQL database for esg node
--

SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

CREATE SCHEMA esgf

CREATE TABLE esgf.access_logging (
    id int PRIMARY KEY,
    user_id character varying NOT NULL,
    email character varying,
    url character varying NOT NULL,
    file_id character varying,
    remote_addr character varying NOT NULL,
    user_agent character varying,
    service_type character varying,
    batch_update_time double precision,    
    date_fetched double precision NOT NULL,
    success boolean DEFAULT false,
    duration double precision DEFAULT 0
);

CREATE INDEX idx_access_logging_url ON esgf.access_logging USING btree (url);
CREATE SEQUENCE esgf.seq_access_logging;
ALTER  SEQUENCE esgf.seq_access_logging OWNED BY esgf.access_logging.id;

CREATE TABLE esgf.notification_run_log (
    id character varying NOT NULL,
    notify_time double precision NOT NULL
);

CREATE TABLE esgf.monitor_run_log (
    id character varying NOT NULL,
    last_run_time double precision NOT NULL
);

CREATE TABLE esgf.metrics_run_log (
    id character varying NOT NULL,
    last_run_time double precision NOT NULL
);
