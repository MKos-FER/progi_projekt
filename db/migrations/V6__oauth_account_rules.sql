-- Pravila za vanjske račune i profil koja nedostaju u V4 (F-001, F-002, F-004, C-06).

-- Jedan lokalni korisnik ima jedan Google račun (C-06). Prijava je moguća samo Googleom.
-- Provjera iz V4 (provider nije prazan) zamjenjuje se strožom pod istim imenom.
ALTER TABLE oauth_accounts
    ADD CONSTRAINT oauth_accounts_user_provider_key UNIQUE (user_id, provider),
    DROP CONSTRAINT oauth_accounts_provider_check,
    ADD CONSTRAINT oauth_accounts_provider_check CHECK (provider IN ('google'));

-- Pretraživanje po user_id pokriva novi UNIQUE (user_id, provider)
DROP INDEX oauth_accounts_user_id_idx;

-- Zaštita od očito pogrešnog unosa; da datum nije u budućnosti provjerava backend
ALTER TABLE users
    ADD CONSTRAINT users_date_of_birth_check CHECK (date_of_birth >= DATE '1900-01-01');

COMMENT ON COLUMN oauth_accounts.provider_user_id IS
    'Googleov sub (OpenID Connect); jedini podatak po kojem se prepoznaje korisnik pri prijavi.';
COMMENT ON COLUMN oauth_accounts.email IS
    'Samo informativno; ne koristi se za prijavu ni za spajanje računa.';
COMMENT ON COLUMN users.gender IS
    'Male, Female ili Other; sučelje prikazuje prijevod.';
