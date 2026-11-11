#!/usr/bin/env bash
set -Eeuo pipefail

: "${APP_DB_USER:?APP_DB_USER is required}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"

psql \
  --no-psqlrc \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set ON_ERROR_STOP=1 \
  --set app_user="$APP_DB_USER" \
  --set app_password="$APP_DB_PASSWORD" <<'SQL'
-- Allow manual recovery when database initialization was interrupted.
-- Existing roles and their passwords remain unchanged.
SELECT format(
  'CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS',  :'app_user', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec
SQL
