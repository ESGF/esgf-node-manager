--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = esgf_node_manager, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

-- Update of the access_logging table to include size of datafile and number of its bytes transferred in a download 

alter table esgf_node_manager.access_logging drop data_size;
alter table esgf_node_manager.access_logging drop xfer_size;

SET search_path = public, pg_catalog;

