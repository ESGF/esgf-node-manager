--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- Name: esgf_security; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA esgf_security;


SET search_path = esgf_security, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: group; Type: TABLE; Schema: esgf_security; Owner: -; Tablespace: 
--

CREATE TABLE "group" (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text NOT NULL,
    visible boolean,
    automatic_approval boolean
);


--
-- Name: permission; Type: TABLE; Schema: esgf_security; Owner: -; Tablespace: 
--

CREATE TABLE permission (
    user_id integer NOT NULL,
    group_id integer NOT NULL,
    role_id integer NOT NULL
);


--
-- Name: role; Type: TABLE; Schema: esgf_security; Owner: -; Tablespace: 
--

CREATE TABLE role (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text NOT NULL
);


--
-- Name: user; Type: TABLE; Schema: esgf_security; Owner: -; Tablespace: 
--

CREATE TABLE "user" (
    id integer NOT NULL,
    firstname character varying(100) NOT NULL,
    middlename character varying(100),
    lastname character varying(100) NOT NULL,
    email character varying(100) NOT NULL,
    username character varying(100) NOT NULL,
    password character varying(100),
    dn character varying(300),
    openid character varying(200) NOT NULL,
    organization character varying(200),
    organization_type character varying(200),
    city character varying(100),
    state character varying(100),
    country character varying(100)
);


--
-- Name: group_id_seq; Type: SEQUENCE; Schema: esgf_security; Owner: -
--

CREATE SEQUENCE group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Name: group_id_seq; Type: SEQUENCE OWNED BY; Schema: esgf_security; Owner: -
--

ALTER SEQUENCE group_id_seq OWNED BY "group".id;


--
-- Name: role_id_seq; Type: SEQUENCE; Schema: esgf_security; Owner: -
--

CREATE SEQUENCE role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Name: role_id_seq; Type: SEQUENCE OWNED BY; Schema: esgf_security; Owner: -
--

ALTER SEQUENCE role_id_seq OWNED BY role.id;


--
-- Name: user_id_seq; Type: SEQUENCE; Schema: esgf_security; Owner: -
--

CREATE SEQUENCE user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Name: user_id_seq; Type: SEQUENCE OWNED BY; Schema: esgf_security; Owner: -
--

ALTER SEQUENCE user_id_seq OWNED BY "user".id;


--
-- Name: id; Type: DEFAULT; Schema: esgf_security; Owner: -
--

ALTER TABLE "group" ALTER COLUMN id SET DEFAULT nextval('group_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: esgf_security; Owner: -
--

ALTER TABLE role ALTER COLUMN id SET DEFAULT nextval('role_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: esgf_security; Owner: -
--

ALTER TABLE "user" ALTER COLUMN id SET DEFAULT nextval('user_id_seq'::regclass);


--
-- Name: group_name_key; Type: CONSTRAINT; Schema: esgf_security; Owner: -; Tablespace: 
--

ALTER TABLE ONLY "group"
    ADD CONSTRAINT group_name_key UNIQUE (name);


--
-- Name: group_pkey; Type: CONSTRAINT; Schema: esgf_security; Owner: -; Tablespace: 
--

ALTER TABLE ONLY "group"
    ADD CONSTRAINT group_pkey PRIMARY KEY (id);


--
-- Name: permission_pkey; Type: CONSTRAINT; Schema: esgf_security; Owner: -; Tablespace: 
--

ALTER TABLE ONLY permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (user_id, group_id, role_id);


--
-- Name: role_name_key; Type: CONSTRAINT; Schema: esgf_security; Owner: -; Tablespace: 
--

ALTER TABLE ONLY role
    ADD CONSTRAINT role_name_key UNIQUE (name);


--
-- Name: role_pkey; Type: CONSTRAINT; Schema: esgf_security; Owner: -; Tablespace: 
--

ALTER TABLE ONLY role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


--
-- Name: user_pkey; Type: CONSTRAINT; Schema: esgf_security; Owner: -; Tablespace: 
--

ALTER TABLE ONLY "user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


--
-- Name: user_username_key; Type: CONSTRAINT; Schema: esgf_security; Owner: -; Tablespace: 
--

ALTER TABLE ONLY "user"
    ADD CONSTRAINT user_username_key UNIQUE (username);


--
-- Name: ix_esgf_security_user_openid; Type: INDEX; Schema: esgf_security; Owner: -; Tablespace: 
--

CREATE INDEX ix_esgf_security_user_openid ON "user" USING btree (openid);


--
-- Name: permission_group_id_fkey; Type: FK CONSTRAINT; Schema: esgf_security; Owner: -
--

ALTER TABLE ONLY permission
    ADD CONSTRAINT permission_group_id_fkey FOREIGN KEY (group_id) REFERENCES "group"(id);


--
-- Name: permission_role_id_fkey; Type: FK CONSTRAINT; Schema: esgf_security; Owner: -
--

ALTER TABLE ONLY permission
    ADD CONSTRAINT permission_role_id_fkey FOREIGN KEY (role_id) REFERENCES role(id);


--
-- Name: permission_user_id_fkey; Type: FK CONSTRAINT; Schema: esgf_security; Owner: -
--

ALTER TABLE ONLY permission
    ADD CONSTRAINT permission_user_id_fkey FOREIGN KEY (user_id) REFERENCES "user"(id);


--
-- PostgreSQL database dump complete
--


--
-- Initialize roles
--

INSERT INTO role (name,description) VALUES ('none', 'None');
INSERT INTO role (name,description) VALUES ('default', 'Standard');
INSERT INTO role (name,description) VALUES ('publisher', 'Data Publisher');
INSERT INTO role (name,description) VALUES ('admin', 'Group Administrator');
INSERT INTO role (name,description) VALUES ('super', 'Super User');

--
-- PostgreSQL database dump complete
-- Reset search path to public, so that esgf_migrate_version can be updated.
--

SET search_path = public, pg_catalog;
