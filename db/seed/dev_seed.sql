-- UPOZORENJE: briše postojeće aplikacijske i referentne podatke, a zatim umeće probne podatke u nastavku.
-- Pokrenuti nad odgovarajućom lokalnom/razvojnom bazom s ulogom koja ima ovlasti DELETE/INSERT i pristup sekvencama.

BEGIN;
SET LOCAL search_path = public;

-- Brisanje povezanih i aplikacijskih podataka redoslijedom koji poštuje strane ključeve.
DELETE FROM public.event_members;
DELETE FROM public.events;
DELETE FROM public.venue_sports;
DELETE FROM public.user_sports;
DELETE FROM public.oauth_accounts;
DELETE FROM public.venues;
DELETE FROM public.users;
DELETE FROM public.app_role_has_permissions;
DELETE FROM public.sif_permissions;
DELETE FROM public.sif_skill_level;
DELETE FROM public.sports;
DELETE FROM public.sif_city;
DELETE FROM public.sif_app_role;

-- Ponovno popunjavanje šifrarnika i referentnih podataka.

INSERT INTO public.sif_app_role (id) VALUES
  ('USER'),
  ('ADMIN'),
  ('GUEST'),
  ('MODERATOR');

INSERT INTO public.sif_permissions (id, description) VALUES
  ('VENUE_EDIT_ANY', 'Uređivanje bilo kojeg sportskog objekta'),
  ('VENUE_DELETE_ANY', 'Brisanje bilo kojeg sportskog objekta'),
  ('VENUE_REPORT_MANAGE', 'Pregled i upravljanje prijavama sportskih objekata');

INSERT INTO public.app_role_has_permissions (app_role_id, permission_id) VALUES
  ('ADMIN', 'VENUE_EDIT_ANY'),
  ('ADMIN', 'VENUE_DELETE_ANY'),
  ('ADMIN', 'VENUE_REPORT_MANAGE');

INSERT INTO public.sif_skill_level (id, name, rank) VALUES
  (1, 'Amater', 1),
  (2, 'Srednja razina', 2),
  (3, 'Profesionalac', 3);

INSERT INTO public.sif_city (name) VALUES
  ('Zagreb'),
  ('Split'),
  ('Rijeka'),
  ('Osijek'),
  ('Zadar'),
  ('Slavonski Brod');

INSERT INTO public.sports (name, description) VALUES
  ('Nogomet', 'Probni sport: nogomet'),
  ('Košarka', 'Probni sport: košarka'),
  ('Tenis', 'Probni sport: tenis'),
  ('Trčanje', 'Probni sport: trčanje'),
  ('Odbojka', 'Probni sport: odbojka'),
  ('Badminton', 'Probni sport: badminton'),
  ('Biciklizam', 'Probni sport: biciklizam'),
  ('Stolni tenis', 'Probni sport: stolni tenis');

-- Privremene pomoćne tablice omogućuju povezivanje pomoću ID-jeva koje generira baza.
CREATE TEMP TABLE seed_user_map (source_id integer PRIMARY KEY, actual_id bigint NOT NULL);
CREATE TEMP TABLE seed_event_map (source_id integer PRIMARY KEY, actual_id bigint NOT NULL);
CREATE TEMP TABLE seed_venue_map (venue_key text PRIMARY KEY, actual_id bigint NOT NULL);
CREATE TEMP TABLE seed_users (
  source_id integer PRIMARY KEY, username text UNIQUE NOT NULL, name text NOT NULL,
  dob date NOT NULL, city_name text NOT NULL, gender text NOT NULL, role_id text NOT NULL,
  created_at timestamptz NOT NULL, profile_photo_url text
);
CREATE TEMP TABLE seed_user_sports (source_user_id integer, sport_name text, skill_level_id smallint);
CREATE TEMP TABLE seed_venues (
  venue_key text PRIMARY KEY, name text NOT NULL, city_name text NOT NULL,
  latitude numeric(9,6) NOT NULL, longitude numeric(9,6) NOT NULL,
  created_by_source_id integer
);
CREATE TEMP TABLE seed_events (
  source_id integer PRIMARY KEY, sport_name text NOT NULL, venue_key text NOT NULL,
  organizer_source_id integer NOT NULL, name text NOT NULL, description text,
  scheduled_at timestamptz NOT NULL, cancelled_at timestamptz,
  expected_duration_min integer, max_participants integer NOT NULL,
  min_age smallint, max_age smallint, min_skill_level_id smallint,
  created_at timestamptz NOT NULL
);
CREATE TEMP TABLE seed_event_participants (source_event_id integer, source_user_id integer);

