---
date_created: 2026-10-23T20:11:02+02:00
date_modified: 2026-10-23T20:18:33+02:00
---

# Sprint-01

---

## 1. Tehnologije i arhitektura

- **Karte:** Koristit ćemo [OpenStreetMap](https://www.openstreetmap.org/).
- **Backend:** Java Spring Boot.
- **Autentifikacija:** Registracija i prijava korisnika ostvarivat će se putem vanjskih servisa za autentifikaciju, korištenjem OAuth 2.0 (npr. Google Login), umjesto vlastitog sustava korisničkih imena i lozinki.

## 2. Funkcionalni zahtjevi

**Oglasna ploča događaja**
- Dodati novi funkcijski zahtjev: mogućnost odgovaranja na pojedinačne komentare ("Discord-style Reply").
    - Svaki komentar ima gumb "Reply".
    - Klikom na "Reply" iznad polja za unos prikazuje se referenca na komentar na koji korisnik odgovara.
- Provjeriti mogućnost automatskog osvježavanja komentara u određenim vremenskim intervalima (npr. svakih 10-15 sekundi).

**Frontend**
- Omogućiti filtriranje događaja prema sportu izravno na početnoj stranici (homepage).

**Korisnički profil**
- Dodati atribut "Skill level" (razina vještine):
    - Amateur
    - Average
    - Pro
- GPS lokacija korisnika neće se pohranjivati u bazu podataka.
- Prilikom registracije korisnik odabire grad u kojem želi pretraživati sportske događaje.

## 3. Zadaci i rokovi

- **Leonarda:** Izraditi početni prijedlog korisničkog sučelja (UI draft).
- **Subota, 24.10.2026.:**
    - Svi članovi tima pregledavaju projektnu dokumentaciju.
    - Dostaviti komentare i prijedloge izmjena.
- **Nedjelja, 25.10.2026.:**
    - Razmotriti pristigle komentare.
    - Donijeti konačne odluke o otvorenim pitanjima.
    - Započeti implementaciju dogovorenih rješenja.

## 4. Otvorena pitanja

- Odrediti interval automatskog osvježavanja komentara.
- Potvrditi odabir vanjskog servisa za autentifikaciju.
- Definirati detalje implementacije odgovaranja na komentare.
