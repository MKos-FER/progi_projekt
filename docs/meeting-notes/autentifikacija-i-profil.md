---
date_created: 2026-11-14
---

# Autentifikacija, registracija i profil: API ugovor (SHARED-02)

Kratki ugovor za zadatke #54 – #72. Proširuje [API ugovor](api-endpoints.md) (oznake `Public`/`Authenticated`/`Own`, model profila iz §3.1, šifrarnici iz §4) i ne ponavlja ga. Polazišta: [analiza zahtjeva](../2-Analiza-zahtjeva.md) (F-001 – F-004, NF-01 – NF-05), [opis tablica korisnika](korisnici-i-oauth.md) i PR #51. Nove odluke označene su s **Prijedlog**. Dijagram: [`auth-tijek.puml`](../puml/auth-tijek.puml).

**Ovisnosti:** ugovor vrijedi kad se spoje PR #51 (`ApiExceptionHandler`, `@CurrentUserId`), PR #73 (migracija V6) i PR #74 (`korisnici-i-oauth.md`). Do tada poveznica na opis tablica i ograničenja iz V6 ne postoje na `master`u.

## 0. Sažetak odluka

- Prijava samo putem Googlea (OIDC), bez lozinki. Korisnika određuje par `google` + `sub`. E-pošta nikad ne spaja račune.
- Prijava sama nikad ne stvara korisnika. `users`, `user_sports` i `oauth_accounts` upisuju se u **jednoj transakciji** tek kad je obrazac valjan.
- Sesija na poslužitelju (`JSESSIONID`), CSRF uključen, isti origin bez CORS-a (§6). Frontend nikad ne šalje `userId` ni `X-User-Id`.
- **Prijedlog:** stanje se čita s `GET /api/v1/auth/session`, registracija završava s `POST /api/v1/auth/registration`.

## 1. Tijek korisnika

1. Na `/login` korisnik klikne "Prijava s Googleom". Frontend radi navigaciju cijele stranice na `/oauth2/authorization/google`, ne `fetch`.
2. Spring sprema `state` u sesiju i preusmjerava na Google. Google vraća na `/login/oauth2/code/google?code=…&state=…`.
3. Spring provjerava `state`, mijenja `code` za tokene i provjerava ID token (NF-01).
4. U istom koraku `AppOidcUserService` traži korisnika po `sub` (§2), a nakon toga Spring mijenja ID sesije:
   - **postoji** → `AUTHENTICATED`, preusmjerenje na `/`;
   - **ne postoji** → `REGISTRATION_REQUIRED`, u bazu se ništa ne upisuje, preusmjerenje na `/register`.
