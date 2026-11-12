-- Korisnički profil i identiteti; prijava i provjera ovlasti dolaze u zasebnom zadatku.
CREATE TABLE sif_app_role (
    id text PRIMARY KEY
);

CREATE TABLE sif_permissions (
    id          text PRIMARY KEY,
    description text NOT NULL
);

CREATE TABLE app_role_has_permissions (
    app_role_id   text NOT NULL REFERENCES sif_app_role (id),
    permission_id text NOT NULL REFERENCES sif_permissions (id),
    PRIMARY KEY (app_role_id, permission_id)
);

INSERT INTO sif_app_role (id) VALUES ('USER'), ('ADMIN');
INSERT INTO sif_permissions (id, description) VALUES
    ('VENUE_EDIT_ANY', 'Ispravak podataka bilo kojeg terena'),
    ('VENUE_DELETE_ANY', 'Uklanjanje neispravnog terena'),
    ('VENUE_REPORT_MANAGE', 'Pregled i obrada prijava netočnih podataka');
INSERT INTO app_role_has_permissions (app_role_id, permission_id)
SELECT 'ADMIN', id FROM sif_permissions;

-- DEFAULT čuva postojeće korisnike i kompatibilnost s razvojnim seedom.
ALTER TABLE users
    ADD COLUMN profile_photo_url text,
    ADD COLUMN gender text,
    ADD COLUMN app_role_id text NOT NULL DEFAULT 'USER' REFERENCES sif_app_role (id),
    ADD CONSTRAINT users_username_not_blank CHECK (username ~ '[^[:space:]]'),
    ADD CONSTRAINT users_name_not_blank CHECK (name ~ '[^[:space:]]');
    ADD CONSTRAINT users_gender_options CHECK (gender IN ('Male', 'Female', 'Other')

CREATE INDEX ON users (city_id);

CREATE TABLE oauth_accounts (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          bigint NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         text NOT NULL CHECK (provider ~ '[^[:space:]]'),
    provider_user_id text NOT NULL CHECK (provider_user_id ~ '[^[:space:]]'),
    email            text,
    linked_at        timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);

CREATE INDEX ON oauth_accounts (user_id);