-- Korisnici (32 probna korisnika)

INSERT INTO seed_users (source_id, username, name, dob, city_name, gender, role_id, created_at, profile_photo_url) VALUES
  (1, 'mateo', 'Mateo Horvat', '1991-02-02', 'Slavonski Brod', 'Male', 'USER', '2026-10-02 10:00:00', NULL),
  (2, 'ivan', 'Ivan Kovač', '1992-03-03', 'Slavonski Brod', 'Male', 'USER', '2026-10-03 10:00:00', NULL),
  (3, 'ana', 'Ana Babić', '1993-04-04', 'Slavonski Brod', 'Female', 'USER', '2026-10-04 10:00:00', NULL),
  (4, 'luka', 'Luka Marić', '1994-05-05', 'Slavonski Brod', 'Male', 'USER', '2026-10-05 10:00:00', NULL),
  (5, 'petra', 'Petra Novak', '1995-06-06', 'Zagreb', 'Female', 'USER', '2026-10-06 10:00:00', NULL),
  (6, 'marko', 'Marko Jurić', '1996-07-07', 'Zagreb', 'Male', 'USER', '2026-10-07 10:00:00', NULL),
  (7, 'iva', 'Iva Knežević', '1997-08-08', 'Zagreb', 'Female', 'USER', '2026-10-08 10:00:00', NULL),
  (8, 'filip', 'Filip Božić', '1998-09-09', 'Zagreb', 'Male', 'USER', '2026-10-09 10:00:00', NULL),
  (9, 'nikola', 'Nikola Radić', '1999-10-10', 'Zagreb', 'Male', 'USER', '2026-10-10 10:00:00', NULL),
  (10, 'lucija', 'Lucija Perić', '1990-11-11', 'Zagreb', 'Female', 'USER', '2026-10-11 10:00:00', NULL),
  (11, 'dominik', 'Dominik Šimić', '1991-12-12', 'Zagreb', 'Male', 'USER', '2026-10-12 10:00:00', NULL),
  (12, 'marija', 'Marija Kralj', '1992-01-13', 'Zagreb', 'Female', 'USER', '2026-10-13 10:00:00', NULL),
  (13, 'ante', 'Ante Vuković', '1993-02-14', 'Split', 'Male', 'USER', '2026-10-14 10:00:00', NULL),
  (14, 'lea', 'Lea Matić', '1994-03-15', 'Split', 'Female', 'USER', '2026-10-15 10:00:00', NULL),
  (15, 'toni', 'Toni Grgić', '1995-04-16', 'Split', 'Male', 'USER', '2026-10-16 10:00:00', NULL),
  (16, 'josip', 'Josip Pavlović', '1996-05-17', 'Rijeka', 'Male', 'USER', '2026-10-17 10:00:00', NULL),
  (17, 'ema', 'Ema Blažević', '1997-06-18', 'Rijeka', 'Female', 'USER', '2026-10-18 10:00:00', NULL),
  (18, 'david', 'David Šarić', '1998-07-19', 'Rijeka', 'Male', 'USER', '2026-10-19 10:00:00', NULL),
  (19, 'nina', 'Nina Lončar', '1999-08-20', 'Rijeka', 'Female', 'USER', '2026-10-20 10:00:00', NULL),
  (20, 'petar', 'Petar Barišić', '1990-09-21', 'Osijek', 'Male', 'USER', '2026-10-21 10:00:00', NULL),
  (21, 'sara', 'Sara Filipović', '1991-10-22', 'Osijek', 'Female', 'USER', '2026-10-22 10:00:00', NULL),
  (22, 'andrej', 'Andrej Vidović', '1992-11-23', 'Osijek', 'Male', 'USER', '2026-10-23 10:00:00', NULL),
  (23, 'marta', 'Marta Bošnjak', '1993-12-24', 'Zadar', 'Female', 'USER', '2026-10-24 10:00:00', NULL),
  (24, 'karlo', 'Karlo Tadić', '1994-01-25', 'Zadar', 'Male', 'USER', '2026-10-25 10:00:00', NULL),
  (25, 'tea', 'Tea Rukavina', '1995-02-26', 'Zagreb', 'Female', 'USER', '2026-10-26 10:00:00', NULL),
  (26, 'mihael', 'Mihael Špoljarić', '1996-03-27', 'Zagreb', 'Male', 'USER', '2026-10-27 10:00:00', NULL),
  (27, 'dorotea', 'Dorotea Cindrić', '1997-04-01', 'Zagreb', 'Female', 'USER', '2026-10-28 10:00:00', NULL),
  (28, 'stjepan', 'Stjepan Lovrić', '1998-05-02', 'Zagreb', 'Male', 'USER', '2026-10-01 10:00:00', NULL),
  (29, 'klara', 'Klara Zelić', '1999-06-03', 'Zagreb', 'Female', 'USER', '2026-10-02 10:00:00', NULL),
  (30, 'guest_ana', 'Ana (gost)', '1995-01-01', 'Zagreb', 'Other', 'GUEST', '2026-10-30 10:00:00', NULL),
  (31, 'guest_marko', 'Marko (gost)', '1995-01-01', 'Zagreb', 'Other', 'GUEST', '2026-10-30 10:00:00', NULL),
  (32, 'moderator_iva', 'Iva (moderatorica)', '1995-01-01', 'Zagreb', 'Other', 'MODERATOR', '2026-10-30 10:00:00', NULL);

