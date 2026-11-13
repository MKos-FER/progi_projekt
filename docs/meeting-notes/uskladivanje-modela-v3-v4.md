---
date_created: 2026-11-13
---

# Usklađivanje modela podataka prema V3

## Ažurirani ER dijagram

Izrađen je [ERD v6](../assets/diagrams/Let's%20Do%20Sports%20-%20ERDv6.svg) i njegov [PlantUML izvor](../puml/Let's%20Do%20Sports%20-%20ERDv6.puml), prema stvarnoj strukturi baze nakon migracija V1–V3. Prethodni ERD v5 sačuvan je bez izmjena.

Dijagram prikazuje implementirane aplikacijske tablice i pogled `events_with_status`. Usklađeni su naziv `is_paid`, opcionalnost grada korisnika te oznake obaveznih polja i važnih ograničenja.

## Veze i ključevi

Provjereni su primarni i strani ključevi te veze između korisnika, događaja, sudionika, terena, sportova i šifrarnika. ERD v6 odgovara provjerenoj strukturi V3, uključujući obaveznost i opcionalnost veza.

Ispravno su prikazani složeni primarni ključ `(venue_id, sport_id)` u `venue_sports` te jedinstvena ograničenja `(event_id, user_id)` u `event_members` i `(user_id, sport_id)` u `user_sports`.

## Ograničenja i poslovna pravila

U dijagram su dodane bilješke o kapacitetu, dobnim granicama, trajanju, koordinatama i poveznici za rezervaciju. Pogled `events_with_status` odgovara bazi: računa broj sudionika i statuse događaja.

Pravilo da je organizator prvi sudionik označeno je kao poslovno pravilo koje se provodi transakcijskim upisom događaja i članstva. Sam strani ključ ne jamči taj upis.

Pregled i usklađivanje ERD-a nisu mijenjali aplikacijski kod ni postojeće SQL migracije.
