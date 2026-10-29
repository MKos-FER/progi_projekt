# Quick start

Koristeći docker-compose
```bash
sudo docker compose up
```

ili alternativno s podman-compose
```bash
podman compose up
```

Pokretanjem naredbe kratko se pokreće i `migrate` pomoćni container koji
provjerava da je `db` container ispravan te onda pokreće `.sql` datoteke
iz `./migrations/` direktorija.

> [!warning]
> `migrate` **neće** pokretati `.sql` datoteke koje su modificirane a već prije
> pokrenute već samo novododane datoteke. Ako je nužno urediti prijašnju `.sql`
> datoteku potrebno je u potpunosti izbrisati bazu ([naredba](#brisanje-baze))

# Gašenje baze

Naredbom
```bash
sudo docker compose down
```
ili s podmanom
```bash
podman compose down
```

# Brisanje baze

Naredbom
```bash
sudo docker compose down -v
```
ili s podmanom
```bash
podman compose down -v
```

> [!important]
> Ovo će izbrisati sve podatke koje baza sadržava!
> Ponovnim pokretanjem će se obnoviti samo podatci zapisani u migracijama.


# Produkcija

U produkcijskom okruženju baza (`db` servis) je pokrenuta uvijek te se migracije
izvršavaju eksplicitnim pozivom `migrate` servisa.

To se može napraviti ručno sa:
```bash
sudo docker compose run --rm migrate
```
i naravno sa podmanom bez sudo naredbe.

`migrate` pokreće sql datoteke koje nisu bile pokrenute prije kada se pozove.