INSERT INTO public.users (username, name, date_of_birth, city_id, created_at, profile_photo_url, gender, app_role_id)
SELECT su.username, su.name, su.dob, c.id, su.created_at, su.profile_photo_url, su.gender, su.role_id
FROM seed_users su JOIN public.sif_city c ON c.name = su.city_name;
INSERT INTO seed_user_map (source_id, actual_id)
SELECT su.source_id, u.id FROM seed_users su JOIN public.users u ON u.username = su.username;

-- Sportske preferencije korisnika

INSERT INTO seed_user_sports (source_user_id, sport_name, skill_level_id) VALUES
  (1, 'Nogomet', 1),
  (1, 'Košarka', 1),
  (1, 'Tenis', 2),
  (1, 'Odbojka', 1),
  (1, 'Biciklizam', 2),
  (2, 'Nogomet', 1),
  (2, 'Košarka', 2),
  (2, 'Trčanje', 1),
  (3, 'Nogomet', 2),
  (3, 'Košarka', 1),
  (3, 'Badminton', 1),
  (3, 'Stolni tenis', 1),
  (4, 'Nogomet', 1),
  (4, 'Košarka', 1),
  (4, 'Odbojka', 1),
  (5, 'Nogomet', 1),
  (5, 'Trčanje', 1),
  (5, 'Badminton', 1),
  (5, 'Biciklizam', 2),
  (6, 'Nogomet', 1),
  (6, 'Košarka', 2),
  (6, 'Odbojka', 1),
  (6, 'Badminton', 2),
  (6, 'Stolni tenis', 1),
  (7, 'Nogomet', 2),
  (7, 'Odbojka', 2),
  (7, 'Biciklizam', 1),
  (8, 'Nogomet', 1),
  (8, 'Košarka', 1),
  (8, 'Trčanje', 2),
  (8, 'Stolni tenis', 2),
  (9, 'Nogomet', 1),
  (9, 'Košarka', 1),
  (9, 'Badminton', 1),
  (9, 'Biciklizam', 2),
  (10, 'Nogomet', 1),
  (10, 'Trčanje', 1),
  (11, 'Košarka', 1),
  (11, 'Odbojka', 2),
  (12, 'Nogomet', 1),
  (13, 'Odbojka', 1),
  (14, 'Odbojka', 1),
  (15, 'Odbojka', 2),
  (16, 'Trčanje', 2),
  (17, 'Trčanje', 1),
  (18, 'Trčanje', 1),
  (19, 'Trčanje', 1),
  (20, 'Košarka', 1),
  (21, 'Košarka', 1),
  (22, 'Košarka', 2),
  (23, 'Tenis', 1),
  (24, 'Tenis', 1),
  (25, 'Nogomet', 1),
  (26, 'Nogomet', 1),
  (27, 'Nogomet', 2),
  (28, 'Nogomet', 1),
  (29, 'Nogomet', 1);

