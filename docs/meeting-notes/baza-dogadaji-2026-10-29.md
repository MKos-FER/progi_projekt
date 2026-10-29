---
date_created: 2026-10-29
---

# Baza: model događaja

---

## 1. Odluke

Dogovoreno s Mateom na Discordu i provedeno u PR-u `feature/db-events-schema` (migracije V1 i V2, `db/seed/dev_seed.sql`).

- **Status događaja:** spremamo samo `cancelled_at` (NULL = nije otkazan). Statuse `open`, `full`, `in_progress` i `finished` računa view `events_with_status` iz broja sudionika i termina, pa se ne mogu razići sa stvarnim stanjem.
- **Gradovi:** šifrarnik `sif_city`. Na njega pokazuju `users.city_id` i `venues.city_id`. Događaj grad dobiva preko terena. `latitude` i `longitude` ostaju za prikaz na karti.
- **Razine vještine:** šifrarnik `sif_skill_level` sa stupcem `rank`, da se može provjeriti minimalna razina na događaju. Vrijednosti su Amateur, Average i Pro (prema sprint-01).
- **Organizator** je red u `event_members` i ulazi u `max_participants`. Događaj i organizator upisuju se u istoj transakciji.
- **Teren je obavezan:** događaj se uvijek veže na teren iz baze (`venue_id NOT NULL`). Ako teren ne postoji, korisnik ga prvo doda (F-017).
- **Dobne granice i minimalna razina** na događaju ostaju. Nullable su, a NULL znači da uvjeta nema.
- **Prava:** migracije pokreće `lds_admin`. Backend se spaja kao `lds_app`, koji smije čitati i mijenjati podatke, ali ne i shemu.

## 2. Provjera proširivosti

Skica tablica za terene, prijave netočnosti i komentare ([`db/drafts/skica_V3_prosirenje.sql`](../../db/drafts/skica_V3_prosirenje.sql)) primijenjena je na V2 s oglednim podacima i prolazi bez izmjene V2. Nalazi:

**Tereni**

- Dodaju se novi stupci na `venues` i tablice `venue_sports`, `venue_photos`, `venue_reports` i `sif_report_status`.
- Novi `NOT NULL` stupac na tablici koja već ima podatke mora imati `DEFAULT` (npr. `is_free`).
- F-017 traži početnu fotografiju, ali baza to ne može jednostavno provjeriti, jer se fotografija upisuje nakon terena. Provjera ide u backend, a teren i fotografija upisuju se u istoj transakciji.

**Komentari**

- Odgovor na komentar (F-024) je stupac `reply_to_id`, koji pokazuje na isti `messages`.
- Brisanje komentara na koji netko odgovara baza odbija (provjereno). Zato komentare brišemo **soft deleteom** (`deleted_at`), a sučelje prikazuje "Komentar je obrisan".
- Pravilo "komentirati smiju samo sudionici" (F-013) može se zapisati kao FK `(event_id, sender_id)` → `event_members`. Tada se sudionik koji je komentirao ne može odjaviti (F-011), što je provjereno. Zato tu provjeru radi backend.

**Sudjelovanje**

- Zadnje slobodno mjesto kod istodobnih prijava (F-010): backend u transakciji zaključa događaj (`SELECT ... FROM events WHERE id = ? FOR UPDATE`), prebroji sudionike i tek onda upiše prijavu.

## 3. Otvorena pitanja za idući tjedan

- Potvrditi početne gradove (Zagreb, Split, Rijeka, Osijek) i razine vještine.
- Tko dovršava `users` (spol, profilna slika, uloga) i `oauth_accounts`, te uloge i ovlasti.
- Tko radi terene, a tko komentare. Koristiti skicu kao polazište.
- `venues` nema podatak je li teren besplatan (F-016), a ne postoji ni u ERD-u. Koja je zadana vrijednost?
- ERD uskladiti s V2 i izraditi u PlantUML-u za dokumentaciju (poglavlje 4.3.1).
- Analiza zahtjeva: uskladiti F-007 ("teren ili mjesto") i dodati dobne granice i minimalnu razinu na događaju.
- Produkcijska baza: tko pokreće migracije na produkciji i kada.
