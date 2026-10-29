GRANT USAGE ON SCHEMA public TO lds_app;

-- Vrijedi za sve tablice i sekvence koje lds_admin napravi od sada nadalje
ALTER DEFAULT PRIVILEGES FOR ROLE lds_admin IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO lds_app;

ALTER DEFAULT PRIVILEGES FOR ROLE lds_admin IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO lds_app;