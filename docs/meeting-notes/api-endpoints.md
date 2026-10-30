# Let's Do Sports - prijedlog REST API-ja

**Prijedlog ugovora između React frontenda i Spring Boot backenda**

## 1. Opća pravila

- **API prefiks:** `/api/v1` (osim standardnih OAuth/OIDC ruta koje obrađuje Spring Security).
- **Format:** JSON (`application/json`), osim slanja fotografija (`multipart/form-data`).
- **Nazivi:** množina za kolekcije (`/users`, `/events`, `/venues`, `/sports`), `camelCase` za JSON polja.
- **Identifikator korisnika:** određuje backend iz valjane autentificirane sesije; frontend ne šalje `userId` za radnje koje obavlja u svoje ime.
- **Permission:** u tablicama opisuje *uvjet pristupa*, ne nužno zasebnu permission-vrijednost ili aplikacijsku rolu.
- **Statusi:**
	- `200 OK` (dohvat/izmjena)
	- `201 Created` (kreiranje)
	- `204 No Content` (brisanje/odjava)
	- `400 Bad Request` (neispravan unos)
	- `401 Unauthorized` (nije prijavljen)
	- `403 Forbidden` (nema ovlast)
	- `404 Not Found` (resurs ne postoji)
	- `409 Conflict` (sukob poslovnih pravila).
- **Napomena o oznakama permissiona:**
	- `Public` = i bez prijave
	- `Authenticated` = registrirani i prijavljeni
	- `Organizer` = organizator konkretnog događaja
	- `Participant` = sudionik/organizator konkretnog događaja
	- `Own` = autor/vlasnik konkretnog resursa
	- `Admin` = administrator.
	- `Own/Admin` znači autor terena ili administrator.

## 2. Autentifikacija (F-001 do F-003)

Sljedeće dvije rute su primjer **standardnih Spring Security ruta**, a nisu dio aplikacijskog `/api/v1` API-ja. Stvarne rute ovise o konfiguraciji.

| Metoda | Putanja                                  | Opis                                                                                 | Podaci koji se šalju                              | Permission    | Zahtjev      |
| ------ | ---------------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------- | ------------- | ------------ |
| GET    | `/oauth2/authorization/{registrationId}` | Pokreće vanjsku prijavu, npr. putem Googlea                                          | Path: `registrationId`                            | Public        | F-001, F-002 |
| GET    | `/login/oauth2/code/{registrationId}`    | OAuth/OIDC povratni poziv koji obrađuje Spring Security; frontend ga ne poziva ručno | Query: `code`, `state` (vraća pružatelj prijave)  | OAuth flow    | F-001, F-002 |
| POST   | `/api/v1/auth/logout`                    | Odjavljuje korisnika iz aplikacije                                                   | Bez bodyja; sesijski cookie ako se koristi sesija | Authenticated | F-003        |

Nakon uspješnog vanjskog logina backend mora provjeriti identitet, dohvatiti postojećeg korisnika ili stvoriti novog, a frontend može koristiti `GET /api/v1/users/me` kako bi dohvatio profil i podatak je li profil dovršen. Nema API-ja za lokalno postavljanje lozinke.

## 3. Korisnici i profili (F-004, F-005)

| Metoda | Putanja           | Opis                                            | Podaci koji se šalju                                                                                              | Permission    | Zahtjev |
| ------ | ----------------- | ----------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- | ------------- | ------- |
| GET    | `/users/me`       | Dohvat vlastitog profila (i stanja dovršenosti) | -                                                                                                                 | Authenticated | F-004   |
| PATCH  | `/users/me`       | Prvi unos ili naknadno uređivanje profila       | Body: `name`, `username`, `gender`, `birthDate`, `city`, `sports: [{sportId, skillLevel}]` (samo mijenjana polja) | Authenticated | F-004   |
| PUT    | `/users/me/photo` | Postavljanje/zamjena profilne fotografije       | Multipart: `file`                                                                                                 | Authenticated | F-004   |
| GET    | `/users`          | Pretraga i filtriranje igrača                   | Query: `sportId`, `city`, `ageMin`, `ageMax`, `skillLevel`                                                        | Authenticated | F-005   |
| GET    | `/users/{userId}` | Pregled profila odabranog igrača                | Path: `userId`                                                                                                    | Authenticated | F-005   |

**Prijedlog podatkovnog modela:** `sports` u profilu je popis parova `{sportId, skillLevel}` (razina iskustva ovisi o sportu). Točan model i dopuštene vrijednosti za `skillLevel` treba dogovoriti. `username` služi prikazu, ne prijavi.

## 4. Sportovi (pomoćni podaci za F-004, F-005, F-006, F-007, F-016, F-017)