INSERT INTO public.user_sports (user_id, sport_id, skill_level_id)
SELECT um.actual_id, sp.id, sus.skill_level_id
FROM seed_user_sports sus
JOIN seed_user_map um ON um.source_id = sus.source_user_id
JOIN public.sports sp ON sp.name = sus.sport_name;

INSERT INTO public.oauth_accounts (user_id, provider, provider_user_id, email)
SELECT actual_id, 'google', 'dev-seed-' || su.username, su.username || '@example.test'
FROM seed_user_map um JOIN seed_users su ON su.source_id = um.source_id;

-- Sportski tereni / mjesta događanja

INSERT INTO seed_venues (venue_key, name, city_name, latitude, longitude, created_by_source_id) VALUES
  ('event-1', 'Sportski park Klasije', 'Slavonski Brod', 45.1603, 18.0156, 1),
  ('event-2', 'Sportska dvorana Vijuš', 'Slavonski Brod', 45.1589, 18.0121, 3),
  ('event-3', 'Teniski centar Slavonski Brod', 'Slavonski Brod', 45.1575, 18.018, 1),
  ('event-4', 'Šetnica uz Savu', 'Slavonski Brod', 45.1518, 18.0165, 5),
  ('Sportska dvorana Trešnjevka, Zagreb|Zagreb', 'Sportska dvorana Trešnjevka, Zagreb', 'Zagreb', 45.79636, 15.94207, 4),
  ('Sportski centar Zagi, Zagrebački velesajam|Zagreb', 'Sportski centar Zagi, Zagrebački velesajam', 'Zagreb', 45.777922, 15.969492, 6),
  ('Sportski centar Dubrava, Zagreb|Zagreb', 'Sportski centar Dubrava, Zagreb', 'Zagreb', 45.828229, 16.053652, 8),
  ('ŠRC Jarun, Zagreb|Zagreb', 'ŠRC Jarun, Zagreb', 'Zagreb', 45.78121, 15.92309, 7),
  ('Bundek, Zagreb|Zagreb', 'Bundek, Zagreb', 'Zagreb', 45.785385, 15.98603, 9),
  ('Sportski centar Sava, Sesvete|Zagreb', 'Sportski centar Sava, Sesvete', 'Zagreb', 45.835494, 16.118921, 10),
  ('Plaža Bačvice, Split|Split', 'Plaža Bačvice, Split', 'Split', 43.50247, 16.446767, 13),
  ('Korzo, Rijeka|Rijeka', 'Korzo, Rijeka', 'Rijeka', 45.324234, 14.443, 16),
  ('Športska dvorana Zrinjevac, Osijek|Osijek', 'Športska dvorana Zrinjevac, Osijek', 'Osijek', 45.55532, 18.68039, 20),
  ('Teniski tereni Sutomiška 1, Zadar|Zadar', 'Teniski tereni Sutomiška 1, Zadar', 'Zadar', 44.12239, 15.24539, 23),
  ('Sportski centar Šalata, Zagreb|Zagreb', 'Sportski centar Šalata, Zagreb', 'Zagreb', 45.83112, 15.97985, 25);

INSERT INTO public.venues (created_by_id, name, city_id, latitude, longitude, description, is_paid, supports_booking)
SELECT um.actual_id, sv.name, c.id, sv.latitude, sv.longitude,
       'Probni sportski objekt za lokalni razvoj.', FALSE, FALSE
FROM seed_venues sv
JOIN public.sif_city c ON c.name = sv.city_name
LEFT JOIN seed_user_map um ON um.source_id = sv.created_by_source_id;
INSERT INTO seed_venue_map (venue_key, actual_id)
SELECT sv.venue_key, v.id
FROM seed_venues sv
JOIN public.sif_city c ON c.name = sv.city_name
JOIN public.venues v ON v.name = sv.name AND v.city_id = c.id;

