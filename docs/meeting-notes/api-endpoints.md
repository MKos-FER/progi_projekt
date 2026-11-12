---
date_modified: 2026-11-12T09:48:55+01:00
---
# Let's Do Sports - REST API ugovor

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

| Metoda | Putanja           | Opis                                      | Podaci koji se šalju                                                                                                  | Permission    | Zahtjev |
| ------ | ----------------- | ----------------------------------------- | --------------------------------------------------------------------------------------------------------------------- | ------------- | ------- |
| GET    | `/users/me`       | Dohvat vlastitog profila                  | -                                                                                                                     | Authenticated | F-004   |
| PATCH  | `/users/me`       | Uređivanje postojećeg profila             | Body: `name`, `username`, `gender`, `birthDate`, `cityId`, `sports: [{sportId, skillLevelId}]` (samo mijenjana polja) | Authenticated | F-004   |
| PUT    | `/users/me/photo` | Postavljanje/zamjena profilne fotografije | Multipart: `file`                                                                                                     | Authenticated | F-004   |
| GET    | `/users`          | Pretraga i filtriranje igrača             | Query: `sportId`, `cityId`, `ageMin`, `ageMax`, `skillLevelId`                                                        | Authenticated | F-005   |
| GET    | `/users/{userId}` | Pregled profila odabranog igrača          | Path: `userId`                                                                                                        | Authenticated | F-005   |

Sve putanje iz ove tablice imaju prefiks `/api/v1`. Za F-005 rute pregleda drugih igrača i pretraživanja još nisu implementirane.

**Pravila filtriranja igrača (F-005):** svi filtri su opcionalni, a poslani filtri primjenjuju se zajedno. Izostavljeni filtri ne ograničavaju rezultate. ID-evi se preuzimaju iz šifrarnika.

| Filtar         | Značenje                                                                |
| -------------- | ----------------------------------------------------------------------- |
| `cityId`       | Igrači čiji profil ima odabrani grad.                                   |
| `sportId`      | Igrači koji imaju odabrani sport.                                       |
| `skillLevelId` | Igrači s točno odabranom razinom iskustva za sport naveden u `sportId`. |

`skillLevelId` zahtijeva `sportId`; ako se pošalje bez njega, backend vraća `400 Bad Request`. Razina se uspoređuje za isti odabrani sport, a ne za neki drugi sport u profilu.

Primjer: `GET /api/v1/users?cityId=1&sportId=2&skillLevelId=1` vraća igrače iz grada `1` koji imaju sport `2` s točno razinom `1`. Vrijednosti su ilustrativne.

### 3.1 JSON model i pravila za F-004

| Polje              | Tip i značenje                                                                                                                                                                    |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `name`, `username` | Neprazni tekstovi; username je jedinstven uz razlikovanje velikih i malih slova i služi prikazu.                                                                                  |
| `gender`           | Obavezan neprazan tekst.                                                                                                                                                          |
| `birthDate`        | Obavezan datum oblika `YYYY-MM-DD`; budući datum nije dopušten.                                                                                                                   |
| `cityId`           | Obavezan pozitivan cjelobrojni ID postojećeg grada (1–32767).                                                                                                                     |
| `sports`           | Neprazan popis `{sportId, skillLevelId}`; oba ID-a moraju postojati i biti cijeli brojevi 1–32767. Svaki element sadrži točno ta dva polja. Svaki sport navodi se najviše jednom. |
| `profilePhotoUrl`  | Samo u odgovoru; URL koji vraća adapter za pohranu fotografije. Ne prima se kroz `PATCH`.                                                                                         |

`GET` i uspješan `PATCH`/`PUT` vraćaju isti flat JSON model:

```json
{
  "id": 1,
  "name": "Ana Horvat",
  "username": "ana",
  "profilePhotoUrl": null,
  "gender": "žensko",
  "birthDate": "2002-03-14",
  "age": 24,
  "cityId": 1,
  "appRoleId": "USER",
  "sports": [{"sportId": 1, "skillLevelId": 2}]
}
```

Vrijednosti su ilustrativne. `age` se računa prema trenutačnom datumu u zoni `Europe/Zagreb`. Datum rođenja je obavezan. ID-eve gradova, sportova i iskustva treba preuzeti iz šifrarnika.

**Svaki spremljeni profil** ima ime, username, neprazan spol, datum rođenja, grad i barem jedan sport s iskustvom. Fotografija je opcionalna. Nepotpun profil se ne sprema; `sports: []` vraća `400`.

`PATCH` mijenja samo poslana polja; izostavljena ostaju nepromijenjena. `null` za obavezna polja, prazan spol i prazan ili `null` popis sportova vraćaju `400`. Ako je `sports` naveden, zamjenjuje cijeli popis. Nepoznata polja, uključujući `userId` i `appRoleId`, odbijaju se s `400`. Spajanje i spremanje izvršavaju se u jednoj transakciji. Početna registracija nije `PATCH /users/me`; OAuth sloj prvo dovršava obrazac i poziva `createWithOAuth`. Ta registracijska integracija još nije implementirana.

### 3.2 Odgovor za pregled igrača (F-005)

`GET /api/v1/users` vraća JSON popis igrača, a `GET /api/v1/users/{userId}` jedan objekt igrača. Svaki element popisa i pojedinačni profil imaju isti skup polja. Obje rute dostupne su samo prijavljenim korisnicima; zahtjev bez prijave vraća `401`. Rute još nisu implementirane.

| Polje             | Značenje                                                                                                      |
| ----------------- | ------------------------------------------------------------------------------------------------------------- |
| `id`              | Identifikator igrača za otvaranje profila.                                                                    |
| `username`        | Korisničko ime za prikaz.                                                                                     |
| `name`            | Ime koje je korisnik unio.                                                                                    |
| `profilePhotoUrl` | URL fotografije ili `null`; bez fotografije sučelje prikazuje zamjensku sliku.                                |
| `age`             | Dob izračunata prema trenutačnom datumu u zoni `Europe/Zagreb`.                                               |
| `city`            | Grad kao `{id, name}`.                                                                                        |
| `sports`          | Odabrani sportovi i iskustvo; svaki element sadrži `sportId`, `sportName`, `skillLevelId` i `skillLevelName`. |

Primjer jednog igrača (vrijednosti su ilustrativne):

```json
{
  "id": 12,
  "username": "ana",
  "name": "Ana",
  "profilePhotoUrl": null,
  "age": 24,
  "city": {"id": 1, "name": "Zagreb"},
  "sports": [
    {
      "sportId": 1,
      "sportName": "Nogomet",
      "skillLevelId": 2,
      "skillLevelName": "Average"
    }
  ]
}
```

Odgovori za pregled drugih igrača ne sadrže datum rođenja, spol, e-poštu, OAuth podatke ni aplikacijsku ulogu. Nazivi grada, sportova i iskustva vraćaju se uz ID-eve radi izravnog prikaza u sučelju. Prazan rezultat pretrage vraća `200` i `[]`.

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
4. **Lokacija i privatnost:** korisničku trenutačnu GPS lokaciju nije potrebno pohranjivati; za teren treba spremiti odabrane koordinate. Pri pregledu drugih igrača vraća se dob, a datum rođenja, spol, e-pošta, OAuth podaci i aplikacijska uloga izostavljaju se iz odgovora (F-005).