5. Pri svakom učitavanju aplikacije frontend poziva `GET /auth/session` i prema `status` prikazuje stranicu.
6. Na `/register` frontend dohvaća šifrarnike i predlaže `name` iz `pendingIdentity`.
7. `POST /auth/registration`: `400` → greške uz polja; `409` → §7; `201` → korisnik je stvoren, sesija je `AUTHENTICATED` s novim ID-em sesije i novim CSRF tokenom. Frontend tada radi korake iz §7 "Nakon `201`".
8. Sljedeća prijava istim Google računom ide ravno na "postoji".
9. `/profile/edit`: `GET`/`PATCH /users/me` ([§3.1 API ugovora](api-endpoints.md#31-json-model-i-pravila-za-f-004)) i `PUT /users/me/photo`.
10. Odjava: `POST /auth/logout` → `204`. Backend poništava sesiju i briše cookie `JSESSIONID`. Frontend briše `['authSession']` i ide na `/`. Stari cookie daje `401`.

| Situacija | Što se događa | Poruka |
| --- | --- | --- |
| Odustajanje kod Googlea | `/login?error=cancelled`, sesija anonimna | "Prijava je otkazana." |
| Nevaljana prijava (krivi `state`, istekao kod) | `/login?error=failed` | "Prijava putem Googlea nije uspjela. Pokušaj ponovno." |
| Sesija istekne (obrazac ili profil otvoren) | `401` → `/login?error=expired` | "Sesija je istekla. Prijavi se ponovno." Unos se gubi. |
| "Odustani" na obrascu | `POST /auth/logout`, u bazi ništa | `/`, neprijavljen |
| Obrazac napušten | sesija istekne za 30 min, u bazi ništa | sljedeća prijava kreće ispočetka |
| Drugi Google račun, i s istom e-poštom | novi `sub` → `REGISTRATION_REQUIRED` | obrazac za registraciju |

## 2. Stanja sesije

| `status` | Kako nastaje | Smije | Ostale zaštićene rute |
| --- | --- | --- | --- |
| `ANONYMOUS` | nema sesije, istekla je ili odjava | `Public`, `GET /auth/session`, `POST /auth/logout`, Google prijava | `401 NOT_AUTHENTICATED` |
| `REGISTRATION_REQUIRED` | Google identitet provjeren, korisnik ne postoji | kao `ANONYMOUS` + `POST /auth/registration` | `403 REGISTRATION_REQUIRED` |
| `AUTHENTICATED` | postojeći korisnik ili završena registracija | prema oznakama API ugovora | `POST /auth/registration` → `409 ALREADY_REGISTERED` |

- **Prijedlog, provedba:** `AppOidcUserService` (delegira na `OidcUserService`) radi `SELECT u.id, u.app_role_id FROM oauth_accounts o JOIN users u ON u.id = o.user_id WHERE o.provider = 'google' AND o.provider_user_id = :sub` i vraća `AppOidcUser extends DefaultOidcUser` s poljem `Long userId`. Postoji → `ROLE_USER` (+ `ROLE_ADMIN`). Ne postoji → `userId = null`, `ROLE_REGISTRATION_PENDING`. Promjena uloge vrijedi od iduće prijave.
- **Prijava nakon `201`** (nakon commita): novi `OAuth2AuthenticationToken` s novim `AppOidcUser` i izvornim ovlastima (`OIDC_USER`, `SCOPE_*`, `FACTOR_AUTHORIZATION_CODE`), bez `ROLE_REGISTRATION_PENDING`, uz `ROLE_USER`. Zatim prazan `SecurityContext` sa `setAuthentication`, `holderStrategy.setContext(ctx)` i `repo.saveContext(ctx, req, res)`, gdje je `repo` zajednički bean `HttpSessionSecurityContextRepository` (isti u `http.securityContext(...)`). Na kraju `request.changeSessionId()` i `csrfRepo.saveToken(null, req, res)`, gdje je `csrfRepo` zajednički bean `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
- `CurrentUserArgumentResolver` čita `userId` iz principala i zadržava provjeru `SELECT EXISTS` s PR-a #51. Ako korisnika nema, poništava sesiju i vraća `401`. `X-User-Id` se uklanja.
- `AccessDeniedHandler`: `CsrfException` → `CSRF_INVALID`; inače `ROLE_REGISTRATION_PENDING` → `REGISTRATION_REQUIRED`; inače `FORBIDDEN`. `AuthenticationEntryPoint` → `401 NOT_AUTHENTICATED`. Oba pišu isti JSON kao `ApiExceptionHandler`, koji se seli iz paketa `event` u `common`.

## 3. Endpointi

Putanje osim prve dvije imaju prefiks `/api/v1`. Šifrarnici (`/sports`, `/cities`, `/skill-levels`) su u [§4 API ugovora](api-endpoints.md#4-sportovi-pomoćni-podaci-za-f-004-f-005-f-006-f-007-f-016-f-017).

| Metoda | Putanja | Tko | Zahtjev | Uspjeh | Greške |
| --- | --- | --- | --- | --- | --- |
| GET | `/oauth2/authorization/google` | Public | navigacija | `302` na Google | – |
| GET | `/login/oauth2/code/google` | Google | `code`, `state` ili `error` | `302` na `/` ili `/register` | `302` na `/login?error=cancelled` ili `failed` |
| GET | `/auth/session` **(Prijedlog)** | Public | – | `200` (§4) | – |
| POST | `/auth/registration` **(Prijedlog)** | `REGISTRATION_REQUIRED` | JSON (§4) | `201`, `Location: /api/v1/users/me`, `MyProfile` | `400`, `401`, `403`, `409` |
| POST | `/auth/logout` | svi | `X-XSRF-TOKEN` | `204` (i bez sesije) | `403` CSRF |
| GET, PATCH | `/users/me` | Authenticated | PATCH: promijenjena polja | `200` `MyProfile` | `400`, `401`, `403`, `409` |
| PUT | `/users/me/photo` | Authenticated | `multipart/form-data`, polje `file` | `200` `MyProfile` | `400`, `401`, `403`, `413` |
| GET | `/photos/{fileName}` **(Prijedlog)** | Public | – | `200` slika | `404` |

**Prijedlog (fotografija, #67):** `image/jpeg`, `image/png` ili `image/webp`, najviše 5 MB, vrstu provjerava backend (`400 errors.file`). Datoteke idu u `${APP_UPLOAD_DIR}` (u produkciji Docker volumen) pod imenom `<UUID>.<ekstenzija>`. Poslužuje ih javni `GET /api/v1/photos/{fileName}`. `profilePhotoUrl` je relativna putanja, npr. `/api/v1/photos/3f2a….jpg`. Pri zamjeni se stara datoteka briše. Ako slanje nakon `201` ne uspije, korisnik postoji i dodaje fotografiju na `/profile/edit`.

## 4. JSON

`frontend/src/types/user.ts` zamjenjuje se ovim tipovima: `camelCase`, `AppRole` samo `USER`/`ADMIN` (bez `GUEST` i `MODERATOR`), obavezni `gender`, `birthDate` i `cityId`, sportovi bez `id`.

```ts
type Gender = 'Male' | 'Female' | 'Other';   // sučelje: "Muško", "Žensko", "Drugo"
type AppRole = 'USER' | 'ADMIN';
interface UserSport { sportId: number; skillLevelId: number; }
interface MyProfile { id: number; name: string; username: string; profilePhotoUrl: string | null;
  gender: Gender; birthDate: string; age: number; cityId: number; appRoleId: AppRole; sports: UserSport[]; }
interface AuthSession { status: 'ANONYMOUS' | 'REGISTRATION_REQUIRED' | 'AUTHENTICATED';
  user: MyProfile | null; pendingIdentity: { name: string | null; email: string | null } | null; }
interface ApiError { message?: string; code?: string; errors?: Record<string, string>; }
```

`GET /auth/session`, stanje `AUTHENTICATED` (za `REGISTRATION_REQUIRED` je `user: null` i `pendingIdentity: {"name": "Ana Horvat", "email": "ana.horvat@gmail.com"}`, za `ANONYMOUS` su oba `null`):

```json
{"status": "AUTHENTICATED", "pendingIdentity": null,
 "user": {"id": 1, "name": "Ana Horvat", "username": "ana", "profilePhotoUrl": null, "gender": "Female",
          "birthDate": "2002-03-14", "age": 24, "cityId": 1, "appRoleId": "USER",
          "sports": [{"sportId": 1, "skillLevelId": 2}]}}
```

`POST /auth/registration`:

```json
{"name": "Ana Horvat", "username": "ana", "gender": "Female", "birthDate": "2002-03-14",
 "cityId": 1, "sports": [{"sportId": 1, "skillLevelId": 2}, {"sportId": 3, "skillLevelId": 1}]}
```

| Polje | Pravilo | Greška |
| --- | --- | --- |
| `name` | obavezno, nije prazno; **Prijedlog:** trim, najviše 100 znakova | `400 errors.name` |
| `username` | obavezno, jedinstveno (razlikuje velika i mala slova); **Prijedlog:** trim, 3–30 znakova, bez razmaka | `400 errors.username`, `409 USERNAME_TAKEN` |
| `gender` | `Male`, `Female` ili `Other` | `400 errors.gender` |
| `birthDate` | `YYYY-MM-DD`, od `1900-01-01` do danas (`Europe/Zagreb`) | `400 errors.birthDate` |
| `cityId` | postojeći grad | `400 errors.cityId` |
| `sports` | barem jedan, postojeći ID-evi, sport se ne ponavlja | `400 errors.sports` |
| ostala polja (`userId`, `appRoleId`, `sub`…) | nisu dopuštena (`fail-on-unknown-properties=true`) | `400` |

Isti validator vrijedi za `PATCH /users/me`, gdje su sva polja opcionalna. `sub` i `email` dolaze iz sesije. Greške imaju oblik iz `ApiExceptionHandler` uz opcionalni `code` (**Prijedlog**), npr. `{"message": "Korisničko ime je zauzeto.", "code": "USERNAME_TAKEN", "errors": {"username": "…"}}`. **Prijedlog:** Googleova slika se ne sprema kao `profilePhotoUrl` (NF-03).

## 5. Statusni kodovi

| Status | `code` | Kada |
| --- | --- | --- |
| `200`, `201`, `204` | – | dohvat i izmjena, registracija, odjava (uvijek, i bez sesije) |
| `400` | (`VALIDATION_FAILED`) | neispravan JSON ili polje, nepostojeći ID, ponovljen sport, kriva vrsta datoteke; uvijek s `errors` |
| `401` | `NOT_AUTHENTICATED` | nema sesije ili je istekla |
| `403` | `REGISTRATION_REQUIRED` | stanje `REGISTRATION_REQUIRED` na ruti za prijavljene |
| `403` | `CSRF_INVALID` | `POST`/`PUT`/`PATCH`/`DELETE` bez valjanog `X-XSRF-TOKEN` |
| `403` | `FORBIDDEN` | prijavljen, ali bez ovlasti (`Own`, `Admin`) |
| `409` | `USERNAME_TAKEN` | `username` je zauzet |
| `409` | `ALREADY_REGISTERED` | `sub` je već povezan (dvostruko slanje, drugi preglednik, poziv iz `AUTHENTICATED`) |
| `413` | `FILE_TOO_LARGE` | fotografija veća od 5 MB |

**Baza nikad ne vraća `500`.** Backend prvo sam provjerava pravila. Ako se ograničenje ipak prekrši (istodobni zahtjevi), handler za `DataIntegrityViolationException` prema nazivu ograničenja vraća: `users_username_key` → `409 USERNAME_TAKEN`; `oauth_accounts_provider_provider_user_id_key` i `oauth_accounts_user_provider_key` (V6) → `409 ALREADY_REGISTERED`; `user_sports_user_id_sport_id_key` → `400 errors.sports`; ostali `CHECK`, `FK` i `NOT NULL` → `400` uz odgovarajuće polje.

## 6. Sesija, CSRF i proxy

- **Cookie** `JSESSIONID`: `HttpOnly`, `SameSite=Lax` (mora se poslati pri povratku s Googlea), `Secure` u profilu `prod`. Ništa u `localStorage`. Istek nakon 30 min neaktivnosti. Novi ID sesije nakon Google prijave (Spring) i nakon registracije (ručno, §2).
- **Odjava** poništava sesiju, `SecurityContext` i CSRF token, briše cookie `JSESSIONID` i vraća `204`.
- **CSRF (Prijedlog):** `csrf(c -> c.spa().csrfTokenRepository(csrfRepo))`, uz zajednički bean `csrfRepo`. Frontend čita cookie `XSRF-TOKEN` i šalje ga u `X-XSRF-TOKEN` uz `POST`, `PUT`, `PATCH` i `DELETE`. Handler za `GET /auth/session` prima `CsrfToken csrfToken` i poziva `csrfToken.getToken()`, pa je cookie uvijek postavljen.
- **Isti origin (Prijedlog):** u razvoju Vite (`:5173`) prosljeđuje backend rute na `:8080`, bez CORS-a. U produkciji backend poslužuje SPA ili su oba iza istog proxyja.
- **`/login` je stranica SPA-a**, zato `.loginPage("/login")`. Bez toga Spring na `/login?error=…` poslužuje svoju generiranu stranicu umjesto React stranice (vidi se tek u produkciji). API i dalje vraća `401` JSON, ne `302`, zbog vlastitog entry pointa.

```ts
// frontend/vite.config.ts; bez changeOrigin: backend mora vidjeti Host localhost:5173
server: { proxy: { '/api': 'http://localhost:8080', '/oauth2': 'http://localhost:8080',
                   '/login/oauth2': 'http://localhost:8080' } },   // /login ostaje frontend
```

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=${APP_BASE_URL:http://localhost:5173}/login/oauth2/code/google
server.servlet.session.timeout=30m
server.servlet.session.cookie.same-site=lax
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
server.tomcat.max-swallow-size=10MB
app.upload-dir=${APP_UPLOAD_DIR:./uploads}
# prod: server.servlet.session.cookie.secure=true, server.forward-headers-strategy=framework
```

`max-swallow-size` je potreban jer bi inače Tomcat prekinuo vezu i preglednik ne bi dobio `413`. `MaxUploadSizeExceededException` se u `ApiExceptionHandler` preslikava u `413 FILE_TOO_LARGE`.

**Skica i matrica zaštite (Spring Security 7, #54, #61, #71):**

```java
http
  .authorizeHttpRequests(a -> a
      .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/*", "/api/v1/events/*/comments",
          "/api/v1/venues", "/api/v1/venues/*", "/api/v1/sports", "/api/v1/sports/*", "/api/v1/cities",
          "/api/v1/skill-levels", "/api/v1/photos/*", "/api/v1/auth/session").permitAll()
      .requestMatchers(HttpMethod.POST, "/api/v1/auth/registration").authenticated() // stanje provjerava kontroler
      .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
      .requestMatchers("/api/v1/**").hasRole("USER")        // Own, Organizer… provjerava servis
      .anyRequest().permitAll())                            // /oauth2/**, /login/oauth2/**, /error, SPA
  .oauth2Login(o -> o
      .loginPage("/login")
      .userInfoEndpoint(u -> u.oidcUserService(appOidcUserService))
      .successHandler(successHandler)                       // userId != null ? "/" : "/register"
      .failureHandler(failureHandler))                      // access_denied ? "?error=cancelled" : "?error=failed"
  .logout(l -> l
      .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/logout"))
      .deleteCookies("JSESSIONID")
      .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
  .securityContext(c -> c.securityContextRepository(securityContextRepository)) // zajednički bean (§2)
  .requestCache(c -> c.requestCache(new NullRequestCache()))   // anonimni 401 ne stvara sesiju
  .csrf(c -> c.spa().csrfTokenRepository(csrfRepo))
  .exceptionHandling(e -> e.authenticationEntryPoint(jsonEntryPoint).accessDeniedHandler(jsonAccessDeniedHandler))
  .httpBasic(b -> b.disable()).formLogin(f -> f.disable());
```

## 7. Frontend pravila

- Stanje prijave je jedan TanStack Query, `['authSession']`. Omotač u `api_v1.ts` šalje `X-XSRF-TOKEN`, čita `ApiError` i baca grešku sa `status` i `code`. `USE_DUMMY_DATA` se isključuje za ove rute.
- **Nakon `201`:** `await queryClient.invalidateQueries({ queryKey: ['authSession'] })`, tek onda fotografija i navigacija na `/`. Inače zaštita ruta vidi stari `REGISTRATION_REQUIRED`, a fotografija ide sa starim CSRF tokenom.
- `401` → očisti `['authSession']`; ako je bio `AUTHENTICATED` ili `REGISTRATION_REQUIRED`, idi na `/login?error=expired`, inače na `/login`.
- `403 REGISTRATION_REQUIRED` → `/register`. `403 CSRF_INVALID` → ponovno dohvati `/auth/session` i zamoli korisnika da ponovi radnju. `409 ALREADY_REGISTERED` → ponovno dohvati `/auth/session`. `AUTHENTICATED` → `/`. Inače poruka "Račun je već stvoren. Prijavi se ponovno.", `POST /auth/logout` i `/login`.
- **Prijedlog, zaštita ruta (#71):** javne su `/`, `/events/:eventId`, `/legal/*` i `/login`. Privatne su `/events`, `/reports` i `/profile/edit` (`ANONYMOUS` → `/login`, `REGISTRATION_REQUIRED` → `/register`). Nova ruta `/register` vrijedi samo u `REGISTRATION_REQUIRED`, inače `/login` ili `/`. `AUTHENTICATED` na `/login` → `/`.
- **Navigacijska traka:** `ANONYMOUS` → "PRIJAVA"; `REGISTRATION_REQUIRED` → "Dovrši registraciju" i "Odustani" (odjava); `AUTHENTICATED` → korisničko ime i fotografija.
- **Fotografija:** provjera vrste i veličine prije slanja, `FormData` s poljem `file` i **bez** zaglavlja `Content-Type` (sadašnji `"Multipart: file"` u `api_v1.ts` se uklanja).

## 8. Postavljanje za razvoj

1. Kos u Google Cloud Consoleu postavlja consent screen (External, status **Testing**) i dodaje e-pošte svih šest članova kao test korisnike, te stvara Web OAuth klijent s redirect URI-jem `http://localhost:5173/login/oauth2/code/google`.
2. Client ID i secret dijeli privatno, nikad u gitu ni u issueima. Svatko ih postavlja kao `GOOGLE_CLIENT_ID` i `GOOGLE_CLIENT_SECRET` u run konfiguraciji i otvara aplikaciju samo na `:5173`.
3. U `pom.xml` se otkomentira `spring-boot-starter-security-oauth2-client` (i `-test`).
4. `src/test/resources/application.properties` dobiva lažne vrijednosti (`client-id=test`, `client-secret=test`), da se `contextLoads` pokrene bez pravih tajni.

## 9. Što radi tko

Redoslijed: #69 → #70 → #54 → #55, #58 → #56, #61, #63, #64 → frontend → #71 → testovi → #72.

| Zadatak | Dio ugovora |
| --- | --- |
| #54 – #56 REG (backend) | §6 skica i postavke, §8, handleri (#54); `AppOidcUserService` i `/auth/session` (#55); registracija, transakcija, prijava nakon `201`, §5 (#56) |
| #57, #65 REG (frontend) | `/login` i `?error=…`, `/register` s pravilima iz §4, "Odustani", §7 nakon `201` i `409` |
| #58, #59 LOGIN | uloge i `CurrentUserArgumentResolver` (§2); `useAuthSession`, navigacijska traka, `api_v1.ts`, Vite proxy |
| #60 LOGIN-03 | (a) `AppOidcUserService.loadUser` s mockanim delegatom i testnom bazom: postojeći, novi, ista e-pošta s drugim `sub`. (b) MockMvc s `oidcLogin().oidcUser(new AppOidcUser(...))`, s ovlastima zadanima u konstruktoru `AppOidcUser`, za `/auth/session` i `401`/`403`/`409` (sam `oidcLogin()` ne poziva servis; `.authorities(...)` nakon `.oidcUser(...)` zamijeni principal zadanim, pa ga ne koristiti). (c) Failure handler: nakon `GET /oauth2/authorization/google` u istoj sesiji `error=access_denied` → `?error=cancelled`, krivi `state` → `?error=failed`, sesija anonimna. |
| #61, #62 LOGOUT | `logout` iz §6, stari cookie → `401`; gumb s CSRF zaglavljem, čišćenje cachea |
| #63, #64, #66 PROFILE | `/api/v1/users/me` (ne `/api`), `409 USERNAME_TAKEN`, šifrarnici iz API ugovora, `PATCH` samo promijenjenih polja |
| #67 PROFILE-05 | fotografija, pohrana i `413` (§3, §6, §7) |
| #68 PROFILE-06 | pravila iz §4 i §5 (1900-01-01, budući datum, duplikat sporta, zauzet username), `401`, `403` u registraciji i bez CSRF-a |
| #69 SHARED-01 | gotovo nakon spajanja PR-a #73 (V6); mora biti prije #56 |
| #71, #72 SHARED | skica iz §6, handleri iz §2, zaštita ruta iz §7; E2E scenariji iz §1 |

## 10. Otvorena pitanja

- Postoji li minimalna dob? Zasad vrijedi samo raspon od 1900. do danas.
- Potvrditi trim i ograničenja duljine za `name` i `username`.
- Produkcija: isti origin je pretpostavka. Odvojene domene traže CORS s `allowCredentials` i `SameSite=None; Secure`.
- Redoslijed spajanja PR-ova #51, #73 i #74.
- Učitavaju li se ovlasti iz `app_role_has_permissions` kao Spring authorities, ili je dovoljna uloga `ADMIN`?
