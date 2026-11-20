---
date_created: 2026-11-14
date_modified: 2026-11-18
---

# Autentifikacija, registracija i profil: API ugovor (SHARED-02)

Kratki ugovor za zadatke #54 – #72. Proširuje [API ugovor](api-endpoints.md) i ne ponavlja ga. Polazišta: [analiza zahtjeva](../2-Analiza-zahtjeva.md) (F-001 – F-004, NF-01 – NF-05), [opis tablica korisnika](korisnici-i-oauth.md) i implementacija iz PR-a #76 (`SecurityConfig`, `RegistrationController`, `CsrfController`, `CurrentUserArgumentResolver`). Oznaka **✓** znači da je dio već implementiran na `master`u, a **Prijedlog** da je to nova odluka za tim. Dijagram: [`auth-tijek.puml`](../puml/auth-tijek.puml).

## 0. Sažetak odluka

- Prijava samo putem Googlea (OIDC), bez lozinki. Korisnika određuje par `google` + `sub`. E-pošta nikad ne spaja račune. ✓
- Prijava sama nikad ne stvara korisnika. `users`, `user_sports` i `oauth_accounts` upisuju se u **jednoj transakciji** kroz `POST /api/v1/auth/register`. ✓
- Sesija na poslužitelju (`JSESSIONID`) i uključen CSRF. Frontend nikad ne šalje `userId`. ✓
- Backend pri svakom zahtjevu u `oauth_accounts` provjerava je li Google račun registriran, pa se sesija nakon registracije ne mijenja. ✓

## 1. Tijek korisnika