| Metoda | Putanja             | Opis                                                                           | Podaci koji se šalju | Permission | Zahtjev             |
| ------ | ------------------- | ------------------------------------------------------------------------------ | -------------------- | ---------- | ------------------- |
| GET    | `/sports`           | Popis sportova za filtre i obrasce                                             | -                    | Public     | F-004, F-007, F-017 |
| GET    | `/sports/{sportId}` | Detalji pojedinog sporta; **opcionalno** ako popis sadrži sve potrebne podatke | Path: `sportId`      | Public     | Pomoćni             |

## 5. Događaji (F-006 do F-009)

| Metoda | Putanja                    | Opis                                                                 | Podaci koji se šalju                                                     | Permission    | Zahtjev                 |
| ------ | -------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------ | ------------- | ----------------------- |
| GET    | `/events`                  | Javni popis događaja i filtriranje                                   | Query: `sportId`, `city`, `from`, `to`, `status`                         | Public        | F-006                   |
| GET    | `/events/{eventId}`        | Detalji događaja, status, organizator i broj sudionika               | Path: `eventId`                                                          | Public        | F-006                   |
| GET    | `/users/me/events`         | Događaji na kojima je prijavljeni korisnik organizator ili sudionik  | Query: `status`                                                          | Authenticated | Pomoćni za F-010, F-011 |
| POST   | `/events`                  | Kreira događaj i automatski upisuje organizatora kao prvog sudionika | Body: `sportId`, `venueId`, `startsAt`, `maxParticipants`, `description` | Authenticated | F-007                   |
| PATCH  | `/events/{eventId}`        | Mijenja osnovne podatke događaja                                     | Body: bilo koji dopušteni podskup polja iz POST zahtjeva                 | Organizer     | F-008                   |
| POST   | `/events/{eventId}/cancel` | Otkazuje događaj bez brisanja zapisa (`status = CANCELLED`)          | Path: `eventId`; bez bodyja                                              | Organizer     | F-009                   |

**Poslovna pravila:** organizator se računa u `maxParticipants`, koji mora biti barem 1. Pri promjeni `maxParticipants` nova vrijednost ne smije biti manja od trenutačnog broja sudionika. Otkazani događaj ostaje dohvatljiv i ne prima nove prijave. Za događaj mora postojati lokacija: registrirani `venueId`. Vremena slati u ISO 8601 formatu s vremenskom zonom/offsetom.

## 6. Sudionici događaja (F-010, F-011)

| Metoda | Putanja                        | Opis                                             | Podaci koji se šalju        | Permission                     | Zahtjev      |
| ------ | ------------------------------ | ------------------------------------------------ | --------------------------- | ------------------------------ | ------------ |
| GET    | `/events/{eventId}/members`    | Popis sudionika događaja                         | Path: `eventId`             | Authenticated (predloženo)     | F-006, F-010 |
| POST   | `/events/{eventId}/members`    | Prijavljuje **trenutačnog korisnika** na događaj | Path: `eventId`; bez bodyja | Authenticated                  | F-010        |
| DELETE | `/events/{eventId}/members/me` | Odjavljuje **trenutačnog korisnika** s događaja  | Path: `eventId`; bez bodyja | Participant, osim organizatora | F-011        |

**Poslovna pravila:** nema dvostruke prijave, prijave na otkazani/pun događaj niti odobravanja od organizatora. Istodobne prijave za zadnje mjesto moraju biti riješene u bazi/transakciji. Organizator ne može napustiti vlastiti događaj ovom rutom: treba ga otkazati.

## 7. Oglasna ploča događaja - komentari (F-012 do F-014, F-024)

| Metoda | Putanja                                  | Opis                                                                     | Podaci koji se šalju                        | Permission  | Zahtjev      |
| ------ | ---------------------------------------- | ------------------------------------------------------------------------ | ------------------------------------------- | ----------- | ------------ |
| GET    | `/events/{eventId}/comments`             | Kronološki popis komentara; odgovor sadrži referencu na izvorni komentar | Path: `eventId`                             | Public      | F-012, F-024 |
| POST   | `/events/{eventId}/comments`             | Objavljuje komentar ili odgovor na komentar                              | Body: `text`, opcionalni `replyToCommentId` | Participant | F-013, F-024 |
| DELETE | `/events/{eventId}/comments/{commentId}` | Briše vlastiti komentar                                                  | Path: `eventId`, `commentId`                | Own         | F-014        |

**Poslovna pravila:** tekst ne smije biti prazan; `replyToCommentId` mora upućivati na komentar **istog događaja**. Backend postavlja autora i vrijeme objave. Odgovori ostaju u **jedinstvenom kronološkom popisu**, a ne u zasebnim threadovima. Frontend može prikazati autora/sažetak izvornog komentara iz povezanih podataka. Za brisane komentare koji već imaju odgovore preporuka je ne prikazivati izvorni tekst, ali očuvati referencu ili neutralnu oznaku radi konzistentnosti.

## 8. Tereni i fotografije (F-015 do F-019, F-022)

