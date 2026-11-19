---
date_created: 2026-11-13
---

# Baza: korisnici i vanjski računi

Opis tablica za korisnički profil i prijavu Googleom nakon migracija V4–V6. Polazište su [API ugovor](api-endpoints.md#31-json-model-i-pravila-za-f-004) i [analiza zahtjeva](../2-Analiza-zahtjeva.md) (F-001 – F-005, C-06). Pravila koja baza uvodi, a ugovor ih još ne navodi, označena su u odjeljku 5.

## 1. Tablice i polja

### `users`

| Stupac | Tip | Obavezno | Pravilo |
| --- | --- | --- | --- |
| `id` | `bigint` identity | da | primarni ključ |
| `username` | `text` | da | jedinstven, razlikuje velika i mala slova, nije prazan |
| `name` | `text` | da | nije prazan |
| `gender` | `text` | da | `Male`, `Female` ili `Other` (V4); sučelje prikazuje prijevod |
| `date_of_birth` | `date` | da | ne prije 1900. godine (V6); da nije u budućnosti provjerava backend |
| `city_id` | `smallint` | da | FK → `sif_city` |
| `profile_photo_url` | `text` | ne | URL koji vraća pohrana fotografija |
| `app_role_id` | `text` | da | FK → `sif_app_role`, zadano `USER` |
| `created_at` | `timestamptz` | da | zadano `now()` |

Sportovi i razine korisnika su u tablici `user_sports` (V3), s `UNIQUE (user_id, sport_id)`. Od V7 baza pri `COMMIT`-u odbija transakciju nakon koje postojeći korisnik nema nijedan sport (odgođeni okidač `user_sports_last_sport_guard`), pa zamjena cijelog popisa u jednoj transakciji prolazi, a brisanje posljednjeg sporta ne.

### `oauth_accounts`

| Stupac | Tip | Obavezno | Pravilo |
| --- | --- | --- | --- |
| `id` | `bigint` identity | da | primarni ključ |
| `user_id` | `bigint` | da | FK → `users`, `ON DELETE CASCADE` |
| `provider` | `text` | da | samo `google` (V6) |
| `provider_user_id` | `text` | da | Googleov `sub`; nije prazan |
| `email` | `text` | ne | samo informativno |
| `linked_at` | `timestamptz` | da | zadano `now()` |

Ograničenja:
- `UNIQUE (provider, provider_user_id)`: jedan Google račun pripada najviše jednom korisniku.
- `UNIQUE (user_id, provider)` (V6): korisnik ima najviše jedan Google račun (C-06).

### Uloge i ovlasti

| Tablica | Sadržaj |
| --- | --- |
| `sif_app_role` | `USER`, `ADMIN` |
| `sif_permissions` | `VENUE_EDIT_ANY`, `VENUE_DELETE_ANY`, `VENUE_REPORT_MANAGE` |
| `app_role_has_permissions` | `ADMIN` ima sve tri ovlasti, `USER` nijednu |

Ovlasti `Own`, `Organizer` i `Participant` iz API ugovora ne spremaju se kao uloge. Backend ih provjerava nad konkretnim zapisom.

## 2. Odnosi

- `users` – `oauth_accounts`: baza dopušta najviše jedan Google račun po korisniku. Da svaki korisnik ima točno jedan osigurava backend, jer se račun stvara zajedno s Google identitetom (F-001).
- `users` 1 : N `user_sports` N : 1 `sports`, uz razinu iz `sif_skill_level`.
- `users` N : 1 `sif_city`, `users` N : 1 `sif_app_role`.
- `sif_app_role` N : M `sif_permissions` preko `app_role_has_permissions`.

## 3. Što provjerava backend

Ova pravila baza ne može jednostavno provesti, pa ih provodi backend:

- Korisnik, njegovi sportovi i Google račun upisuju se u **jednoj transakciji**, tek kad su uneseni svi obavezni podaci (F-001). Tako svaki korisnik ima Google račun.
- Profil ima barem jedan sport, i pri stvaranju i pri uređivanju (F-004). Backend to javlja kao `400`; V7 je samo zadnja zaštita u bazi.
- Datum rođenja nije u budućnosti. Dob se računa u zoni `Europe/Zagreb`.
- Korisnik se prepoznaje samo po paru `provider` + `provider_user_id`, nikad po e-pošti (F-002).
- Unos koji krši ograničenje baze vraća `400` ili `409` s razumljivom porukom, a ne grešku poslužitelja. To vrijedi i za `UNIQUE` (zauzeto korisničko ime, već povezan račun) i za `CHECK` (spol izvan dopuštenih vrijednosti, datum rođenja prije 1900.).

## 4. Provjera migracija

Provjereno 13. 11. 2026. na privremenim bazama (PostgreSQL 18, Flyway 13):

| Scenarij | Rezultat |
| --- | --- |
| Čista baza: V1–V6, seed dvaput zaredom, `validate` | prolazi |
| Baza na V3 s oglednim podacima: postupak iz [README-a baze](../../db/README.md#nadogradnja-lokalne-baze-s-v3-na-noviju-verziju), zatim `validate` | prolazi, završava na V6 |
| Baza koja je pokrenula ispravljeni V4 i V5 s brancha `feature/event-api` | V6 prolazi, nema nepodudaranja kontrolnih zbrojeva |
| Baza zaustavljena na neispravnom V4 s mastera (zapisani V1–V3) | obična `migrate` primijeni V4–V6 |

Negativni testovi kao `lds_app`: drugi Google račun istog korisnika, `provider = 'Google'`, datum rođenja `1899-12-31` i spol `žensko` baza odbija.

## 5. Otvoreno

- **Prijedlog:** backend sprema ime i korisničko ime bez razmaka na početku i kraju. To još nije dio ugovora.
- **ERD (Roko):** sljedeću verziju ERD-a uskladiti s V4–V6.
- Brisanje korisničkog računa nije u zahtjevima. Korisnik koji je organizirao događaj trenutačno se ne može obrisati (`events.created_by_id` nema `ON DELETE`).