-- Događaji

INSERT INTO seed_events (source_id, sport_name, venue_key, organizer_source_id, name, description, scheduled_at, cancelled_at, expected_duration_min, max_participants, min_age, max_age, min_skill_level_id, created_at) VALUES
  (1, 'Nogomet', 'event-1', 1, 'Nedjeljni nogometni turnir', 'Rekreativni malonogometni turnir 5 na 5. Dobrodošli su igrači svih razina znanja.', CURRENT_TIMESTAMP + INTERVAL '2 days', NULL, 120, 20, 16, 45, 1, '2026-10-20 12:30:00'),
  (2, 'Košarka', 'event-2', 3, 'Večernja košarka', 'Opuštena košarkaška utakmica. Povedite prijatelje i uživajte u prijateljskom nadmetanju.', CURRENT_TIMESTAMP - INTERVAL '30 minutes', NULL, 90, 10, 16, 40, 1, '2026-10-22 09:15:00'),
  (3, 'Tenis', 'event-3', 1, 'Teniski meč za srednju razinu', 'Prijateljski pojedinačni teniski meč za igrače srednje razine. Ponesite vlastiti reket.', CURRENT_TIMESTAMP + INTERVAL '1 day', NULL, 90, 2, 18, 50, 2, '2026-10-23 15:00:00'),
  (4, 'Trčanje', 'event-4', 5, 'Subotnje jutarnje trčanje', 'Započnite vikend opuštenim grupnim trčanjem od 5 km kroz park Maksimir.', CURRENT_TIMESTAMP + INTERVAL '3 days', NULL, 60, 15, 15, 65, 1, '2026-10-24 11:20:00'),
  (5, 'Odbojka', 'Sportska dvorana Trešnjevka, Zagreb|Zagreb', 4, 'Okupljanje za dvoransku odbojku', 'Rekreativno okupljanje za odbojku. Profesionalno iskustvo nije potrebno.', CURRENT_TIMESTAMP + INTERVAL '5 days', NULL, 120, 12, 16, 45, 1, '2026-10-25 17:45:00'),
  (6, 'Badminton', 'Sportski centar Zagi, Zagrebački velesajam|Zagreb', 6, 'Badminton u parovima', 'Opušteni mečevi u parovima. Reketi su dostupni u objektu.', CURRENT_TIMESTAMP + INTERVAL '6 days', NULL, 90, 8, 14, 55, 1, '2026-10-26 13:10:00'),
  (7, 'Nogomet', 'Sportski centar Dubrava, Zagreb|Zagreb', 2, 'Mali nogomet 5 na 5', 'Večernji mali nogomet u dvorani. Tražimo još nekoliko igrača.', CURRENT_TIMESTAMP - INTERVAL '15 minutes', NULL, 90, 10, 18, 40, 2, '2026-10-27 08:00:00'),
  (8, 'Biciklizam', 'ŠRC Jarun, Zagreb|Zagreb', 7, 'Bicikliranje oko Jaruna', 'Opuštena grupna vožnja biciklom oko Jarunskog jezera. Prikladno za rekreativne bicikliste.', CURRENT_TIMESTAMP + INTERVAL '4 days', NULL, 120, 12, 16, 60, 1, '2026-10-27 16:30:00'),
  (9, 'Stolni tenis', 'Sportski centar Dubrava, Zagreb|Zagreb', 8, 'Okupljanje za stolni tenis', 'Prijateljske partije stolnog tenisa. Početnici su dobrodošli.', CURRENT_TIMESTAMP + INTERVAL '7 days', NULL, 90, 8, 12, 70, 1, '2026-10-28 10:00:00'),
  (10, 'Košarka', 'Bundek, Zagreb|Zagreb', 9, 'Košarka na otvorenom kod Bundeka', 'Neformalna košarkaška utakmica na vanjskim terenima. Sudjelovanje je prema redoslijedu dolaska.', CURRENT_TIMESTAMP + INTERVAL '8 days', NULL, 90, 10, 16, 40, 1, '2026-10-28 18:20:00'),
  (11, 'Nogomet', 'Sportski centar Sava, Sesvete|Zagreb', 10, 'Nedjeljni nogomet u Sesvetama', 'Tjedno rekreativno okupljanje za nogomet. Odlična prilika za upoznavanje novih igrača.', CURRENT_TIMESTAMP + INTERVAL '10 days', NULL, 120, 14, 18, 50, 1, '2026-10-29 09:45:00'),
  (12, 'Odbojka', 'Plaža Bačvice, Split|Split', 13, 'Okupljanje za odbojku na pijesku', 'Okupljanje za odbojku na pijesku na Bačvicama. Uživajte u prijateljskoj igri uz more.', CURRENT_TIMESTAMP - INTERVAL '3 days', NULL, 120, 8, 16, 45, 1, '2026-10-24 14:00:00'),
  (13, 'Trčanje', 'Korzo, Rijeka|Rijeka', 16, 'Trčanje uz riječku rivu', 'Grupno trčanje uz riječku obalu. Oko 6 km laganim tempom.', CURRENT_TIMESTAMP - INTERVAL '2 days', NULL, 75, 12, 15, 65, 1, '2026-10-25 08:30:00'),
  (14, 'Košarka', 'Športska dvorana Zrinjevac, Osijek|Osijek', 20, 'Košarkaško okupljanje u Osijeku', 'Opuštena košarkaška utakmica za lokalne igrače. Dobrodošli su igrači svih razina iskustva.', CURRENT_TIMESTAMP + INTERVAL '1 day', NULL, 90, 10, 16, 45, 1, '2026-10-26 12:00:00'),
  (15, 'Tenis', 'Teniski tereni Sutomiška 1, Zadar|Zadar', 23, 'Vikend-tenis u Zadru', 'Prijateljsko vikend-okupljanje za tenis. Dobrodošli su igrači svih razina.', CURRENT_TIMESTAMP + INTERVAL '9 days', NULL, 90, 4, 18, 55, 2, '2026-10-27 11:00:00'),
  (16, 'Nogomet', 'Sportski centar Šalata, Zagreb|Zagreb', 25, 'Vikend-okupljanje za nogomet u Zagrebu', 'Pridružite nam se na prijateljskom vikend-okupljanju za nogomet u Zagrebu! Događaj je otvoren igračima svih razina iskustva, bilo da ste potpuni početnik ili igrate već godinama. Ekipe ćemo organizirati ovisno o broju sudionika, odigrati nekoliko kraćih utakmica i pobrinuti se da svi dobiju dovoljno vremena na terenu. Ponesite sportsku odjeću, udobne nogometne tenisice i dovoljno vode. Cilj je dobro se zabaviti, upoznati nove ljude, unaprijediti vještine i uživati u prijateljskom nadmetanju. Slobodno pozovite i prijatelje!', CURRENT_TIMESTAMP + INTERVAL '12 days', CURRENT_TIMESTAMP, 120, 19, 16, 40, 1, '2026-10-28 14:00:00');

