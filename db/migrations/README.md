# SQL migracije

Ovaj direktorij sadrži SQL migracije za PostgreSQL koje izvršava Flyway kroz servis `migrate` iz [`db/compose.yml`](../compose.yml). Upute za pripremu okruženja i prvo pokretanje nalaze se u [README-u baze](../README.md).

## Imenovanje datoteka

| Vrsta | Format | Primjer | Namjena |
| --- | --- | --- | --- |
| Verzionirana | `V<verzija>__<opis>.sql` | `V2__events_schema.sql` | Promjene tablica, indeksa, ovlasti i šifrarnika. |
| Ponovljiva | `R__<opis>.sql` | `R__reporting_views.sql` | Definicije pogleda i funkcija koje se mogu ponovno primijeniti. |

- Između verzije i opisa, odnosno prefiksa `R` i opisa, obavezne su **dvije donje crte** (`__`).
- Koristiti kratke engleske opise u obliku `snake_case` i datoteke spremati kao UTF-8.
- Za nove verzionirane migracije nastaviti niz cijelih brojeva (`V1`, `V2`, `V3`, ...). Svaka verzija mora biti jedinstvena; pri paralelnom radu uskladiti broj s timom prije spajanja promjena.
- Primjer `R__reporting_views.sql` prikazuje format; ne označava postojeću datoteku.

## Pravila izvršavanja

Flyway vodi evidenciju izvršenih migracija i njihovih kontrolnih zbrojeva (*checksum*) u tablici `flyway_schema_history`.

- **Verzionirane migracije** izvršavaju se jednom, redoslijedom verzija. Nakon što se migracija primijeni na zajedničku bazu ili spoji u zajedničku granu, ne mijenjati je, preimenovati ni brisati. Za ispravak dodati novu verzioniranu migraciju.
- Izmjena već primijenjene verzionirane migracije uzrokuje pogrešku validacije zbog nepodudaranja kontrolnog zbroja. Vratiti izvornu datoteku i promjenu zapisati u novu migraciju.
- **Ponovljive migracije** izvršavaju se prvi put te ponovno kada im se promijeni kontrolni zbroj. Izvršavaju se nakon svih novih verzioniranih migracija, abecednim redom opisa.
- Ponovljivu migraciju napisati tako da se može sigurno izvršiti više puta, npr. pomoću `CREATE OR REPLACE VIEW` gdje je primjenjivo.

Migracije se izvršavaju pri pokretanju servisa `migrate`. Sam restart PostgreSQL servisa `db` ne izvršava migracije.

## Dodavanje i provjera migracije

1. Provjeriti postojeće datoteke i odabrati sljedeću slobodnu verziju.
2. Dodati SQL datoteku s jednom smislenom cjelinom promjena. Kratkim komentarom objasniti neuobičajene odluke ili ograničenja.
3. Provjeriti migraciju na lokalnoj bazi s prethodno primijenjenim verzijama te provjeriti očekivanu strukturu i podatke.
4. Prije spajanja provjeriti i izvođenje svih migracija na zasebnoj praznoj razvojnoj bazi.

Sljedeće naredbe izvršavati iz direktorija **`db/`**, nakon pripreme `.env` prema glavnom README-u:

```bash
docker compose up -d db
docker compose run --rm migrate info
docker compose run --rm migrate validate
docker compose run --rm migrate
```

- `info` prikazuje primijenjene migracije i one koje čekaju izvršavanje.
- `validate` provjerava usklađenost SQL datoteka s poviješću migracija; ne provjerava može li se novi SQL uspješno izvršiti.
- Posljednja naredba izvršava migracije i ispisuje rezultat u terminal.

## Što ne pripada ovom direktoriju

- [`init-db/`](../init-db/) sadrži skripte za prvu inicijalizaciju PostgreSQL pohrane, primjerice stvaranje aplikacijskog korisnika.
- [`seed/dev_seed.sql`](../seed/dev_seed.sql) sadrži ogledne podatke koji se unose ručno. Skripta briše postojeće podatke iz aplikacijskih tablica i namijenjena je samo lokalnom razvoju.
- Lozinke i druge tajne ne zapisivati u migracije.

## Službena dokumentacija

- [Verzionirane migracije](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations)
- [Ponovljive migracije](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/repeatable-migrations)
- [Dodatna dokumentacija](https://kgs-software.atlassian.net/wiki/spaces/DOCUEN/pages/2774302818/Flyway)
