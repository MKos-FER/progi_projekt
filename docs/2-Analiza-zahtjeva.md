# 2. Analiza zahtjeva

Ovaj dokument opisuje planirane zahtjeve aplikacije Let's Do Sports, u skladu s [opsegom projekta](1-Projekt.md). Zahtjevi nisu potvrda da su funkcionalnosti već izrađene ili ispitane.

Izvori navedeni u tablicama imaju sljedeće značenje:

- **Plan** - [odobreni revidirani plan projekta](<Let's do sports - revidirani plan.md>).
- **Pojašnjenje** - dodatni dogovori s članom tima pri pripremi ove dokumentacije: način prijave, javni pregled, pravila događaja, ovlasti za terene, rezervacije, oglasi i pretraživanje po području.
- **Dogovor dokumentacije** - osnovni zahtjevi kvalitete prihvaćeni u planu izrade ovih dokumenata.

Prioritet **Visok** označava osnovne mogućnosti i pravila aplikacije, **Srednji** dodatne mogućnosti važne za dogovoreni opseg, a **Nizak** sporedne mogućnosti. Niži prioritet ne znači da je zahtjev izvan opsega.

## 2.1 Funkcijski zahtjevi

| ID | Opis | Prioritet | Izvor | Kriteriji prihvaćanja |
| --- | --- | --- | --- | --- |
| F-001 | Posjetitelj može registrirati račun e-poštom i lozinkom. | Visok | Plan | S valjanom e-poštom i lozinkom stvara se račun. Već korištena e-pošta i nedostajući obavezni podaci ne stvaraju novi račun; prikazuje se objašnjenje. |
| F-002 | Korisnik se može prijaviti e-poštom i lozinkom. | Visok | Plan | Ispravni podaci omogućuju pristup radnjama prijavljenog korisnika. Pogrešni podaci ne omogućuju prijavu i prikazuje se poruka o neuspješnoj prijavi. |
| F-003 | Prijavljeni korisnik može se odjaviti. | Visok | Plan | Nakon odjave zaštićene radnje ponovno zahtijevaju prijavu. |
| F-004 | Korisnik pri registraciji popunjava svoj sportski profil, a poslije ga može uređivati. | Visok | Plan | Profil sadrži ime, korisničko ime, profilnu sliku, spol, dob, grad ili kvart, odabrane sportove i razinu iskustva za svaki odabrani sport. Spremljene promjene vidljive su nakon ponovnog otvaranja profila. Korisnik ne može uređivati tuđi profil. |
| F-005 | Prijavljeni korisnik može pregledavati profile i filtrirati igrače. | Visok | Plan | Filtri za sport, grad ili kvart, dobnu skupinu i iskustvo mogu se kombinirati; prikazani igrači odgovaraju odabranim kriterijima. Bez rezultata prikazuje se jasna poruka. Posjetitelj bez prijave ne može pregledavati profile igrača. |
| F-006 | Svatko može pregledavati događaje i filtrirati ih prema sportu, području i terminu. | Visok | Plan | Pregled radi i bez prijave. Rezultati odgovaraju odabranim filtrima, a detalji događaja prikazuju sport, organizatora, mjesto, datum i vrijeme, broj sudionika, maksimalan broj sudionika i opis. |
| F-007 | Prijavljeni korisnik može stvoriti sportski događaj. | Visok | Plan | Korisnik unosi sport, teren ili mjesto, grad ili kvart, datum i vrijeme početka, maksimalan broj sudionika i opis. Događaj se pojavljuje u pregledu, a njegov autor postaje organizator i prvi sudionik. Maksimalan broj sudionika mora biti najmanje jedan. |
| F-008 | Organizator može uređivati osnovne podatke svojeg događaja. | Visok | Plan | Spremljene promjene sporta, mjesta, područja, termina, maksimalnog broja sudionika i opisa vidljive su u detaljima. Novi maksimum ne može biti manji od već prijavljenog broja sudionika. Drugi korisnik ne može uređivati taj događaj. |
| F-009 | Organizator može otkazati svoj događaj. | Visok | Plan | Događaj dobiva vidljivu oznaku da je otkazan i ne prima nove prijave. Drugi korisnik ne može otkazati taj događaj. |
| F-010 | Prijavljeni korisnik može se prijaviti na događaj koji ima slobodnih mjesta. | Visok | Plan | Prijava odmah dodaje korisnika među sudionike, bez odobrenja organizatora. Ponovna prijava istog korisnika, prijava na popunjeni ili otkazani događaj odbija se uz poruku. Broj sudionika, uključujući organizatora, ne prelazi maksimum ni pri istodobnim pokušajima prijave. |
| F-011 | Sudionik može odjaviti svoje sudjelovanje na događaju. | Visok | Plan | Sudionik koji nije organizator može se ukloniti s popisa, čime se oslobađa mjesto. Ne može odjaviti drugog korisnika. Organizator za odustajanje od održavanja koristi otkazivanje događaja. |
| F-012 | Oglasna ploča događaja prikazuje komentare kronološki. | Visok | Plan | Uz svaki komentar prikazuju se autor i vrijeme objave. Novi komentari vide se nakon ponovnog otvaranja ili osvježavanja stranice događaja. |
| F-013 | Organizator i prijavljeni sudionici mogu objavljivati tekstualne komentare na oglasnoj ploči događaja. | Visok | Plan | Neprazan komentar sudionika ili organizatora sprema se i pojavljuje na ploči. Prazan komentar i pokušaj objave osobe koja nije sudionik ni organizator odbijaju se. |
| F-014 | Autor može obrisati vlastiti komentar. | Srednji | Plan | Obrisani komentar više se ne prikazuje nakon osvježavanja. Drugi korisnik ne može obrisati tuđi komentar. |
| F-015 | Svatko može pregledavati sportske terene na karti. | Visok | Plan | Tereni s unesenom lokacijom prikazuju se oznakama na karti i bez prijave. Odabirom oznake otvaraju se podaci o pripadajućem terenu. |
| F-016 | Svatko može pregledati podatke pojedinog terena. | Visok | Plan | Prikazuju se naziv, lokacija, sportovi, fotografije, opis, podatak je li teren besplatan ili se naplaćuje i podatak o mogućnosti rezervacije. |
| F-017 | Prijavljeni korisnik može dodati sportski teren. | Visok | Plan | Korisnik unosi naziv, lokaciju na karti, grad ili kvart, sportove, početnu fotografiju, opis te podatke o naplati i mogućnosti rezervacije. Bez naziva, lokacije, sporta ili početne fotografije teren se ne sprema; nakon uspješnog dodavanja dostupan je u pregledu. |
| F-018 | Autor može uređivati podatke o terenu koji je dodao. | Srednji | Plan | Promjene podataka vidljive su nakon ponovnog otvaranja terena. Drugi obični korisnik ne može mijenjati te podatke; administratorske ovlasti opisane su u F-021. |
| F-019 | Prijavljeni korisnik može dodati novu fotografiju terena. | Srednji | Plan | Dodana fotografija ostaje povezana s odabranim terenom i vidljiva je nakon ponovnog otvaranja njegovih podataka. |
| F-020 | Prijavljeni korisnik može prijaviti netočne podatke o terenu. | Srednji | Plan | Prijava s opisom netočnosti sprema se uz odgovarajući teren i dostupna je administratoru. Prijava bez opisa ne sprema se. |
| F-021 | Administrator može obraditi prijave netočnosti i ispraviti ili ukloniti neispravne podatke o terenima. | Srednji | Plan | Administrator vidi prijavljeni teren i opis problema, može ispraviti podatke ili ukloniti neispravan unos te označiti prijavu obrađenom. Obični korisnik nema pristup tim radnjama. |
| F-022 | Za teren s dostupnom vanjskom rezervacijom prikazuje se poveznica na stranicu za rezervaciju. | Srednji | Plan | Odabir poveznice vodi na unesenu vanjsku stranicu. Ako poveznica nije unesena, ne prikazuje se aktivna poveznica za rezervaciju. Aplikacija ne prikazuje vlastite slobodne termine i ne sprema rezervacije. |
| F-023 | Aplikacija prikazuje ogledne oglase na predviđenim mjestima. | Nizak | Plan | U sučelju su vidljivi unaprijed pripremljeni oglasi označeni kao oglasi. Njihov prikaz ne zahtijeva povezivanje s oglasnom mrežom ni naplatu korisnicima. |

## 2.2 Nefunkcijski zahtjevi

Ovo su planirani zahtjevi kvalitete i načini njihove provjere. Navedene provjere još nisu provedene.

| ID | Opis | Prioritet | Izvor | Način provjere |
| --- | --- | --- | --- | --- |
| NF-01 | Lozinke se ne smiju čuvati u čitljivom obliku, nego kao sažeci dobiveni postupkom namijenjenim zaštiti lozinki. | Visok | Dogovor dokumentacije | Pregledati način obrade lozinke i zapis probnog računa u bazi te potvrditi da se izvorna lozinka ne pohranjuje, a prijava i dalje radi. |
| NF-02 | Poslužitelj mora provjeravati prijavu i ovlasti za zaštićene radnje. Skriven gumb u sučelju nije dovoljna zaštita. | Visok | Dogovor dokumentacije; Pojašnjenje | Izravno pokušati zaštićene zahtjeve bez prijave i s drugim računom: uređivanje profila, događaja i terena, komentiranje, brisanje komentara i administratorske radnje. Zahtjevi bez ovlasti moraju biti odbijeni bez promjene podataka. |
| NF-03 | Spremljeni podaci o profilima, događajima, sudionicima, komentarima, terenima, fotografijama i prijavama netočnosti moraju ostati dostupni nakon ponovnog pokretanja aplikacije. | Visok | Plan; Dogovor dokumentacije | Spremiti ogledne podatke i fotografiju, ponovno pokrenuti aplikaciju bez brisanja pohrane i provjeriti podatke te mogućnost otvaranja fotografije. |
| NF-04 | Glavni pregledi i obrasci moraju biti čitljivi i upotrebljivi na računalu i mobitelu. | Srednji | Dogovor dokumentacije | U pregledniku provjeriti širine od 1280 i 375 piksela: tekst i gumbi ne smiju se preklapati, a prijava, pregled događaja i prijava na događaj moraju biti izvedivi. Kartu mora biti moguće koristiti unutar njezina prikaza. |
| NF-05 | Obrasci i radnje moraju prikazivati razumljive poruke o uspjehu, pogrešnom unosu i odbijenoj radnji. | Srednji | Dogovor dokumentacije | Provjeriti nedostajuće obavezno polje, pogrešnu prijavu, popunjen i otkazan događaj te uspješno spremanje. Korisnik mora moći razumjeti ishod bez čitanja tehničkih poruka poslužitelja. |

## 2.3 Dionici i akteri

Organizator i sudionik nisu zasebne vrste računa: to su uloge registriranog korisnika u odnosu na pojedini događaj. Zadržavaju mogućnosti registriranog korisnika. Prava autora terena i komentara ovise o tome tko je stvorio taj sadržaj.

| ID uloge ili aktera | Odnos prema sustavu | Relevantni zahtjevi |
| --- | --- | --- |
| A-01 Posjetitelj | Pregledava događaje i terene bez prijave te može otvoriti registraciju ili prijavu. | F-001, F-002, F-006, F-015, F-016, F-022, F-023 |
| A-02 Registrirani korisnik | Ima javne preglede, uređuje vlastiti profil, traži igrače, stvara događaje, prijavljuje se i odjavljuje s događaja te dodaje terene, fotografije i prijave netočnosti. Kao autor uređuje svoj teren. | F-003 - F-007, F-010, F-011, F-015 - F-020, F-022, F-023; NF-01, NF-02 |
| A-03 Organizator događaja | Registrirani korisnik koji je stvorio događaj; uređuje ga i otkazuje te sudjeluje u dogovoru na oglasnoj ploči. | F-007 - F-010, F-012 - F-014; NF-02 |
| A-04 Sudionik događaja | Registrirani korisnik prijavljen na događaj; može se odjaviti, čitati ploču, komentirati i brisati vlastite komentare. | F-010 - F-014; NF-02 |
| A-05 Administrator | Obrađuje prijave netočnih podataka i ispravlja ili uklanja neispravne podatke o terenima. | F-021; NF-02 |
| A-06 Kartografski servis | Vanjski sustav koji omogućuje prikaz karte s lokacijama terena. Pružatelj još nije odabran. | F-015 |
| D-01 Studentski tim | Dionik koji razvija aplikaciju i dokumentaciju u rokovima kolegija. Nije zasebna uloga u aplikaciji. | Svi zahtjevi; C-01 - C-05 |
| D-02 Nastavnici i demonstratori | Dionici koji prate napredak i ocjenjuju projekt. Nisu zasebna uloga u aplikaciji. | Pregled opsega i svih zahtjeva; C-01 |

Vanjska stranica za rezervaciju iz F-022 samo je odredište poveznice. Nije planirana razmjena podataka o terminima ili rezervacijama između nje i aplikacije.

## 2.4 Ograničenja i pretpostavke

| ID | Tip | Opis ograničenja ili pretpostavke | Izvor | Inženjerska posljedica / Provjera |
| --- | --- | --- | --- | --- |
| C-01 | Ograničenje | Projekt izrađuje sedam studenata u rokovima i prema kontrolnim točkama kolegija. | Projektni kontekst člana tima | Prednost imaju osnovni zahtjevi visokog prioriteta. Napredne integracije ostaju izvan dogovorenog opsega. |
| C-02 | Ograničenje | Dogovoreni su React za frontend i PostgreSQL za bazu podataka. | Pojašnjenje člana tima | Sučelje i trajnu pohranu razvijati uz te tehnologije. |
| C-03 | Ograničenje | Prikaz karte oslanja se na vanjski kartografski servis. | Plan | Prije izrade karte odabrati pružatelja i provjeriti uvjete korištenja, potrebnu konfiguraciju i ograničenja usluge. |
| C-04 | Ograničenje | Komentari se dohvaćaju običnim HTTP zahtjevima pri otvaranju ili osvježavanju stranice. | Plan | Ne uvoditi vezu za chat u stvarnom vremenu ni sustav obavijesti. |
| C-05 | Ograničenje | Rezervacije se odvijaju na vanjskoj stranici, a oglasi su ogledni. | Pojašnjenje | Ne razvijati evidenciju termina i rezervacija, plaćanje ni povezivanje s oglasnom mrežom. |
| AS-01 | Pretpostavka | Backend će se vjerojatno razvijati u Javi sa Spring Bootom, ali izbor još nije potvrđen. | Pojašnjenje člana tima | Potvrditi izbor prije razvoja backenda i tada ažurirati dokumentaciju. |
| AS-02 | Pretpostavka | Tim će odabrati kartografski servis i hosting prikladne za studentski projekt. Obje odluke još su otvorene. | Procjena pri pripremi dokumentacije | Servis za karte provjeriti prije njegove integracije, a hosting prije prvog javnog postavljanja. Ne navoditi neodabrane pružatelje kao postojeće ovisnosti. |
| AS-03 | Pretpostavka | Korisnici će unijeti početne podatke i fotografije terena te smisleno navesti svoje područje i iskustvo. | Plan | Za prvu demonstraciju pripremiti nekoliko oglednih profila, terena i događaja te provjeriti filtre i prikaz karte. Netočne podatke o terenima rješavati prijavama administratoru. |

## 2.5 Značajne promjene zahtjeva

Ovim dokumentom uspostavlja se početna verzija označenih zahtjeva. Pojašnjenja iz razgovora uključena su u tu verziju; naknadne promjene još nisu zabilježene.

| Datum | ID zahtjeva | Promjena i razlog | Dogovoreno s / dokaz |
| --- | --- | --- | --- |
| 2026-10-23 | F-001 - F-023; NF-01 - NF-05 | Početni zapis zahtjeva prema planu i pojašnjenjima: vanjske rezervacije, ogledni oglasi, prijava e-poštom i lozinkom, javni pregled terena i događaja, pravila sudjelovanja i ovlasti za terene. Implementacija chata zamijenjena oglasnom pločom i komentarima. | [Revidirani plan](<Let's do sports - revidirani plan.md>) i razgovor s članom tima pri pripremi dokumentacije. |

## 2.6 Sljedivost zahtjeva

Tablica povezuje zahtjeve s ponašanjem koje treba ostvariti. Obrasci uporabe i konkretni moduli još nisu razrađeni za ovu aplikaciju, pa su njihove poveznice na čekanju. Provjere u zadnjem stupcu zasad su samo planirane. Poveznice će se dopuniti nakon izrade [obrazaca uporabe](3-Obrasci-uporabe.md), [arhitekture](4-Arhitektura-i-oblikovanje.md) i [ispitivanja](5-Ispitivanje.md).

| ID zahtjeva | Obrazac uporabe / ponašanje | Arhitekturna realizacija | Ispitivanje ili provjera |
| --- | --- | --- | --- |
| F-001 - F-003 | Registracija, prijava i odjava; obrasci uporabe na čekanju. | Na čekanju - moduli računa i prijave još nisu izrađeni. | Planirano: valjana i nevaljana registracija, zauzeta e-pošta, ispravna i pogrešna prijava te pristup nakon odjave. Nije provedeno. |
| F-004, F-005 | Uređivanje profila i pretraživanje igrača; obrasci uporabe na čekanju. | Na čekanju - moduli profila i pretraživanja još nisu izrađeni. | Planirano: spremanje profila, kombiniranje filtara, prazan rezultat i pokušaj pregleda profila bez prijave. Nije provedeno. |
| F-006 - F-009 | Pregled, stvaranje, uređivanje i otkazivanje događaja; obrasci uporabe na čekanju. | Na čekanju - modul događaja još nije izrađen. | Planirano: javni pregled i filtri, stvaranje događaja s organizatorom kao sudionikom, promjena podataka i otkazivanje; pokušaj uređivanja tuđeg događaja. Nije provedeno. |
| F-010, F-011 | Prijava i odjava sudionika; obrasci uporabe na čekanju. | Na čekanju - obrada sudjelovanja još nije izrađena. | Planirano: prijava na slobodno mjesto, dvostruka prijava, popunjen i otkazan događaj, dva pokušaja za zadnje mjesto te oslobađanje mjesta odjavom. Nije provedeno. |
| F-012 - F-014 | Dogovor na oglasnoj ploči; obrasci uporabe na čekanju. | Na čekanju - modul komentara još nije izrađen. | Planirano: autor, vrijeme i redoslijed komentara, osvježavanje, objava sudionika, zabrana objave nesudioniku, prazan komentar i pokušaj brisanja tuđeg komentara. Nije provedeno. |
| F-015, F-016 | Pregled karte i podataka o terenu; obrasci uporabe na čekanju. | Na čekanju - modul terena i kartografska integracija još nisu izrađeni. | Planirano: pregled bez prijave, oznaka na ispravnoj lokaciji i otvaranje podataka odabranog terena. Nije provedeno. |
| F-017 - F-020 | Dodavanje i uređivanje terena, fotografija i prijava netočnosti; obrasci uporabe na čekanju. | Na čekanju - obrada terena, fotografija i prijava još nije izrađena. | Planirano: unos bez obaveznih podataka i početne fotografije, valjan unos, nova fotografija, uređivanje vlastitog i tuđeg terena te prijava netočnosti. Nije provedeno. |
| F-021 | Administratorska obrada prijava; obrazac uporabe na čekanju. | Na čekanju - administratorske funkcije još nisu izrađene. | Planirano: pregled prijave, ispravak ili uklanjanje neispravnog unosa, oznaka obrađene prijave i odbijanje pristupa običnom korisniku. Nije provedeno. |
| F-022 | Odlazak na vanjsku rezervaciju; obrazac uporabe na čekanju. | Na čekanju - prikaz vanjske poveznice još nije izrađen. | Planirano: teren s poveznicom i bez nje; provjera odredišta poveznice. Nije provedeno. |
| F-023 | Prikaz oglednih oglasa. | Na čekanju - prikaz oglasa još nije izrađen. | Planirano: pregled označenih oglednih oglasa u sučelju. Nije provedeno. |
| NF-01, NF-02 | Zaštita lozinki i provjera ovlasti pri zaštićenim radnjama. | Na čekanju - obrada računa i ovlasti još nije izrađena. | Planirano: pregled pohrane lozinki i izravni zahtjevi bez prijave ili s nedovoljnim ovlastima prema odjeljku 2.2. Nije provedeno. |
| NF-03 | Trajno spremanje podataka i fotografija. | Na čekanju - pohrana u PostgreSQL i način spremanja fotografija još nisu izrađeni. | Planirano: spremanje podataka i fotografije te provjera nakon ponovnog pokretanja. Nije provedeno. |
| NF-04, NF-05 | Čitljivost sučelja i razumljive poruke. | Na čekanju - sučelje i poruke još nisu izrađeni. | Planirano: pregled na dvjema širinama zaslona i scenariji uspješnih i odbijenih radnji prema odjeljku 2.2. Nije provedeno. |

## 2.7 Pregled zahtjeva

Formalni pregledi zahtjeva još nisu provedeni. Odluke nastale nakon stvarnih povratnih informacija upisat će se ovdje, a značajne promjene i u odjeljak 2.5.

| Kontrolna točka | Povratna informacija ili nalaz | Odluka o zahtjevu ili opsegu |
| --- | --- | --- |
| Iteracija 1 | Pregled još nije proveden. | Nema zabilježene odluke. |
| Demo 2 | Pregled još nije proveden. | Nema zabilježene odluke. |
| Završni pregled | Pregled još nije proveden. | Nema zabilježene odluke. |