INSERT INTO public.venue_sports (venue_id, sport_id)
SELECT DISTINCT vm.actual_id, sp.id
FROM seed_events se JOIN seed_venue_map vm ON vm.venue_key = se.venue_key
JOIN public.sports sp ON sp.name = se.sport_name;

-- Događaji koriste ID-jeve objekata, korisnika i sportova koje generira baza; izvorni ID-jevi služe samo za povezivanje probnih podataka.
INSERT INTO public.events (sport_id, venue_id, created_by_id, name, description, scheduled_at, cancelled_at,
                           expected_duration_min, max_participants, min_age, max_age,
                           min_skill_level_id, created_at)
SELECT sp.id, vm.actual_id, organizer.actual_id, se.name, se.description, se.scheduled_at, se.cancelled_at,
       se.expected_duration_min, se.max_participants, se.min_age, se.max_age,
       se.min_skill_level_id, se.created_at
FROM seed_events se
JOIN public.sports sp ON sp.name = se.sport_name
JOIN seed_venue_map vm ON vm.venue_key = se.venue_key
JOIN seed_user_map organizer ON organizer.source_id = se.organizer_source_id;
INSERT INTO seed_event_map (source_id, actual_id)
SELECT se.source_id, ev.id
FROM seed_events se JOIN public.events ev ON ev.name = se.name;

