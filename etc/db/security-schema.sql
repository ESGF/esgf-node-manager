--
-- PostgreSQL database for esg node (security)
--

SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

CREATE SCHEMA esgf;
SET search_path TO public,esgf;

CREATE TABLE esgf.user (
    id int PRIMARY KEY,
    openid character varying NOT NULL,
    firstname character varying NOT NULL,
    middlename character varying,
    lastname character varying,
    username character varying,
    email character varying NOT NULL,
    password character varying,
    organization character varying,
    organization_type character varying,
    city character varying,
    state character varying,
    country character varying
);
CREATE INDEX idx_user_openid ON esgf.user USING btree (openid);
CREATE SEQUENCE esgf.seq_user;
ALTER  SEQUENCE esgf.seq_user OWNED BY esgf.user.id;

CREATE TABLE esgf.permission (
    user_id int NOT NULL,
    group_id int NOT NULL,
    role_id int NOT NULL
);

CREATE TABLE esgf.group (
    id int PRIMARY KEY,
    name character varying NOT NULL
);
CREATE SEQUENCE esgf.seq_group;
ALTER  SEQUENCE esgf.seq_group OWNED BY esgf.group.id;

CREATE TABLE esgf.role (
    id int PRIMARY KEY,
    name character varying NOT NULL
);
CREATE SEQUENCE esgf.seq_role;
ALTER  SEQUENCE esgf.seq_role OWNED BY esgf.role.id;


