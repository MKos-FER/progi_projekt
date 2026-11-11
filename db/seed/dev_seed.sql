-- Ogledni podaci za lokalni razvoj. NIJE migracija i ne ide na produkciju.
-- Pokreće se ručno kao lds_app; prvo briše stare ogledne podatke pa se može pokretati više puta.
-- Koordinate terena su približne.

BEGIN;

DELETE FROM event_members;
DELETE FROM events;
DELETE FROM venue_sports;
DELETE FROM user_sports;
DELETE FROM venues;
DELETE FROM oauth_accounts;
DELETE FROM users;

INSERT INTO users (username, name, date_of_birth, city_id) VALUES
    ('ana',   'Ana Horvat',   '2002-03-14', (SELECT id FROM sif_city WHERE name = 'Zagreb')),
    ('ivan',  'Ivan Kovač',   '1999-11-02', (SELECT id FROM sif_city WHERE name = 'Zagreb')),
    ('petra', 'Petra Babić',  '2001-06-21', (SELECT id FROM sif_city WHERE name = 'Zagreb')),
    ('marko', 'Marko Jurić',  '2004-01-30', (SELECT id FROM sif_city WHERE name = 'Split'));

INSERT INTO user_sports (user_id, sport_id, skill_level_id)
SELECT u.id, s.id, l.id
FROM (VALUES ('ana',   'Nogomet', 'Average'),
             ('ana',   'Košarka', 'Amateur'),
             ('ivan',  'Košarka', 'Pro'),
             ('ivan',  'Nogomet', 'Average'),
             ('petra', 'Nogomet', 'Pro'),
             ('marko', 'Odbojka', 'Average')) AS v(username, sport, skill)
JOIN users           u ON u.username = v.username
JOIN sports          s ON s.name = v.sport
JOIN sif_skill_level l ON l.name = v.skill;

-- Mladost se rezervira preko linka, Bačvice se mogu rezervirati, ali link nije unesen.
-- Link za rezervaciju je izmišljen (example.com).
INSERT INTO venues (created_by_id, name, city_id, latitude, longitude, is_paid, supports_booking, external_booking_url) VALUES
    ((SELECT id FROM users WHERE username = 'ana'),   'Jarun',   (SELECT id FROM sif_city WHERE name = 'Zagreb'), 45.780500, 15.917000, false, false, NULL),
    ((SELECT id FROM users WHERE username = 'ivan'),  'Mladost', (SELECT id FROM sif_city WHERE name = 'Zagreb'), 45.784000, 15.966000, true,  true,  'https://example.com/mladost-rezervacija'),
    ((SELECT id FROM users WHERE username = 'marko'), 'Bačvice', (SELECT id FROM sif_city WHERE name = 'Split'),  43.502500, 16.448000, false, true,  NULL);

INSERT INTO venue_sports (venue_id, sport_id)
SELECT ve.id, s.id
FROM (VALUES ('Jarun',   'Nogomet'),
             ('Mladost', 'Košarka'),
             ('Mladost', 'Odbojka'),
             ('Bačvice', 'Odbojka')) AS v(venue, sport)
JOIN venues ve ON ve.name = v.venue
JOIN sports s  ON s.name = v.sport;

-- Stvaranje događaja: jedna naredba upiše događaj i organizatora kao prvog člana (F-007).
-- Ovako će to raditi i backend.

-- 1) otvoren: sutra, 3 od 10 mjesta, uvjeti dob 18–35 i barem Average
WITH e AS (
    INSERT INTO events (sport_id, venue_id, created_by_id, name, scheduled_at, expected_duration_min,
                        max_participants, min_age, max_age, min_skill_level_id)
    VALUES ((SELECT id FROM sports WHERE name = 'Nogomet'),
            (SELECT id FROM venues WHERE name = 'Jarun'),
            (SELECT id FROM users WHERE username = 'ana'),
            'Mali nogomet nakon faksa', date_trunc('day', now()) + interval '1 day 18 hours', 90,
            10, 18, 35, (SELECT id FROM sif_skill_level WHERE name = 'Average'))
    RETURNING id, created_by_id
)
INSERT INTO event_members (event_id, user_id) SELECT id, created_by_id FROM e;

-- 2) pun: prekosutra, 2 od 2 mjesta
WITH e AS (
    INSERT INTO events (sport_id, venue_id, created_by_id, name, scheduled_at, expected_duration_min, max_participants)
    VALUES ((SELECT id FROM sports WHERE name = 'Košarka'),
            (SELECT id FROM venues WHERE name = 'Mladost'),
            (SELECT id FROM users WHERE username = 'ivan'),
            'Basket 1 na 1', date_trunc('day', now()) + interval '2 days 17 hours', 60, 2)
    RETURNING id, created_by_id
)
INSERT INTO event_members (event_id, user_id) SELECT id, created_by_id FROM e;

-- 3) otkazan
WITH e AS (
    INSERT INTO events (sport_id, venue_id, created_by_id, name, scheduled_at, expected_duration_min, max_participants, cancelled_at)
    VALUES ((SELECT id FROM sports WHERE name = 'Odbojka'),
            (SELECT id FROM venues WHERE name = 'Bačvice'),
            (SELECT id FROM users WHERE username = 'marko'),
            'Odbojka na pijesku', date_trunc('day', now()) + interval '3 days 10 hours', 120, 8, now())
    RETURNING id, created_by_id
)
INSERT INTO event_members (event_id, user_id) SELECT id, created_by_id FROM e;

-- 4) završio: jučer
WITH e AS (
    INSERT INTO events (sport_id, venue_id, created_by_id, name, scheduled_at, expected_duration_min, max_participants)
    VALUES ((SELECT id FROM sports WHERE name = 'Nogomet'),
            (SELECT id FROM venues WHERE name = 'Jarun'),
            (SELECT id FROM users WHERE username = 'petra'),
            'Jučerašnja utakmica', date_trunc('day', now()) - interval '1 day' + interval '19 hours', 90, 10)
    RETURNING id, created_by_id
)
INSERT INTO event_members (event_id, user_id) SELECT id, created_by_id FROM e;

-- Ostali sudionici
INSERT INTO event_members (event_id, user_id)
SELECT ev.id, u.id
FROM (VALUES ('Mali nogomet nakon faksa', 'ivan'),
             ('Mali nogomet nakon faksa', 'petra'),
             ('Basket 1 na 1',            'ana'),
             ('Jučerašnja utakmica',      'ana')) AS v(event_name, username)
JOIN events ev ON ev.name = v.event_name
JOIN users  u  ON u.username = v.username;

COMMIT;
