# Profilna fotografija (PROFILE-05, #67)

Fotografija je opcionalna. Korisnik može postaviti ili zamijeniti JPEG/PNG fotografiju; bez fotografije prikazuje se zamjenska ikona. Nakon uspješnog spremanja frontend ponovno koristi URL iz baze, pa fotografija ostaje dostupna nakon osvježavanja i restarta backenda.

## Pohrana i provjere

- Baza koristi postojeći `users.profile_photo_url` iz V4; nova migracija nije potrebna.
- Datoteke se spremaju u `${user.home}/.lets-do-sports/profile-photos`. Putanju je moguće promijeniti varijablom `PROFILE_PHOTO_DIRECTORY`.
- Dopušteni su stvarni JPEG i PNG, do 5 MiB i 4096 × 4096 piksela. Backend provjerava sadržaj, dimenzije i MIME tip, zatim ponovno kodira sliku bez izvornih metapodataka. I obrađena datoteka mora stati u 5 MiB.
- Naziv datoteke je nasumični UUID; naziv koji je poslao korisnik ne koristi se za putanju. SVG/GIF i lažno označene datoteke odbijaju se.
- Svaka zamjena dobiva novi URL. Promjena URL-a radi se u transakciji uz zaključavanje korisnika. Stara datoteka briše se nakon commita; ako baza odbije spremanje, nova se uklanja, a stara ostaje.
- Neuspjelo naknadno čišćenje bilježi se u log. Nagli prekid procesa između zapisa datoteke i commita može ostaviti nepovezanu datoteku; ne može se osigurati zajednička atomska transakcija PostgreSQL-a i datotečnog sustava.

Za restart na istom računalu direktorij ostaje sačuvan. Kod deploya u kontejner obavezno montirati **trajni disk/volume** na konfiguriranu putanju; disk mora biti upisiv i backupirati se zajedno s bazom. Rješenje je za jednu instancu backenda; više instanci trebaju zajedničku pohranu. Fotografije ne pripadaju Git repozitoriju.

## API

| Metoda i putanja | Rezultat |
| --- | --- |
| `GET /api/v1/users/me/photo` | `{profilePhotoUrl, csrfToken, csrfHeaderName}`; URL je `null` ako fotografije nema. Odgovor nije cacheiran. |
| `PUT /api/v1/users/me/photo` | `multipart/form-data`, dio `file`; isti oblik odgovora s novim URL-om. Koristi trenutačnog korisnika preko zajedničkog `@CurrentUserId`. |
| `GET /api/v1/profile-photos/{name}` | JPEG/PNG sadržaj s eksplicitnim MIME tipom i `nosniff`; nepostojeća/nevaljana putanja vraća 404. Primjenjuju se postojeći sigurnosni filtri aplikacije. |

Prazna, oštećena, nepodržana slika ili nepodudaranje MIME tipa vraćaju 400; prevelika datoteka vraća 413. Datotečna pohrana koja nije dostupna vraća 503. Neuspješan upload ne mijenja prethodnu fotografiju.

Frontend šalje CSRF zaglavlje iz GET odgovora pri PUT zahtjevu i zadržava postojeću fotografiju ako spremanje ne uspije. URL u `profile_photo_url` mogu koristiti i budući odgovori punog korisničkog profila. Endpointi fotografije ne zamjenjuju cijeli profilni API.

## Lokalno pokretanje i identitet korisnika

Na ovoj osnovnoj grani `CurrentUserArgumentResolver` još koristi **razvojni `X-User-Id`**, a aplikacija ima zadanu Spring Security prijavu. Fotografija koristi taj zajednički resolver; ovaj issue ne uvodi OAuth niti pretvara razvojno zaglavlje u produkcijsku autentifikaciju. Prije produkcije zajednički resolver mora dohvaćati interni ID iz provjerene prijave. Čitanje slika i upload podliježu istim sigurnosnim pravilima aplikacije.

