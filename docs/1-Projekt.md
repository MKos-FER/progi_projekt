# 1. Opseg projekta

Let's Do Sports je studentski projekt na kolegiju Programsko inženjerstvo. Ovaj dokument opisuje planirani opseg aplikacije prema [odobrenom revidiranom planu](<Let's do sports - revidirani plan.md>) i dodatnim pojašnjenjima pri pripremi dokumentacije. Detaljni zahtjevi nalaze se u [analizi zahtjeva](2-Analiza-zahtjeva.md).

## 1.1 Problem i cilj projekta

Osoba koja želi igrati rekreativni nogomet, košarku ili odbojku može imati poteškoća s pronalaskom dovoljno suigrača u odgovarajućem terminu. Dolazak na teren bez prethodnog dogovora ne znači da će ondje biti dovoljno igrača ili mjesta u postojećoj skupini.

Cilj aplikacije Let's Do Sports je povezati ljude koji žele igrati isti sport na istom području. Korisnici će moći pronaći igrače prema sportu, gradu ili kvartu, dobnoj skupini i iskustvu, organizirati sportski događaj ili se pridružiti postojećem događaju. Pregled terena na karti i oglasna ploča događaja trebaju im olakšati izbor mjesta i dogovor oko igranja.

Očekivana korist je jednostavnije pronalaženje suigrača i pregled dogovora na jednom mjestu.

## 1.2 Ciljani korisnici i dionici

| Skupina korisnika ili dionika | Potreba ili interes relevantan za ovaj projekt |
| --- | --- |
| Posjetitelj bez prijave | Pregledati terene i sportske događaje prije stvaranja računa. |
| Registrirani korisnik | Urediti svoj sportski profil, pronaći igrače, pridružiti se događaju, sudjelovati u dogovoru i dodavati informacije o terenima. |
| Organizator događaja | Objaviti događaj, odrediti mjesto, termin i broj sudionika te po potrebi urediti ili otkazati događaj. Organizator je registrirani korisnik koji je stvorio događaj. |
| Administrator | Pregledati prijave netočnih podataka o terenima te ispraviti ili ukloniti neispravne podatke. |
| Studentski tim od sedam članova | Razviti aplikaciju u rokovima kolegija i steći iskustvo u timskom razvoju. Tim je dionik projekta. |
| Nastavnici i demonstratori kolegija | Pratiti napredak i ocijeniti aplikaciju, dokumentaciju i timski rad. To su dionici projekta, a ne zasebne korisničke uloge u aplikaciji. |

## 1.3 Granice projekta

**Uključeno u planiranu verziju:**

- Registracija i prijava e-poštom i lozinkom te uređivanje korisničkog profila sa sportovima i razinom iskustva.
- Pretraživanje igrača prema sportu, gradu ili kvartu, dobnoj skupini i iskustvu. Profile igrača mogu pregledavati prijavljeni korisnici.
- Javni pregled događaja i filtriranje prema sportu, području i terminu. Prijavljeni korisnici mogu stvarati događaje i prijavljivati se na njih dok ima mjesta.
- Uređivanje i otkazivanje vlastitog događaja te odjava sudionika. Organizator se računa u maksimalan broj sudionika, a prijave ne trebaju njegovo odobrenje.
- Oglasna ploča svakog događaja: organizator i sudionici mogu objavljivati tekstualne komentare i brisati vlastite komentare. Novi komentari vide se nakon ponovnog otvaranja ili osvježavanja stranice.
- Javni pregled terena na karti i njihovih osnovnih podataka. Prijavljeni korisnici mogu dodati teren s početnom fotografijom, dodavati nove fotografije i prijaviti netočne podatke. Autor može uređivati svoj teren, a administrator obrađivati prijave i ispravljati ili uklanjati neispravne podatke.
- Poveznica na vanjsku stranicu za rezervaciju terena ako je dostupna. Odabir termina i potvrda rezervacije odvijaju se na toj stranici.
- Besplatno korištenje aplikacije i prikaz unaprijed pripremljenih oglednih oglasa.

**Izvan opsega ili odgođeno:**

U ovoj verziji neće biti privatnih poruka, chata u stvarnom vremenu, obavijesti ni zasebnih grupnih chat soba. Za dogovor je predviđena oglasna ploča događaja. Aplikacija neće automatski sastavljati skupine igrača, slati pozivnice, voditi listu čekanja ni tražiti odobravanje prijava na događaj.

Izravna rezervacija i potvrda termina u aplikaciji, plaćanje, partnerstva sa sportskim centrima i povezivanje sa stvarnom oglasnom mrežom ostavljaju se za mogući budući razvoj. Aplikacija neće voditi vlastitu evidenciju slobodnih termina ni rezervacija terena. Time se opseg zadržava na mogućnostima koje tim može razviti u rokovima kolegija.

**Vanjske ovisnosti:**

Za prikaz karte potreban je vanjski kartografski servis, koji još nije odabran. Za rezervacije se koriste vanjske stranice sportskih centara; aplikacija o njima ovisi samo pri odlasku na rezervaciju. Podaci aplikacije pohranjivat će se u PostgreSQL bazu. Usluga za javno postavljanje aplikacije još nije odabrana. Dogovorene tehnologije i otvorene odluke zabilježene su u [odjeljku 2.4 analize zahtjeva](2-Analiza-zahtjeva.md#24-ograničenja-i-pretpostavke).

## 1.4 Postojeći pristupi i opravdanost projekta

Kao primjer postojećeg načina dogovora možemo zamisliti skupinu prijatelja koja porukama traži još nekoliko igrača i usklađuje termin. Takav dogovor može biti dovoljan među poznanicima, ali u tom scenariju osobi izvan skupine nedostaje mjesto na kojem može pronaći događaj i pridružiti mu se. Let's Do Sports treba omogućiti pregled igrača, događaja i terena na jednom mjestu, uz oglasnu ploču za dogovor sudionika. Ovaj primjer objašnjava potrebu koju projekt želi riješiti.
