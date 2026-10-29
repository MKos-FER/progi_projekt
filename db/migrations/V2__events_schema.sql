-- Tablice potrebne za događaje (F-006 – F-011).
-- users i venues su minimalni: imaju samo stupce koje događaji trebaju, ostalo dodaju kasnije migracije.

-- ---------- Lookup tablice ----------

CREATE TABLE sif_city (
    id   smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL UNIQUE
);

INSERT INTO sif_city (name) VALUES ('Zagreb'), ('Split'), ('Rijeka'), ('Osijek');

-- rank određuje poredak, da se može provjeriti zadovoljava li korisnik minimalnu razinu događaja
CREATE TABLE sif_skill_level (
    id   smallint PRIMARY KEY,
    name text     NOT NULL UNIQUE,
    rank smallint NOT NULL UNIQUE
);

INSERT INTO sif_skill_level (id, name, rank) VALUES
    (1, 'Amateur', 1),
    (2, 'Average', 2),
    (3, 'Pro',     3);

CREATE TABLE sports (
    id          smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        text NOT NULL UNIQUE,
    description text
);

INSERT INTO sports (name) VALUES ('Nogomet'), ('Košarka'), ('Odbojka');

-- ---------- Korisnici i tereni (minimalno) ----------

-- date_of_birth i city_id su nullable jer korisnik profil popunjava nakon prve prijave (F-004)
CREATE TABLE users (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      text        NOT NULL UNIQUE,
    name          text        NOT NULL,
    date_of_birth date,
    city_id       smallint    REFERENCES sif_city (id),
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE venues (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_by_id bigint        REFERENCES users (id) ON DELETE SET NULL,
    name          text          NOT NULL,
    city_id       smallint      NOT NULL REFERENCES sif_city (id),
    address       text,
    latitude      numeric(9, 6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude     numeric(9, 6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    created_at    timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX ON venues (city_id);

-- ---------- Događaji ----------

-- Spremamo samo otkazivanje; "pun", "u tijeku" i "završio" računa view events_with_status.
-- NULL u min_age, max_age i min_skill_level_id znači da uvjeta nema.
CREATE TABLE events (
    id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sport_id              smallint    NOT NULL REFERENCES sports (id),
    venue_id              bigint      NOT NULL REFERENCES venues (id),
    created_by_id         bigint      NOT NULL REFERENCES users (id),
    name                  text        NOT NULL CHECK (btrim(name) <> ''),
    description           text,
    scheduled_at          timestamptz NOT NULL,
    expected_duration_min integer     CHECK (expected_duration_min > 0),
    max_participants      integer     NOT NULL CHECK (max_participants >= 1),
    min_age               smallint    CHECK (min_age >= 0),
    max_age               smallint    CHECK (max_age >= 0),
    min_skill_level_id    smallint    REFERENCES sif_skill_level (id),
    cancelled_at          timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    CHECK (min_age IS NULL OR max_age IS NULL OR min_age <= max_age)
);

CREATE INDEX ON events (sport_id);
CREATE INDEX ON events (venue_id);
CREATE INDEX ON events (created_by_id);
CREATE INDEX ON events (scheduled_at);

-- Organizator je također red u ovoj tablici (F-007) i ulazi u max_participants.
CREATE TABLE event_members (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id             bigint      NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    user_id              bigint      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at            timestamptz NOT NULL DEFAULT now(),
    last_read_message_at timestamptz,
    UNIQUE (event_id, user_id)
);

CREATE INDEX ON event_members (user_id);

-- ---------- Status događaja ----------

CREATE VIEW events_with_status AS
SELECT e.*,
       count(m.id) AS participant_count,
       CASE
           WHEN e.cancelled_at IS NOT NULL THEN 'cancelled'
           WHEN e.scheduled_at
                + make_interval(mins => coalesce(e.expected_duration_min, 0)) < now() THEN 'finished'
           WHEN e.scheduled_at <= now() THEN 'in_progress'
           WHEN count(m.id) >= e.max_participants THEN 'full'
           ELSE 'open'
       END AS status
FROM events e
LEFT JOIN event_members m ON m.event_id = e.id
GROUP BY e.id;
