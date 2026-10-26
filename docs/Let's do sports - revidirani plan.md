# Let's do sports

---

## Problem

Mnogi ljudi žele se baviti rekreativnim sportovima, ali često imaju problem s pronalaskom dovoljnog broja igrača za organizaciju utakmice. Primjerice, korisnik može doći na sportski teren i pronaći ili prazan teren bez drugih igrača ili već popunjenu grupu koja nema mjesta za dodatne igrače.

Problem je posebno izražen kod sportova koji zahtijevaju veći broj igrača, poput nogometa, košarke i odbojke. Cilj aplikacije Let's Do Sports je omogućiti jednostavno povezivanje ljudi koji žele igrati isti sport i nalaze se u blizini.

## Cilj projekta

Let's Do Sports je aplikacija koja omogućuje korisnicima pronalazak drugih zainteresiranih igrača u svojoj blizini, organizaciju sportskih aktivnosti i pronalazak odgovarajućih sportskih terena.

Nakon registracije korisnik odabire sportove koje želi igrati, svoju dob i razinu iskustva.

## Pronalaženje igrača

Aplikacija omogućuje korisnicima samostalno pretraživanje i filtriranje profila drugih igrača prema odabranim kriterijima:

- sport koji korisnik želi igrati
- lokacija odnosno područje u kojem želi igrati
- dobna skupina
- razina iskustva

Korisnik sam pregledava rezultate filtriranja i odlučuje s kojim bi igračima želio sudjelovati u sportskoj aktivnosti. Aplikacija ne sastavlja automatski grupe igrača.

Korisnici mogu pregledavati i postojeće sportske događaje te ih filtrirati prema sportu, lokaciji i terminu igranja.

## Registracija i korisnički profil

Prilikom registracije korisnik unosi:

- ime i korisničko ime
- profilnu sliku
- spol
- dob
- lokaciju odnosno područje u kojem želi igrati
- sportove koje želi igrati
- razinu iskustva za odabrane sportove

Razina iskustva može se definirati, primjerice, kao:

- početnik
- rekreativac
- napredni rekreativac
- trenirao određeni broj godina

Korisnik nakon registracije može uređivati svoje podatke i odabrane sportove.

## Sportski događaji i oglasne ploče

Registrirani korisnik može organizirati sportski događaj i objaviti ga na popisu događaja koji ostali korisnici mogu pregledavati. Prilikom kreiranja događaja organizator određuje:

- sport
- sportski teren odnosno mjesto održavanja
- datum i vrijeme početka
- maksimalan broj sudionika
- kratak opis ili dodatne upute

Drugi korisnici mogu pregledavati događaje i prijaviti se na one koji imaju slobodnih mjesta. Prijavljeni korisnici čine skupinu sudionika događaja, bez potrebe za kreiranjem zasebne grupe.

Svaki sportski događaj ima vlastitu **oglasnu ploču** koja služi za dogovor i razmjenu informacija među sudionicima. Organizator i prijavljeni sudionici mogu objavljivati kratke tekstualne komentare, primjerice:

- "Ponesite loptu ako imate."
- "Mogu doći deset minuta kasnije."
- "Hoćemo li igrati tri na tri?"

Komentari se prikazuju kronološki, uz ime autora i vrijeme objave. Autor može obrisati vlastiti komentar. Organizator može uređivati osnovne podatke o događaju ako se promijene termin, lokacija ili druge važne informacije.

Oglasna ploča funkcionira poput jednostavne rasprave: korisnici vide nove komentare kada ponovno otvore ili osvježe stranicu događaja. **Nisu potrebni chat u stvarnom vremenu, privatne poruke, obavijesti ni zasebne grupne chat sobe.**

## Sportski tereni

Aplikacija omogućuje pregled sportskih terena na karti.

Svaki teren minimalno sadržava:

- naziv terena
- lokaciju
- sportove za koje je namijenjen
- fotografiju terena
- opis
- informaciju je li teren besplatan ili se naplaćuje
- informaciju o mogućnosti rezervacije

Korisnici mogu dodavati nove terene u aplikaciju. Prilikom dodavanja terena potrebno je unijeti njegovu lokaciju, naziv, vrstu sporta i početnu fotografiju.

Korisnici nakon korištenja terena mogu dodati novu fotografiju kako bi se omogućilo ažuriranje informacija o stanju terena.

## Rezervacija terena

Za terene koji podržavaju rezervacije aplikacija omogućuje korisniku odabir dostupnog termina.

Za komercijalne terene moguće je ostvariti integraciju s postojećim sustavom za rezervacije ili korisnika preusmjeriti na vanjsku stranicu sportskog centra.

U slučaju ostvarivanja partnerstva sa sportskim centrima, rezervacija i potvrda termina mogu se izvršavati izravno unutar aplikacije.

## Monetizacija

Korištenje aplikacije za korisnike je besplatno.

Jedini oblik monetizacije u osnovnoj verziji aplikacije su oglasi koji se prikazuju na za to predviđenim mjestima unutar aplikacije.

U budućnosti je moguće ostvariti dodatne prihode kroz partnerske odnose sa sportskim centrima i drugim pružateljima sportskih usluga.

## Tehničke napomene

Stanje i postojanje javnih sportskih terena mogu inicijalno unositi korisnici aplikacije. Prilikom unosa terena korisnik mora dodati osnovne informacije i početnu fotografiju.

Aplikacija treba omogućiti ažuriranje podataka o terenu, pri čemu korisnici mogu nakon korištenja dodati novu fotografiju ili prijaviti da informacije o terenu više nisu točne.

Za prikaz lokacija i terena može se koristiti vanjski servis za karte i geolokaciju.

Komunikacija među korisnicima odvija se putem oglasne ploče pojedinog sportskog događaja. Komentari se spremaju u bazu podataka i dohvaćaju običnim HTTP zahtjevima, bez potrebe za komunikacijom u stvarnom vremenu.

Podaci o korisnicima, sportovima, sportskim događajima, sudionicima, komentarima, terenima, fotografijama i rezervacijama trebaju se pohranjivati u odgovarajuću bazu podataka.