| Metoda | Putanja                    | Opis                                                           | Podaci koji se šalju                                  | Permission    | Zahtjev      |
| ------ | -------------------------- | -------------------------------------------------------------- | ----------------------------------------------------- | ------------- | ------------ |
| GET    | `/venues`                  | Javni popis terena i oznaka za kartu                           | Query: `sportId`, `city`                              | Public        | F-015        |
| GET    | `/venues/{venueId}`        | Detalji terena, fotografije i vanjska poveznica za rezervaciju | Path: `venueId`                                       | Public        | F-016, F-022 |
| POST   | `/venues`                  | Kreira teren uz **obaveznu početnu fotografiju**               | Multipart: dio `data` (JSON) + dio `photo` (datoteka) | Authenticated | F-017        |
| PATCH  | `/venues/{venueId}`        | Mijenja metapodatke terena                                     | Body: dopušteni podskup polja terena, bez fotografija | Own/Admin     | F-018, F-021 |
| POST   | `/venues/{venueId}/photos` | Dodaje novu fotografiju postojećem terenu                      | Multipart: `file`                                     | Authenticated | F-019        |
| DELETE | `/venues/{venueId}`        | Uklanja neispravan teren (administratorski)                    | Path: `venueId`                                       | Admin         | F-021        |

**Polja terena (primjer):** `name`, `latitude`, `longitude`, `city`, `sportIds`, `description`, `isPaid`, `isReservable`, `reservationUrl`. Naziv, koordinate, barem jedan sport i **početna fotografija** obavezni su pri kreiranju. `reservationUrl` je vanjski URL, nije endpoint za rezervaciju. Za `GET /venues/{venueId}` uključiti npr. `photos: [{id, url}]`.

Kod Spring Boota `POST /venues` može koristiti `@RequestPart("data") VenueCreateRequest` i `@RequestPart("photo") MultipartFile`. Fotografije moraju ostati dostupne nakon ponovnog pokretanja aplikacije (NF-03).

## 9. Prijave netočnih podataka (F-020, F-021)

| Metoda | Putanja                           | Opis                                                    | Podaci koji se šalju                          | Permission    | Zahtjev |
| ------ | --------------------------------- | ------------------------------------------------------- | --------------------------------------------- | ------------- | ------- |
| POST   | `/venues/{venueId}/reports`       | Korisnik prijavljuje netočne podatke o terenu           | Path: `venueId`; body: `description`          | Authenticated | F-020   |
| GET    | `/admin/venue-reports`            | Administrator pregledava i filtrira prijave svih terena | Query: `status`, opcionalno `venueId`         | Admin         | F-021   |
| PATCH  | `/admin/venue-reports/{reportId}` | Administrator označava prijavu riješenom ili odbijenom  | Body: `status` (npr. `RESOLVED`, `DISMISSED`) | Admin         | F-021   |

**Predložena stanja:** `OPEN`, `RESOLVED`, `DISMISSED`. Popis vrijednosti je projektni dogovor, ne zaseban endpoint. Za ispravak/prijavljeno uklanjanje terena administrator koristi postojeće `PATCH /venues/{venueId}` i `DELETE /venues/{venueId}` pa zatim ažurira status prijave. Bez dodatnog zahtjeva ne uvoditi složen workflow za moderaciju.

## 10. Što ne treba novi API endpoint

| Funkcionalnost              | Rješenje                                                            | Razlog                                                                     |
| --------------------------- | ------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| Ogledni oglasi (F-023)      | Statički podaci/komponente React frontenda                          | Nema stvarne oglasne mreže ni upravljanja oglasima.                        |
| OSM karta (F-015)           | Frontend prikazuje kartu; vlastiti `GET /venues` daje oznake terena | Karte i tileovi dolaze iz odabranog kartografskog servisa.                 |
| Vanjska rezervacija (F-022) | `reservationUrl` dolazi u `GET /venues/{venueId}`                   | Rezervacija se odvija izvan aplikacije.                                    |
| Ponuda statusa prijave      | Enum na frontendu/backendu                                          | Nema potrebe za `/venues/reports/statuses` dok vrijednosti nisu dinamičke. |

## 11. Tehničke i sigurnosne napomene

1. **Ovlasti na backendu:** `Authenticated` se može zaštititi Spring Securityjem, dok `Organizer`, `Participant`, `Own` i `Own/Admin` trebaju provjeru identiteta nad konkretnim zapisom u servisu.
2. **Integritet prijava:** broj mjesta i unikatan par `(eventId, userId)` moraju se štititi u transakciji/bazi, ne samo frontend provjerom.
3. **HTTP statusi:** nakon `POST` kreiranja vratiti `201 Created` i identifikator/URI resursa. Neuspjeh validacije `400`, nedostatak prijave `401`, neovlašteno uređivanje `403`, nepostojeći zapis `404`, puni događaj i dvostruko članstvo `409`.
4. **Lokacija i privatnost:** korisničku trenutačnu GPS lokaciju nije potrebno pohranjivati; za teren treba spremiti odabrane koordinate. Precizan datum rođenja nije nužno prikazivati drugim korisnicima.