1. Na `/login` korisnik klikne "Prijava s Googleom". Frontend radi navigaciju cijele stranice na `/oauth2/authorization/google`, ne `fetch`. ✓
2. Google vraća korisnika na `${BACKEND_URL}/oauth2/login/code/google`. Spring provjerava `state`, mijenja `code` za tokene, provjerava ID token (NF-01) i mijenja ID sesije. ✓
3. Ako je Google račun povezan s korisnikom, backend preusmjerava na `${FRONTEND_URL}/`, a inače na `${FRONTEND_URL}/register`. U bazu se ništa ne upisuje. ✓
4. Frontend stanje prijave čita s `GET /api/v1/users/me` (§2). **Prijedlog**, ovisi o #63.
5. Prije prvog `POST`/`PUT`/`PATCH`/`DELETE` frontend poziva `GET /api/v1/auth/csrf`, koji postavlja cookie `XSRF-TOKEN`. ✓
6. Na `/register` frontend dohvaća `/sports`, `/cities` i `/skill-levels` (#64) i šalje `POST /api/v1/auth/register` (§4). ✓
   - `201 {"id": …}`: korisnik je stvoren. Frontend ponovno dohvaća `/users/me`, koji sad vraća `200`, i ide na `/`.
   - `400`: greške uz polja. `409`: poruka iz odgovora (§5).
7. Sljedeća prijava istim Google računom ide ravno na `/`. ✓
8. Uređivanje profila: `GET`/`PATCH /api/v1/users/me` ([§3.1 API ugovora](api-endpoints.md#31-json-model-i-pravila-za-f-004)) i `PUT /api/v1/users/me/photo`.
9. Odjava: `POST /api/v1/auth/logout` → `204`. Backend poništava sesiju i briše cookieje `JSESSIONID` i `XSRF-TOKEN`. ✓

| Situacija | Što se događa | Poruka |
| --- | --- | --- |
| Odustajanje ili greška kod Googlea | `${FRONTEND_URL}/login?loginError` ✓ | "Prijava putem Googlea nije uspjela ili je otkazana." |
| Sesija istekne | zaštićena ruta vraća `401` → `/login` | "Sesija je istekla. Prijavi se ponovno." |
| "Odustani" na obrascu za registraciju | `POST /auth/logout`, u bazi ništa | `/`, neprijavljen |
| Obrazac napušten | sesija istekne nakon 30 min, u bazi ništa | sljedeća prijava kreće ispočetka |
| Drugi Google račun, i s istom e-poštom | novi `sub` → `/register` | obrazac za registraciju |

## 2. Stanja prijave

| Stanje | Kako nastaje | `GET /users/me` | Ostale zaštićene rute |
| --- | --- | --- | --- |
| Neprijavljen | nema sesije, istekla je ili odjava | `401` | `401`, prazno tijelo ✓ |
| Registracija u tijeku | Google prijava uspjela, račun nije povezan | `403` | `403 {"message": "Registracija nije dovršena."}` ✓; dopušten je `POST /auth/register` |
| Prijavljen | povezan Google račun | `200` + profil | prema oznakama API ugovora; `POST /auth/register` → `409` ✓ |

- `CurrentUserArgumentResolver` za svaki zahtjev s `@CurrentUserId` traži `oauth_accounts.user_id` po `provider` i `sub` iz sesije: nema prijave → `401`, nema povezanog korisnika → `403`. ✓
- **Prijedlog:** `GET /users/me` (#63) koristi `@CurrentUserId`, pa je on ujedno i provjera stanja. Zaseban endpoint za stanje nije potreban.
- Uloga `ADMIN` (F-021) još se ne provjerava. **Prijedlog:** servis čita `users.app_role_id` kad zatreba.

## 3. Endpointi

Putanje osim prve dvije imaju prefiks `/api/v1`. Šifrarnici su u [§4 API ugovora](api-endpoints.md#4-sportovi-pomoćni-podaci-za-f-004-f-005-f-006-f-007-f-016-f-017).

| Metoda | Putanja | Tko | Zahtjev | Uspjeh | Greške | Stanje |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/oauth2/authorization/google` | svi | navigacija | `302` na Google | – | ✓ |
| GET | `/oauth2/login/code/google` | Google | `code`, `state` ili `error` | `302` na `/` ili `/register` | `302` na `/login?loginError` | ✓ |
| GET | `/auth/csrf` | svi | – | `204` + cookie `XSRF-TOKEN` | – | ✓ |
| POST | `/auth/register` | registracija u tijeku | JSON (§4), `X-XSRF-TOKEN` | `201 {"id": …}` | `400`, `401`, `403`, `409` | ✓ |
| POST | `/auth/logout` | svi | `X-XSRF-TOKEN` | `204` | `403` CSRF | ✓ |
| GET, PATCH | `/users/me` | prijavljen | PATCH: promijenjena polja | `200` profil | `400`, `401`, `403`, `409` | #63 |
| GET, PUT | `/users/me/photo` | prijavljen | PUT: `multipart/form-data`, dio `file` | `200 {profilePhotoUrl, csrfToken, csrfHeaderName}` | `400`, `401`, `403`, `413`, `503` | ✓ (#77) |
| GET | `/profile-photos/{name}` | prijavljen | – | `200` JPEG ili PNG | `404` | ✓ (#77) |
| GET | `/sports`, `/cities`, `/skill-levels` | svi | – | `200` popis | – | #64 |

**Fotografija (#67, ✓ #77):** JPEG ili PNG do 5 MiB i 4096 × 4096 piksela. Sprema se pod UUID imenom u `${PROFILE_PHOTO_DIRECTORY}`, a `profilePhotoUrl` pokazuje na `/api/v1/profile-photos/{name}`. Detalji su u [profile-photo.md](../profile-photo.md).

Rute `/cities` i `/skill-levels` postaju javne u PR-u #84.

## 4. JSON

`POST /api/v1/auth/register`:

```json
{"name": "Ana Horvat", "username": "ana", "gender": "Female", "dateOfBirth": "2002-03-14",
 "cityId": 1, "sports": [{"sportId": 1, "skillLevelId": 2}, {"sportId": 3, "skillLevelId": 1}]}
```

Odgovor `201`: `{"id": 12}`. `sub` i e-pošta dolaze iz sesije, nikad iz tijela zahtjeva.

> **Naziv polja:** datum rođenja je `dateOfBirth` u svim JSON-ima, kao `users.date_of_birth`, `RegisterRequest` i frontend (dogovor na reviewu #75). `RegistrationIntegrationTest` još šalje `birthDate`, pa 3 od 6 testova padaju dok se test ne uskladi.

| Polje | Pravilo | Greška |
| --- | --- | --- |
| `name` | nije prazno, sprema se bez razmaka na rubovima ✓ | `400 errors.name` |
| `username` | nije prazno, jedinstveno (razlikuje velika i mala slova), bez razmaka na rubovima ✓ | `400 errors.username`, `409 "Korisničko ime je zauzeto."` |
| `gender` | `Male`, `Female` ili `Other` ✓ | `400 errors.gender` |
| `dateOfBirth` | obavezan, nije u budućnosti ✓; od `1900-01-01` (V6) | `400 errors.dateOfBirth` |
| `cityId` | postojeći grad ✓ | `400 errors.cityId` |
| `sports` | barem jedan, postojeći ID-evi, sport se ne ponavlja ✓ | `400 errors.sports` |

Isti validator vrijedi za `PATCH /users/me`, gdje su sva polja opcionalna.

Odgovor s greškom ima oblik iz `ApiExceptionHandler` ✓: `400` vraća `{"errors": {"polje": "poruka"}}`, ostali kodovi `{"message": "…"}`. Iznimka su `401` i CSRF `403` iz Spring Securityja, koji nemaju polje `message`.

```ts
type Gender = 'Male' | 'Female' | 'Other';   // sučelje: "Muško", "Žensko", "Drugo"
type AppRole = 'USER' | 'ADMIN';
interface UserSport { sportId: number; skillLevelId: number; }
interface MyProfile { id: number; name: string; username: string; profilePhotoUrl: string | null;
  gender: Gender; dateOfBirth: string; age: number; cityId: number; appRoleId: AppRole; sports: UserSport[]; }
interface RegisterRequest { name: string; username: string; gender: Gender; dateOfBirth: string;
  cityId: number; sports: UserSport[]; }
interface ApiError { message?: string; errors?: Record<string, string>; }
```

`frontend/src/types/user.ts` zamjenjuje se ovim tipovima: `camelCase`, `AppRole` samo `USER`/`ADMIN`, obavezni `gender`, `dateOfBirth` i `cityId`.

## 5. Statusni kodovi

| Status | Kada |
| --- | --- |
| `200`, `201`, `204` | dohvat i izmjena, registracija, odjava i `/auth/csrf` |
| `400` | neispravan JSON ili polje, nepostojeći ID, ponovljen sport; uvijek s `errors` |
| `401` | nema prijave ili je sesija istekla (prazno tijelo) |
| `403` | `"Registracija nije dovršena."`; bez `message` kad nedostaje ili ne vrijedi `X-XSRF-TOKEN`; prijavljen, ali bez ovlasti |
| `404` | resurs ne postoji |
| `409` | `"Korisničko ime je zauzeto."` ili `"Registracija je već dovršena."` |
| `413` | fotografija veća od 5 MiB ✓ (#77) |

**Prijedlog: baza nikad ne vraća `500`.** `RegistrationService` prvo sam provjerava pravila. Kod istodobnih zahtjeva ograničenje baze ipak može puknuti (`users_username_key`, `oauth_accounts_provider_provider_user_id_key`, `oauth_accounts_user_provider_key`), a `CHECK` iz V6 hvata datum rođenja prije 1900. `ApiExceptionHandler` treba `DataIntegrityViolationException` preslikati u `409` ili `400`.

## 6. Sesija, CSRF i proxy

- **Cookie** `JSESSIONID` je `HttpOnly`. Ništa se ne sprema u `localStorage`. Sesija istječe nakon 30 min neaktivnosti (zadano). Spring mijenja ID sesije nakon Google prijave. ✓
- **CSRF** ✓: `csrf(c -> c.spa())`. Frontend čita cookie `XSRF-TOKEN` i šalje ga u zaglavlju `X-XSRF-TOKEN` uz `POST`, `PUT`, `PATCH` i `DELETE`. Cookie postavlja `GET /api/v1/auth/csrf`.
- **Povratak s Googlea** ✓: `redirect-uri=${BACKEND_URL}/oauth2/login/code/{registrationId}`, a uspjeh i greška preusmjeravaju na `${FRONTEND_URL}`. Lokalno je `BACKEND_URL=http://localhost:8080` i `FRONTEND_URL=http://localhost:5173`, a u produkciji su oba `https://letsdosports.eu`.
- **Vite proxy** (Prijedlog, #59): u razvoju frontend na `:5173` prosljeđuje `/api` i `/oauth2` na `:8080`, pa CORS nije potreban. Cookieji vrijede za `localhost` bez obzira na port, pa je sesija iz povratka na `:8080` vidljiva i na `:5173`.
- **`/login` i `/register` su React stranice.** `loginPage(FRONTEND_URL + "/")` isključuje Springovu generiranu stranicu, a zaštićene API rute vraćaju `401`, ne `302`. ✓
- **Prijedlog za produkciju:** `server.servlet.session.cookie.secure=true` i `server.forward-headers-strategy=framework`.

```ts
// frontend/vite.config.ts
server: { proxy: { '/api': 'http://localhost:8080', '/oauth2': 'http://localhost:8080' } }
```

## 7. Frontend pravila

- Stanje prijave je jedan TanStack Query, `['me']` (`GET /users/me`): `200` → prijavljen, `401` → neprijavljen, `403` → registracija u tijeku.
- Omotač u `api_v1.ts` pri pokretanju jednom poziva `/auth/csrf`, uz izmjene šalje `X-XSRF-TOKEN` i baca grešku sa `status` i `ApiError`. Ako izmjena dobije `403` bez `message`, ponovno dohvaća `/auth/csrf` i jednom ponavlja zahtjev.
- **Nakon `201`:** `await queryClient.invalidateQueries({ queryKey: ['me'] })`, zatim navigacija na `/`, pa po želji fotografija.
- `401` → očisti `['me']` i idi na `/login`. `403 "Registracija nije dovršena."` → `/register`. `409 "Registracija je već dovršena."` → ponovno dohvati `['me']` i idi na `/`. `409 "Korisničko ime je zauzeto."` → poruka uz polje `username`.
- **Prijedlog, zaštita ruta (#71):** javne su `/`, `/events/:eventId`, `/legal/*` i `/login`. Privatne su `/events`, `/reports` i `/profile/edit` (neprijavljen → `/login`, registracija u tijeku → `/register`). `/register` vrijedi samo dok je registracija u tijeku.
- **Navigacijska traka:** neprijavljen → "PRIJAVA"; registracija u tijeku → "Dovrši registraciju" i "Odustani" (odjava); prijavljen → korisničko ime i fotografija.
- **Fotografija:** provjera vrste i veličine prije slanja, `FormData` s poljem `file` i **bez** zaglavlja `Content-Type`.

## 8. Postavljanje za razvoj

1. Kos u Google Cloud Consoleu drži consent screen u statusu **Testing**, s e-poštama svih članova kao test korisnicima. Web OAuth klijent ima redirect URI-je `http://localhost:8080/oauth2/login/code/google` i `https://letsdosports.eu/oauth2/login/code/google`.
2. `backend/LetsDoSports/.env` nastaje iz `.env.example`. `GOOGLE_CLIENT_ID` i `GOOGLE_CLIENT_SECRET` dijele se privatno, nikad u gitu ni u issueima. ✓
3. Baza se pokreće iz `db/` (`docker compose up -d` ili `make db`), a backend s `make run` i `make test`, koji čitaju `db/.env`. ✓
4. Backend traži Javu 23 ili noviju (`java.version` u `pom.xml`).

## 9. Što radi tko

| Zadatak | Dio ugovora |
| --- | --- |
| #54 – #56 REG (backend) | osnova je u #76 ✓; ostaje test s `dateOfBirth` (§4) i `409`/`400` umjesto `500` (§5) |
| #57, #65 REG (frontend) | `/login?loginError`, `/register` s pravilima iz §4, "Odustani", §7 |
| #58, #59 LOGIN | preusmjeravanja ✓; stanje preko `['me']`, navigacijska traka, `api_v1.ts`, Vite proxy |
| #60 LOGIN-03 | testovi po uzoru na `RegistrationIntegrationTest` (`oauth2Login().clientRegistration(...).attributes(...)`), za sva tri stanja iz §2 |
| #61, #62 LOGOUT | backend ✓; gumb s `X-XSRF-TOKEN` i čišćenje `['me']` |
| #63, #66 PROFILE | `GET`/`PATCH /users/me` s `@CurrentUserId`, `409` za zauzet `username` |
| #64 PROFILE-02 | `/sports`, `/cities`, `/skill-levels` (javne u `SecurityConfig`); `user_sports` pri `PATCH` |
| #67 PROFILE-05 | ✓ (#77), detalji u [profile-photo.md](../profile-photo.md) |
| #68 PROFILE-06 | pravila iz §4 i §5, `401`, `403` u registraciji i bez CSRF-a |
| #69 SHARED-01 | ✓ (V4 – V6) |
| #71, #72 SHARED | `SecurityConfig` ✓ i zaštita ruta iz §7; E2E scenariji iz §1 |

## 10. Otvoreno

- Uskladiti `dateOfBirth`: `RegistrationIntegrationTest` (šalje `birthDate`) i `UpdateUserRequest` na `feature/users-api`.
- Nepoznata polja u JSON-u: §3.1 traži `400`, a Spring ih sada prešutno ignorira. Zato greška s nazivom datuma rođenja nije bila vidljiva. **Prijedlog:** `spring.jackson.deserialization.fail-on-unknown-properties=true`.
- Kako backend provjerava ulogu `ADMIN` (F-021)?
- Postoji li minimalna dob? Zasad vrijedi raspon od 1900. do danas.
