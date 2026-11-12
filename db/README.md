# Baza podataka - Let's Do Sports

Ovaj direktorij sadrži konfiguraciju za pokretanje PostgreSQL baze pomoću Docker Composea. PostgreSQL nije potrebno zasebno instalirati: pokreće se u Docker containeru.

## 1. Što se pokreće?

Datoteka `compose.yml` definira dva servisa:

| Servis    | Tehnologija   | Uloga                                       |
| --------- | ------------- | ------------------------------------------- |
| `db`      | PostgreSQL 18 | Baza podataka aplikacije; ostaje pokrenuta. |
| `migrate` | Flyway 13     | Izvršava nove SQL migracije i završava rad. |

Docker Compose također stvara mrežu `backend` za komunikaciju servisa i volume `postgres_data` za trajnu pohranu podataka.
Stvarni Docker nazivi imaju prefiks projekta, npr. `db_backend` i `db_postgres_data` ako je naziv projekta `db`.

Pri pokretanju se prvo pokreće `db`. Nakon uspješne provjere spremnosti (*healthcheck*) pokreće se `migrate`, koji primjenjuje nove migracije iz `migrations/`.

**Napomena:** ova konfiguracija ne pokreće Spring Boot backend ni React frontend.

## 2. Preduvjeti

- Instaliran i pokrenut [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/) (Windows) ili Docker Engine s Docker Composeom (Linux).
- Kloniran Git repozitorij projekta.
- Slobodan lokalni port `5432`.

Provjera instalacije:

```bash
docker --version
docker compose version
```

Sve sljedeće naredbe izvršavati iz direktorija `db/`, u kojem se nalazi `compose.yml`.
Na Windowsima s Docker Desktopom nije potreban `sudo`; na Linuxu može biti potreban.

## 3. Quick start

Prije prvog pokretanja kopirati `.env.example` u `.env`:

```bash
# Linux / macOS
cp .env.example .env
```

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```

U `.env` promijeniti vrijednosti `REPLACE_WITH_ADMIN_PASSWORD` i `REPLACE_WITH_APP_PASSWORD` u vlastite lozinke.
Zadržati korisnička imena `lds_admin` i `lds_app`: migracija `V1__app_user_grants.sql` izravno ih koristi pa sama promjena u `.env` nije dovoljna.
Datoteka `.env` s lozinkama **ne smije se commitati u Git**.

**Napomena:** promjena lozinki ili naziva baze u `.env` ne mijenja već inicijaliziranu bazu. Postojeće lozinke mijenjaju se SQL naredbom `ALTER ROLE`; nove vrijednosti treba uskladiti s `.env` i postavkama aplikacije.

Pokrenuti bazu i migracije:

```bash
docker compose up -d
```

Parametar `-d` pokreće containere u pozadini. Bez njega prikazuju se logovi u terminalu.
Provjeriti status i migracije:

```bash
docker compose ps -a
docker compose logs migrate
```

Očekivano: `db` je `Up (healthy)`, a `migrate` je uspješno završio (`Exited (0)`).
Ako se pojavi pogreška, pogledati i `docker compose logs db`.

Alternativno, ako koristite Podman s podrškom za Compose:

```bash
podman compose up -d
```

## 4. Povezivanje na bazu

Bazi se s lokalnog računala (npr. iz Spring Boota) pristupa na:

```text
Host:     localhost
Port:     5432
Database: lets_do_sports
User:     lds_app
Password: vrijednost APP_DB_PASSWORD iz .env
```

Navedeni naziv baze i korisnik preuzeti su iz `.env.example`. Primjer JDBC URL-a:

```text
jdbc:postgresql://localhost:5432/lets_do_sports
```

Iz drugog containera na mreži `backend` koristi se hostname `db`, a ne `localhost`.

Korisnik `lds_admin` namijenjen je administraciji i Flyway migracijama, dok skripta `init-db/01-create-appuser-role.sh` pri prvoj inicijalizaciji stvara korisnika `lds_app` za aplikaciju bez administratorskih ovlasti.

## 5. Gašenje baze

```bash
docker compose down
```

Ili s Podmanom:

```bash
podman compose down
```

Ovo uklanja containere i mrežu, ali **čuva podatke** u volumeu `postgres_data`.

## 6. Brisanje baze

```bash
docker compose down -v
```

Ili s Podmanom:

```bash
podman compose down -v
```

> [!WARNING]
> Ova naredba briše Docker volume i **sve podatke u lokalnoj bazi**.
> Nakon ponovnog pokretanja obnovit će se samo struktura i podaci definirani inicijalizacijskim skriptama i migracijama.

## 7. Migracije i inicijalizacija

- `init-db/` sadrži skripte koje PostgreSQL izvršava **samo pri prvom stvaranju baze**.
  Primjer je skripta koja kreira aplikacijskog korisnika.
- `migrations/` sadrži SQL migracije koje obrađuje Flyway. Primjer naziva:
  `V1__app_user_grants.sql`, `V2__events_schema.sql`.
- Flyway u bazi pamti izvršene verzije. Nove migracije primjenjuje pri sljedećem pokretanju servisa `migrate`.
- **Ne mijenjati već primijenjene verzionirane migracije (`V...`).** Umjesto toga dodati novu SQL datoteku s novom verzijom. Izmjena primijenjene migracije uzrokuje pogrešku validacije zbog nepodudaranja kontrolnog zbroja (*checksum*).

Za ručno pokretanje migracija dok je baza dostupna:

```bash
docker compose run --rm migrate
```

> [!WARNING]
> Brisanje i ponovno stvaranje baze opcija je samo za lokalni razvoj kada podaci nisu potrebni ([naredba](#6-brisanje-baze)). Za promjene postojeće baze dodati novu migraciju.

### Ogledni podaci

Skripta `seed/dev_seed.sql` sadrži ogledne podatke za lokalni razvoj. Nije dio Flyway migracija i pokreće se ručno, nakon migracija, kao aplikacijski korisnik `lds_app`.
**Prije unosa briše sve postojeće retke iz tablica `event_members`, `events`, `venues` i `users`**, a ne samo prethodne ogledne podatke. Ne koristiti je na produkcijskoj bazi.

## 8. Produkcija

Ovo je lokalna razvojna konfiguracija; `docker compose up -d` pokreće i bazu i migracije. Za buduću produkcijsku konfiguraciju preporučuje se odvojeno pokretanje baze i migracija, nakon provjere i u okviru postupka objave nove verzije:

```bash
docker compose up -d db
docker compose run --rm migrate
```

Za produkciju je potrebno dodatno urediti sigurnost, pristupne podatke, ovlasti korisnika i sigurnosne kopije. Lokalnu `.env` datoteku ne koristiti kao način dijeljenja produkcijskih tajni.
