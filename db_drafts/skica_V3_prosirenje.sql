-- SKICA, nije migracija. Provjera da se tereni, prijave i komentari mogu dodati na V2 bez mijenjanja V2.

-- ---------- Tereni (F-015 – F-022) ----------
-- Postojeći redovi već imaju podatke, pa novi NOT NULL stupac treba DEFAULT
ALTER TABLE venues
    ADD COLUMN description          text,
    ADD COLUMN is_free              boolean NOT NULL DEFAULT true,
    ADD COLUMN external_booking_url text;

CREATE TABLE venue_sports (
    venue_id bigint   NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    sport_id smallint NOT NULL REFERENCES sports (id),
    PRIMARY KEY (venue_id, sport_id)
);

CREATE TABLE venue_photos (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id       bigint      NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    uploaded_by_id bigint      REFERENCES users (id) ON DELETE SET NULL,
    photo_url      text        NOT NULL,
    caption        text,
    uploaded_at    timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE venues ADD COLUMN cover_photo_id bigint REFERENCES venue_photos (id) ON DELETE SET NULL;

CREATE TABLE sif_report_status (
    id   smallint PRIMARY KEY,
    name text NOT NULL UNIQUE
);
INSERT INTO sif_report_status VALUES (1, 'pending'), (2, 'resolved'), (3, 'dismissed');

CREATE TABLE venue_reports (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id        bigint      NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    reported_by_id  bigint      REFERENCES users (id) ON DELETE SET NULL,
    description     text        NOT NULL CHECK (btrim(description) <> ''),
    status_id       smallint    NOT NULL DEFAULT 1 REFERENCES sif_report_status (id),
    resolution_note text,
    resolved_by_id  bigint      REFERENCES users (id) ON DELETE SET NULL,
    resolved_at     timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now()
);

-- ---------- Komentari (F-012 – F-014, F-024) ----------
CREATE TABLE messages (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id    bigint      NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    sender_id   bigint      REFERENCES users (id) ON DELETE SET NULL,
    reply_to_id bigint      REFERENCES messages (id),
    content     text        NOT NULL CHECK (btrim(content) <> ''),
    sent_at     timestamptz NOT NULL DEFAULT now(),
    edited_at   timestamptz,
    deleted_at  timestamptz
);

CREATE INDEX ON messages (event_id, sent_at);