-- Sudionici događaja

INSERT INTO seed_event_participants (source_event_id, source_user_id) VALUES
  (1, 1),
  (1, 2),
  (1, 4),
  (1, 5),
  (1, 7),
  (1, 8),
  (1, 9),
  (2, 2),
  (2, 3),
  (2, 4),
  (2, 6),
  (3, 1),
  (4, 1),
  (4, 5),
  (4, 8),
  (4, 10),
  (5, 1),
  (5, 4),
  (5, 6),
  (5, 7),
  (5, 11),
  (6, 2),
  (6, 5),
  (6, 6),
  (6, 9),
  (7, 1),
  (7, 2),
  (7, 3),
  (7, 4),
  (7, 6),
  (7, 8),
  (7, 10),
  (7, 12),
  (8, 1),
  (8, 5),
  (8, 7),
  (8, 9),
  (9, 2),
  (9, 6),
  (9, 8),
  (10, 1),
  (10, 2),
  (10, 4),
  (10, 8),
  (10, 9),
  (10, 11),
  (11, 4),
  (11, 6),
  (11, 7),
  (11, 9),
  (11, 10),
  (12, 12),
  (12, 13),
  (12, 14),
  (12, 15),
  (13, 15),
  (13, 16),
  (13, 17),
  (13, 18),
  (13, 19),
  (14, 19),
  (14, 20),
  (14, 21),
  (14, 22),
  (15, 22),
  (15, 23),
  (15, 24),
  (16, 24),
  (16, 25),
  (16, 26),
  (16, 27),
  (16, 28),
  (16, 29);

INSERT INTO public.event_members (event_id, user_id, joined_at)
SELECT DISTINCT em.actual_id, um.actual_id, se.created_at
FROM seed_event_participants sep
JOIN seed_event_map em ON em.source_id = sep.source_event_id
JOIN seed_user_map um ON um.source_id = sep.source_user_id
JOIN seed_events se ON se.source_id = sep.source_event_id;

-- Provjera ispravnosti. Očekuje se 32 korisnika i 16 događaja.
DO $$
DECLARE user_count integer; event_count integer; sb_user_count integer; sb_event_count integer;
BEGIN
  SELECT count(*) INTO user_count FROM public.users;
  SELECT count(*) INTO event_count FROM public.events;
  SELECT count(*) INTO sb_user_count FROM public.users u JOIN public.sif_city c ON c.id = u.city_id WHERE c.name = 'Slavonski Brod';
  SELECT count(*) INTO sb_event_count FROM public.events e JOIN public.venues v ON v.id = e.venue_id JOIN public.sif_city c ON c.id = v.city_id WHERE c.name = 'Slavonski Brod';
  IF user_count <> 32 THEN RAISE EXCEPTION 'Očekivano je 32 korisnika, pronađeno: %', user_count; END IF;
  IF event_count <> 16 THEN RAISE EXCEPTION 'Očekivano je 16 događaja, pronađeno: %', event_count; END IF;
  IF sb_user_count <> 4 THEN RAISE EXCEPTION 'Očekivana su 4 korisnika u Slavonskom Brodu, pronađeno: %', sb_user_count; END IF;
  IF sb_event_count <> 4 THEN RAISE EXCEPTION 'Očekivana su 4 događaja u Slavonskom Brodu, pronađeno: %', sb_event_count; END IF;
END $$;

COMMIT;

-- Ispisuje sve gradove (i broj korisnika u njima) te venues (i broj događaja koji se održavaju tamo)
SELECT c.name, count(*) AS korisnici FROM users u JOIN sif_city c ON c.id=u.city_id GROUP BY c.name ORDER BY c.name;
SELECT c.name, count(*) AS događaji FROM events e JOIN venues v ON v.id=e.venue_id JOIN sif_city c ON c.id=v.city_id GROUP BY c.name ORDER BY c.name;
