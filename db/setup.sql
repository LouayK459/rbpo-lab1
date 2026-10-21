-- Выполнить от postgres в заранее созданной базе rbpo_lab3.
-- Пароль роли задать отдельно через команду psql: \password rbpo_lab3_app
BEGIN;

CREATE ROLE rbpo_lab3_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
    NOREPLICATION NOBYPASSRLS;
CREATE SCHEMA delivery AUTHORIZATION postgres;

REVOKE ALL ON DATABASE rbpo_lab3 FROM PUBLIC;
GRANT CONNECT ON DATABASE rbpo_lab3 TO rbpo_lab3_app;
REVOKE ALL ON SCHEMA delivery FROM PUBLIC;
GRANT USAGE ON SCHEMA delivery TO rbpo_lab3_app;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA delivery
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rbpo_lab3_app;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA delivery
    GRANT USAGE ON SEQUENCES TO rbpo_lab3_app;

COMMIT;
