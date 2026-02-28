\set ON_ERROR_STOP on

-- Required psql variables:
--   app_user
--   app_password
--   app_db

-- 1) Role: create if missing, always reset login/password (idempotent)
SELECT
  format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
WHERE
  NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

SELECT
  format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'app_user', :'app_password')
\gexec

-- 2) Database: create if missing, enforce owner (idempotent)
SELECT
  format('CREATE DATABASE %I OWNER %I', :'app_db', :'app_user')
WHERE
  NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'app_db')
\gexec

SELECT
  format('ALTER DATABASE %I OWNER TO %I', :'app_db', :'app_user')
\gexec

SELECT
  format('GRANT CONNECT ON DATABASE %I TO %I', :'app_db', :'app_user')
\gexec

\connect :app_db

-- 3) Schema privileges (idempotent)
SELECT
  format('GRANT USAGE, CREATE ON SCHEMA public TO %I', :'app_user')
\gexec

-- 4) Table bootstrap (idempotent)
CREATE TABLE IF NOT EXISTS notes (
  id         CHAR(4) PRIMARY KEY,
  content    TEXT NOT NULL DEFAULT '',
  version    BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

SELECT
  format('ALTER TABLE notes OWNER TO %I', :'app_user')
\gexec

SELECT
  format('GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE notes TO %I', :'app_user')
\gexec