1. Pokrenuti lokalnu bazu i izvršiti postojeće migracije. Backend povezati preko `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` i `SPRING_DATASOURCE_PASSWORD` (lozinke ostaju lokalno).
2. Za razvojni prikaz postaviti `VITE_PROFILE_DEV_USER_ID` u lokalnom `frontend/.env.local` na ID postojećeg vlastitog testnog korisnika. Zaglavlje se šalje samo u Vite development modu. Nikada ga ne koristiti kao produkcijsku autorizaciju.
3. Pokrenuti backend na portu 8080 i prijaviti se u njegovu postojeću razvojnu Spring Security sesiju na `http://localhost:8080/login`. Zadani korisnik je `user`, a generirana lozinka ispisuje se pri pokretanju. Koristiti isključivo lokalno; ne commitati lozinke.
4. U `frontend` izvršiti `npm install` i `npm run dev` (ili postojeći Bun workflow). Vite prosljeđuje `/api` backendu na portu 8080. Koristiti `localhost` za oba servisa radi sesijskog cookieja.
5. U postojećoj početnoj stranici nalazi se odjeljak profilne fotografije. Nakon spajanja stvarnog profilnog ekrana komponenta `src/profile/ProfilePhoto.tsx` može se smjestiti u njega. U produkciji frontend i `/api` moraju biti dostupni kroz isti origin/reverse proxy.

Bez uspješne prijave/identiteta kontrola je onemogućena uz poruku; odsutnost fotografije sama po sebi nije greška profila. Nema obaveznog uploada ni automatske promjene ostalih polja profila.

## Provjera

Iz `backend/LetsDoSports`, uz projektni JDK 23:

```powershell
.\mvnw.cmd '-Dtest=PhotoStorageTest,ProfilePhotoServiceTest,ProfilePhotoControllerTest' test
```

Na računalu s JDK-om 21 isti izvori provjereni su uz dodatni argument `'-Djava.version=21'`; projektni `pom.xml` nije mijenjan.

Testovi provjeravaju validnu/krivu sliku, dimenzije, veličinu, putanje, trajnost pohrane, neuspješan commit, zamjenu, opcionalni URL, autentifikaciju i CSRF. Integracijski `ProfilePhotoPersistenceIT` pokreće pravi HTTP server dvaput, koristeći isti direktorij i PostgreSQL, te provjerava upload, dohvat nakon restarta, odbijanje lažne slike i zamjenu s uklanjanjem starog URL-a.

Integracijski test koristiti **samo na zasebnoj testnoj bazi** s migracijama do V5, uz lokalno zadane `PROFILE_TEST_JDBC_URL`, `PROFILE_TEST_DB_USER` i `PROFILE_TEST_DB_PASSWORD`:

```powershell
.\mvnw.cmd -Pintegration verify
```

Bez `PROFILE_TEST_JDBC_URL` taj test se preskače. Ne upisivati testne pristupne podatke u repozitorij. Za standardni kontekstni test aplikacije također je potrebna ispravna konfiguracija datasourcea.

Frontend provjere: `npm run prod` i `npm run lint`. Ručna prihvatna provjera: profil bez fotografije → upload → refresh → restart backenda → dohvat iste fotografije → zamjena → odbijanje nevaljane/prevelike datoteke uz očuvanje stare fotografije.

### Rezultat provjere 15. 11. 2026.

- `mvnw.cmd '-Djava.version=21' -Pintegration verify`: **68 standardnih testova i 1 integracijski test, bez grešaka i preskakanja**. Izvršeno s JDK-om 21 i PostgreSQL-om 18.6.
- Zasebna testna baza `profile_test` u Compose projektu `progi-profile67-check` uspješno je migrirana od V1 do V5. Razvojna baza nije korištena.
- Stvarnim HTTP zahtjevima uz uključene sigurnosne filtre potvrđeni su opcionalni URL, upload, ista fotografija nakon restarta cijelog servera, odbijanje lažne slike bez promjene URL-a te zamjena i 404 za uklonjenu staru fotografiju.
- `npm run prod` i `npm run lint`: prolaze. Automatizirana provjera obuhvaća HTTP/backend i frontend build/lint; ručni klikovi kroz preglednik nisu dio tog testa.
