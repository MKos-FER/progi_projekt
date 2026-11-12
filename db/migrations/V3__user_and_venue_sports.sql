-- Sportovi korisnika s razinom (F-004, F-005) i sportovi terena (F-015 – F-017), koji su nedostajali u V2.
-- Teren dobiva opis te podatke o naplati i rezervaciji (F-016, F-022).
-- Mora proći i na bazama koje već imaju podatke iz V2, zato novi NOT NULL stupci imaju DEFAULT.

CREATE TABLE user_sports (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        bigint   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sport_id       smallint NOT NULL REFERENCES sports (id),
    skill_level_id smallint NOT NULL REFERENCES sif_skill_level (id),
    UNIQUE (user_id, sport_id)
);

-- Pretraživanje igrača po sportu i razini (F-005)
CREATE INDEX ON user_sports (sport_id, skill_level_id);

CREATE TABLE venue_sports (
    venue_id bigint   NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    sport_id smallint NOT NULL REFERENCES sports (id),
    PRIMARY KEY (venue_id, sport_id)
);

-- Pretraživanje terena po sportu
CREATE INDEX ON venue_sports (sport_id);

-- Teren na kojem već postoji događaj očito podržava sport tog događaja
INSERT INTO venue_sports (venue_id, sport_id)
SELECT DISTINCT venue_id, sport_id FROM events;

-- is_paid: naplaćuje li se korištenje terena (ERD ga zove is_free). Zauzetost terena nije u opsegu.
-- supports_booking: može li se teren rezervirati (F-016). Poveznica je opcionalna (F-022),
-- ali smije postojati samo za teren koji se može rezervirati.
ALTER TABLE venues
    ADD COLUMN description          text,
    ADD COLUMN is_paid              boolean NOT NULL DEFAULT false,
    ADD COLUMN supports_booking     boolean NOT NULL DEFAULT false,
    ADD COLUMN external_booking_url text,
    ADD CONSTRAINT venues_booking_url_requires_booking_check
        CHECK (external_booking_url IS NULL OR supports_booking);
