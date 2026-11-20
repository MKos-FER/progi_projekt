v

# Let's do sports


Aplikaicja korisnicima omogućuje pronalazak ljudi na sličnoj razini vještine za igranje sporta u dogovorenom terminu.

**Projekt na kolegiju:** Ovu je aplikaciju razvio studentski tim u sklopu kolegija [Programsko inženjerstvo](https://www.fer.unizg.hr/predmet/proinz) na Fakultetu elektrotehnike i računarstva Sveučilišta u Zagrebu (FER).

## Opis aplikacije


Aplikacija rješava problem pronalaska ljudi za bavljenje sportom, posebno osoba koje imaju sličnu razinu vještine, interese i dostupno vrijeme. Namijenjena osobama koji žele jednostavno pronaći odgovarajuće osobe za zajedničko igranje, bez obzira na prethodno poznanstvo. Glavna korist aplikacije je lakše organiziranje sportskih aktivnosti i kvalitetnija igra kroz povezivanje korisnika slične razine znanja.

Trenutačne funkcionalnosti:
- Prijava pomoću OAuth2
- Kreacija te uređivanje korisničkog profila
- Priključivanje, izlazak te odgađanje događaja
- Filtriranje događaja 

Buduće funkcionalnosti:
- Dodavanje te uređivanje vlastitih terena
- Komentiranje pod događajima te odgovaranje na komentare



**Ključne funkcionalnosti**

-   Kreiranje korisničkog profila s odabirom sportova i određivanjem vlastite razine vještine.
-   Organiziranje zajedničkih sportskih aktivnosti kroz dogovaranje termina i povezivanje s drugim zainteresiranim igračima.
-   Pretraživanje i pronalazak drugih korisnika koji se bave istim sportom i imaju sličnu razinu vještine.

Za potpuni opseg, zahtjeve, oblikovanje i dokaze ispitivanja pogledajte [docs](https://github.com/MKos-FER/progi_projekt/tree/master/docs).

<a id="deployment"></a>
## Postavljanje (engl. deployment)

**Javno dostupna aplikacija:** [letsdosports.eu](https://letsdosports.eu/)

**Isprobajte:** Registrirajte se i izradite korisnički profil unosom osnovnih podataka, odabirom sporta i vlastite razine vještine.

**Demonstracija:** Za korištenje aplikacije potrebno je registrirati vlastiti korisnički račun.

**Ograničenja:** Aplikacija je u razvojnom stanju te nema sve funkcionalnosti implementirane. Aplikacija je trenutačno dostupna u demonstracijskoj verziji, stoga se očekuje manji broj registriranih korisnika i objavljenih sportskih aktivnosti. Zbog toga mogućnosti pronalaska igrača i sudjelovanja u sportskim aktivnostima mogu biti ograničene.

[Vodič za postavljanje i pokretanje](docs/6-Postavljanje-instalacija-i-konfiguracija.md) opisuje smještaj aplikacije (engl. hosting) koji tim stvarno koristi. Tim može koristiti Render ili drugu platformu koju ste dogovorili s asistentom/demonstratorom.

<a id="quick-start"></a>

## Brzi početak / instalacija

**Preduvjeti:**

Aplikacija je postavljena na Linux poslužitelju (VPS). Za posluživanje aplikacije koristi se Caddy web poslužitelj, koji upravlja HTTPS vezom, poslužuje frontend aplikaciju i prosljeđuje zahtjeve prema backendu. Backend aplikacija pokreće se kao systemd servis, dok se podaci pohranjuju u PostgreSQL bazu podataka.



Frontend aplikacija poslužuje se kao statički sadržaj iz direktorija `/var/www/lets-do-sports`, dok backend prihvaća zahtjeve na portu `8080`. GitHub webhook servis sluša na portu `5999` i zadužen je za pokretanje automatskog deploymenta.



Za lokalno pokretanje aplikacije potrebno je instalirati:

* **Java JDK 23** – za pokretanje backend aplikacije.
* **Bun** – za instalaciju ovisnosti i pokretanje frontend aplikacije.
* **PostgreSQL** – za pohranu podataka.
* **Git** – za preuzimanje projektnog repozitorija.



1. Preuzimanje projekta

```bash
git clone https://github.com/MKos-FER/progi\_projekt.gitcd progi\_projekt
```

2. Konfiguracija baze podataka

Potrebno je pokrenuti PostgreSQL poslužitelj i kreirati bazu podataka za projekt.

Kreirati bazu podataka i korisnika s odgovarajućim ovlastima. Podatke za povezivanje s bazom potrebno je uskladiti s konfiguracijom backend aplikacije.

3. Konfiguracija backenda

Potrebno je konfigurirati varijable okruženja za povezivanje s bazom podataka i OAuth2 autentifikaciju. Primjer možete pronaći [ovdje](https://github.com/MKos-FER/progi_projekt/blob/master/db/.env.example)

Za OAuth2 prijavu potrebno je konfigurirati odgovarajuće pristupne podatke pružatelja autentifikacije te adresu lokalnog preusmjeravanja (`http://localhost:8080`).

Nakon konfiguracije pokrenuti backend aplikaciju na portu `8080`.

4. Pokretanje frontenda

U novom terminalu:

```bash
cd frontendbun installbun run dev
```

Frontend razvojni poslužitelj pokreće se na adresi `http://localhost:5173`.

5. Korištenje aplikacije

Otvoriti preglednik i posjetiti:

`http://localhost:5173`

Frontend automatski prosljeđuje API zahtjeve (`/api`) i OAuth2 zahtjeve (`/oauth2`) na backend koji sluša na portu `8080`.

Za ispravan rad aplikacije potrebno je da PostgreSQL i backend budu pokrenuti prije korištenja funkcionalnosti koje zahtijevaju pristup podacima.

Za detaljno postavljanje pogledajte [projektnu dokumentaciju](docs/Home.md).

## Tehnologije


| Tehnologija ili alat | Uloga u ovom projektu | Razlog uporabe ili važno ograničenje |
| --- | --- | --- |
|Java | Glavni programski jezik za razvoj backend dijela aplikacije. | Omogućuje objektno orijentirani razvoj, dobru organizaciju koda i integraciju sa Spring Boot okvirom. |
| Spring Boot | Backend framework za razvoj REST API-ja i poslovne logike aplikacije. |Pojednostavljuje razvoj backend sustava i povezivanje s bazom podataka. |
| PostgreSQL | Sustav za upravljanje relacijskom bazom podataka. | Koristi se za pohranu korisničkih profila, sportova, razina vještina i podataka o sportskim aktivnostima. |
| React | JavaScript biblioteka za razvoj korisničkog sučelja (frontend). | Omogućuje izradu dinamičkog i interaktivnog sučelja te komunikaciju s backend dijelom aplikacije. |
| JavaScript | Programski jezik koji se koristi za razvoj frontend dijela aplikacije. | Omogućuje implementaciju interaktivnosti i upravljanje podacima u React komponentama. |
| Docker | Alat za kontejnerizaciju aplikacije i njezinih servisa. | Olakšava pokretanje i konfiguraciju aplikacije u različitim okruženjima. Koristi se ako je projekt konfiguriran za rad putem Docker kontejnera. | 
|Git | Sustav za kontrolu verzija izvornog koda. | Omogućuje praćenje promjena, povratak na prethodne verzije i suradnju članova razvojnog tima. |
| Maven | Alat za upravljanje ovisnostima i izgradnju Java aplikacije. | Olakšava upravljanje bibliotekama i automatizira izgradnju Spring Boot projekta. Koristi se ako projekt koristi Maven. |

<a id="team-members"></a>
## Članovi tima

| Član | Glavna uloga ili doprinos |
| --- | --- |
| Nikša Šutej | Voditelj tima |
| Leonarda Ćuro | Frontend razvoj i dizajn sučelja |
| Mateo Kos | Full-stack i dev-ops |
| Luka Bajto | Baza podataka |
| Roko Butorac | Baza podataka |
| Filip Žužić | Backend razvoj |
| Dominik Lovrić | Backend razboj |

## Doprinos projektu

Rad na projektu i obavljene promjene evidentiraju se putem Git commitova, koji omogućuju praćenje razvoja i pregled povijesti promjena u kodu. Za organizaciju, dodjelu i praćenje zadataka te evidentiranje potencijalnih nedostataka koristiti se GitHub Issues. Pregled promjena koda obavlja se kroz pregled commitova i promjena na GitHub repozitoriju. Aktualna evidencija zadataka dostupna je u odjeljku [GitHub Issues](https://github.com/MKos-FER/progi_projekt/issues).

<a id="license"></a>

**Status licence:** Projekt je objavljen pod MIT licencom, čiji je puni tekst dostupan u dokumentu [LICENCE](https://github.com/MKos-FER/progi_projekt/blob/master/LICENSE)

Dokumentacijski predložak kolegija sadrži otvorene obrazovne sadržaje (engl. Open Educational Resources, OER) i licenciran je licencom Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International. Ta licenca **ne određuje automatski licencu studentske aplikacije**; tim treba jasno navesti licencu svojeg projekta u prethodnom retku i datoteci `LICENSE`, ako je primjenjivo.

Kod trećih strana, skupovi podataka, slike, ikone i drugi sadržaji zadržavaju vlastite licence i uvjete uporabe. 
<a id="ai-usage"></a>
## Uporaba umjetne inteligencije [![AI Usage: Disclosed](https://img.shields.io/badge/AI%20Usage-Disclosed-blue.svg)](https://www.fer.unizg.hr/_download/repository/Policy%20on%20the%20appropriate%20use%20of%20artificial%20intelligence%20at%20the%20faculty%20of%20electrical%20engineering%20and%20computing%5B1%5D.pdf)

Ovaj projekt slijedi FER-ova [pravila o primjerenoj uporabi umjetne inteligencije](https://www.fer.unizg.hr/_download/repository/Policy%20on%20the%20appropriate%20use%20of%20artificial%20intelligence%20at%20the%20faculty%20of%20electrical%20engineering%20and%20computing%5B1%5D.pdf).

**Izjava:** Tijekom izrade projekta korišten je alat Claude (Anthropic) i Codex (OpenAI) kao pomoć pri razvoju. Korišten je za pomoć pri implementaciji pojedinih funkcionalnosti, analizu programskog koda, za formatiranje dijelova dokumentacije, te generalan debugging pri razvoju.

Prijedlozi generirani pomoću alata kritički su pregledani i provjereni od strane članova tima. Članovi tima samostalno su donosili odluke o prihvaćanju i prilagodbi predloženih rješenja te su odgovorni za ispravnost i funkcionalnost konačne implementacije. Tim razumije implementirana rješenja i sposoban je objasniti vlastiti doprinos razvoju projekta.



<a id="code-of-conduct-and-support"></a>
## Kodeks ponašanja i podrška [![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

Od članova tima očekuje se pridržavanje **Kodeksa ponašanja studenata** Fakulteta elektrotehnike i računarstva Sveučilišta u Zagrebu, pravila timskog rada na kolegiju, [Etičkog kodeksa IEEE-a](https://www.ieee.org/about/corporate/governance/p7-8.html) i našeg [kodeksa ponašanja](CODE_OF_CONDUCT.md).

---

Struktura dokumentacije prilagođena je predlošku kolegija Programsko inženjerstvo FER-a autora [Vlade Sruka](https://www.fer.unizg.hr/vlado.sruk), licenciranom pod [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). Aplikaciju i njezin sadržaj izradio je gore navedeni studentski tim.

