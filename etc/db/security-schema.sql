--
-- PostgreSQL database for esg node (security)
--

SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

CREATE TABLE user (
    id int PRIMARY KEY,
    openid character varying NOT NULL,
    firstname character varying NOT NULL,
    middlename character varying,
    lastname character varying,
    username character varying,
    email character varying NOT NULL,
    username character varying,
    password character varying,
    organization character varying,
    organization_type character varying,
    city character varying,
    state character varying,
    country character varying
);
CREATE INDEX idx_user_openid ON user USING btree (openid);
CREATE SEQUENCE seq_user;
ALTER  SEQUENCE seq_user OWNED BY user.id;

CREATE TABLE permission (
    user_id int NOT NULL,
    group_id int NOT NULL,
    role_id int NOT NULL
);

CREATE TABLE group (
    id int PRIMARY KEY,
    name character varying NOT NULL
);
CREATE SEQUENCE seq_group;
ALTER  SEQUENCE seq_group OWNED BY group.id;

CREATE TABLE role (
    id int PRIMARY KEY,
    name character varying NOT NULL
);
CREATE SEQUENCE seq_role;
ALTER  SEQUENCE seq_role OWNED BY role.id;


