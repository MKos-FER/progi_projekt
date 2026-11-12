# Specifikacija događaja – Let’s Do Sports


Izvor strukture podataka: ER dijagram (ERDv3), tablice `events`, `eventMembers`, `sports`, `venues`, `venueSports`.


## 1. Podaci koje događaj mora sadržavati

Pri kreiranju događaja obavezno je unijeti:

| Polje | Tip |
|---|---|
| `name` | string |
| `sportId` | FK → `sports.id` |
| `venueId` | FK → `venues.id` |
| `scheduledAt` | datetime |
| `minimumAgeRequirement` | int |




## 2. Osnovna validacija


#### Name
 Nakon uklanjanja razmaka (trim) ne smije biti prazan i mora imati najviše 40 znakova. 

#### spotId
Sport mora postojati.

#### venueId
 Venue mora postojati i mora podržavati odabrani sport.

#### scheduledAt
 Mora biti sada ili ubudućnosti (scheduledAt >= trenutno vrijeme). 

#### minimumAgeRequirement
Cijeli broj, mora biti barem minimalne dobi aplikacije (13). Mora biti manje od maximumAgeRequirement.

Validacija bi se terbala provoditi na klijentu(zbog UX-a) i na poslužitelji(zbog sigurnosti)

### 2.1 SQL provjere

**Postoji li sport**

```sql
SELECT 1
FROM sports
WHERE id = ?;
```

Ova provjera nije potrebna ako korisniku na formi ponudimo listu postojećih sportova, odnosno ako venue filtriramo po odabranom sportu.

**Postoji li venue**

```sql
SELECT 1
FROM venues
WHERE id = ?;
```

**Podržava li venue odabrani sport**

```sql
SELECT COUNT(*)
FROM venueSports
WHERE venueId = ?
  AND sportId = ?;
```


### 2.2 Pseudokod validacije

```js
function validateCreateEvent(input, now = new Date()) {
  const errors = {};

  const name = (input.name ?? "").trim();
  if (name.length === 0 || name.length > 60)
    errors.name = "Naziv mora imati najviše 60 znakova.";

  if (!Number.isInteger(input.sportId) || !sportExists(input.sportId))
    errors.sportId = "Sport ne postoji.";

  if (!Number.isInteger(input.venueId) || !venueExists(input.venueId))
    errors.venueId = "Venue ne postoji.";
  else if (!errors.sportId && !venueSupportsSport(input.venueId, input.sportId))
    errors.venueId = "Venue ne podržava odabrani sport.";

  const at = new Date(input.scheduledAt);
  if (isNaN(at) || at <= now)
    errors.scheduledAt = "Termin mora biti u budućnosti.";

  const minAge = input.minimumAgeRequirement ?? 0;
  const maxAge = input.maximumAgeRequirement ?? 999;
  if (!Number.isInteger(minAge) || minAge < MIN_APP_AGE)
    errors.minimumAgeRequirement = "Dob ne smije biti ispod minimalne dobi aplikacije.";
  else if (minAge > maxAge)
    errors.minimumAgeRequirement = "Minimalna dob ne smije biti veća od maksimalne.";

  return errors; // prazan objekt = valjano
}
```


## 3. Kapacitet događaja i broj sudionika

### 3.1 Kapacitet

- `neededPlayers` je nullable.
- `NULL` znači da je broj sudionika neograničen i to je default.
- Ako vrijednost nije `NULL`, mora biti veća od 0.

### 3.2 Broj sudionika

Broj sudionika ne pohranjujemo kao zasebno polje, nego ga računamo iz tablice `eventMembers`:

```sql
SELECT COUNT(*)
FROM eventMembers
WHERE eventId = ?;
```

Napomene:
- `UNIQUE(eventId, userId)` na tablici `eventMembers` onemogućuje dvostruku prijavu istog korisnika.

### 3.3 Provjera kapaciteta pri ulasku u događaj

```sql
SELECT e.neededPlayers,
       COUNT(eM.id) AS currentMembers,
       (e.neededPlayers IS NOT NULL
        AND COUNT(eM.id) >= e.neededPlayers) AS isFull
FROM events e
LEFT JOIN eventMembers eM ON eM.eventId = e.id
WHERE e.id = ?
GROUP BY e.id, e.neededPlayers;
```

- Uvjet `neededPlayers IS NOT NULL` je potreban jer bi usporedba `COUNT(...) >= NULL` vratila `NULL`, a ne `false`. Događaj bez ograničenja time nikad nije popunjen.

### 3.4 Sprječavanje prekoračenja kapaciteta

Provjeru kapaciteta i bi trebali izvesti u jednoj transakciji tj jednim upitom. Inače dva korisnika koja se prijave u isto vrijeme mogu oba proći provjeru i prekoračiti kapacitet:

```sql
INSERT INTO eventMembers (eventId, userId, joinedAt)
SELECT ?, ?, NOW()
WHERE (SELECT neededPlayers FROM events WHERE id = ?) IS NULL
   OR (SELECT COUNT(*) FROM eventMembers WHERE eventId = ?)
      < (SELECT neededPlayers FROM events WHERE id = ?);
-- 0 umetnutih redaka → događaj je popunjen
```

## 4. Specifikacije za pridruživanje/napušstanje događaja
### Pridruživanje događaju

1. Korisnik se može pridružiti samo događaju sa statusom otvoren (`open`).
2. Korisnik se ne može pridružiti događaju koji je već počeo, završen ili otkazan.
3. Korisnik se ne može prijaviti dvaput na isti događaj.
4. Nakon uspješne prijave korisnik se automatski dodaje u događaj.


### Napuštanje događaja

1. Korisnik se može odjaviti s događaja dok on nije počeo.
2. Odjava ažurira broj sudionika i uklanja korisnika iz događaja.
3. Ako se odjavi organizator, organizatorska uloga prenosi se na sljedećeg prijavljenog sudionika (najranija prijava). Ako nema drugih sudionika, događaj se otkazuje.
4. Ako broj sudionika nakon odjave padne ispod minimalnog broja, organizator i sudionici dobivaju obavijest.
