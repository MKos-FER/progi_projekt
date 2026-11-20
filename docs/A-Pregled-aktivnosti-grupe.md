# A. Pregled aktivnosti grupe

Ovaj pregled sažima rad tima Let's Do Sports od 8. 10. do 20. 11. 2026.

## A.1 Evidencija sastanaka

**Kratice članova tima**

- DL - Dominik Lovrić
- FŽ - Filip Žužić
- LB - Luka Bajto
- LĆ - Leonarda Ćuro
- MK - Mateo Kos
- NŠ - Nikša Šutej
- RB - Roko Butorac

| Datum i sudionici | Glavna tema | Donesena odluka ili zadatak | Poveznica na zapis ili rezultat |
| --- | --- | --- | --- |
| 23. 10. 2026. (DL, FŽ, LB, LĆ, MK, NŠ, RB) | Prvi sprint i opseg aplikacije | Dogovoreni su Spring Boot, OpenStreetMap i prijava putem vanjskog pružatelja identiteta. U zahtjeve je dodan odgovor na komentar na oglasnoj ploči. LĆ priprema nacrt sučelja, a svi članovi do 24. 10. pregledavaju dokumentaciju i šalju komentare. | [Prvi sprint](meeting-notes/sprint-01.md), [usvojene izmjene dokumentacije](https://github.com/MKos-FER/progi_projekt/commit/0e34615) |
| 29. 10. 2026. (MK, RB, koordinacija na Discordu) | Model događaja i pravila sudjelovanja | Dogovoreno je da se status računa iz termina i popunjenosti te da se organizator računa u kapacitet. RB je zapisao odluke i provjeru proširenja sheme, a LB ih je primijenio u početnim migracijama za događaje. | [Zapis o modelu događaja](meeting-notes/baza-dogadaji-2026-10-29.md#1-odluke), [migracije](https://github.com/MKos-FER/progi_projekt/commit/45bbc44) |

Detaljni zapisi koordinacije i povezane tehničke bilješke nalaze se u direktoriju [meeting-notes](meeting-notes/).

## A.2 Plan rada

Plan rada održava Filip Žužić (FŽ), a tim prati zadatke u [GitHub Issues](https://github.com/MKos-FER/progi_projekt/issues). Tablica sažima plan iz zapisa koordinacije i tehničkog ugovora. Ondje gdje u zapisima nema imena, navedeni su izvršitelji prema povezanim izmjenama.

| Razdoblje | Glavni planirani rad | Odgovorni članovi | Poveznica na zadatke ili dogovor |
| --- | --- | --- | --- |
| 23.-25. 10. 2026. | Pripremiti početni nacrt sučelja. Do 24. 10. prikupiti primjedbe na dokumentaciju, a 25. 10. razmotriti ih i započeti provedbu dogovorenih izmjena. | LĆ (nacrt sučelja), svi članovi (pregled dokumentacije) | [Zadaci i rokovi prvog sprinta](meeting-notes/sprint-01.md#3-zadaci-i-rokovi) |
| Kraj listopada 2026. | Dogovoriti model događaja, implementirati početne migracije i provjeriti može li se shema proširiti za terene, prijave netočnosti i komentare. | MK i RB (dogovor), LB (migracije), RB (skica i provjera proširenja) | [Dogovor o modelu](meeting-notes/baza-dogadaji-2026-10-29.md), [početne migracije](https://github.com/MKos-FER/progi_projekt/commit/45bbc44) |
| Od 13. 11. 2026. | Uskladiti ER dijagram s migracijama profila i OAuth računa te provjeriti migraciju od prazne baze i nadogradnju s V3. | RB (ER dijagram), LB (provjera migracija) | [Plan usklađivanja](meeting-notes/uskladivanje-modela-v3-v4.md#potvrda-pregleda-i-sljedeći-koraci), [provjere migracija](meeting-notes/korisnici-i-oauth.md#4-provjera-migracija) |
| Od 14. 11. 2026. | Povezati Google prijavu, registraciju i uređivanje profila s korisničkim sučeljem. Dodati šifrarnike i spremanje sportova s razinom iskustva te postavljanje i zamjenu fotografije. | MK (prijava i registracija), LĆ (sučelje), DL (API profila), LB (šifrarnici i sportovi), RB (fotografija) | [Raspodjela zadataka u API ugovoru](meeting-notes/autentifikacija-i-profil.md#9-što-radi-tko), [#64](https://github.com/MKos-FER/progi_projekt/issues/64), [#67](https://github.com/MKos-FER/progi_projekt/issues/67) |
| Do 20. 11. 2026. | Pripremiti pokaznu inačicu aplikacije i dokumentaciju za predaju prvog ciklusa. | Tim | [Stranica projekta na FERWebu](https://www.fer.unizg.hr/predmet/proinz/projekt) |
| Do 22. 1. 2027. | Pripremiti završnu inačicu aplikacije i dokumentaciju za predaju drugog ciklusa. | Tim | [Stranica projekta na FERWebu](https://www.fer.unizg.hr/predmet/proinz/projekt) |

### Rokovi kolegija

| Aktivnost | Rok |
| --- | --- |
| Burza grupa | 2.-7. 10. 2026. (do 12:00) |
| Formiranje i evidentiranje tima | 10. 10. 2026. u 23:59 |
| Početni prijedlog vlastite teme | 10. 10. 2026. u 23:59 |
| Konačni prijedlog i odluka o vlastitoj temi | 16. 10. 2026. u 23:59 |
| Predaja prvog ciklusa | 20. 11. 2026. u 23:59 |
| Kraj ocjenjivanja prvog ciklusa | 6. 12. 2026. u 15:00 |
| Predaja drugog ciklusa | 22. 1. 2027. u 23:59 |
| Kraj ocjenjivanja drugog ciklusa | 29. 1. 2027. u 15:00 |

Izvor rokova je [stranica projekta na FERWebu](https://www.fer.unizg.hr/predmet/proinz/projekt).

## A.3 Aktivnosti i doprinosi članova

Prema stanju koje je tim prijavio za prvi ciklus, ispunjeni su F-001, F-002 i F-003 (registracija i prijava putem Googlea te odjava), F-005 (pregled i filtriranje igrača) i F-006 (javni pregled i filtriranje događaja). F-004 (unos i uređivanje sportskog profila) djelomično je ispunjen. Opisi su u [analizi funkcijskih zahtjeva](2-Analiza-zahtjeva.md#21-funkcijski-zahtjevi).

Crtica u stupcu sati označava da za aktivnost nema prijavljenog podatka.

| Aktivnost ili rezultat | Član | Prijavljeni sati | Reprezentativni rezultat ili poveznica |
| --- | --- | --- | --- |
| Oblikovanje i izrada zaslona početne stranice, događaja i prijave te priprema prikaza sučelja | LĆ | - | [Prikazi sučelja](https://github.com/MKos-FER/progi_projekt/commit/e667998), [integracija sučelja #86](https://github.com/MKos-FER/progi_projekt/pull/86) |
| Implementacija Google prijave i registracije te povezivanje sučelja s API-jem i šifrarnicima | MK | - | [Google OAuth #76](https://github.com/MKos-FER/progi_projekt/pull/76), [integracija API-ja](https://github.com/MKos-FER/progi_projekt/commit/7a22cbd) |
| Priprema skripti i servisa za automatizirano postavljanje aplikacije | MK | - | [Skripte i upute za postavljanje](../deploy/README.md), [promjena u repozitoriju](https://github.com/MKos-FER/progi_projekt/commit/f4e6810) |
| Izrada početne backend strukture, konfiguracije i pohrane korisnika | FŽ | - | [Backend i baza za korisnike #50](https://github.com/MKos-FER/progi_projekt/pull/50) |
| Razrada obrazaca uporabe i povezivanje s funkcijskim zahtjevima | FŽ | - | [Obrasci uporabe i sljedivost #79](https://github.com/MKos-FER/progi_projekt/pull/79) |
| Implementacija API-ja za događaje i sudjelovanje te API-ja za korisnike i uređivanje profila, s testovima | DL | - | [API događaja #51](https://github.com/MKos-FER/progi_projekt/pull/51), [API korisnika #89](https://github.com/MKos-FER/progi_projekt/pull/89) |
| Migracije baze, šifrarnici i spremanje sportova s razinom iskustva | LB | - | [Početne migracije](https://github.com/MKos-FER/progi_projekt/commit/45bbc44), [šifrarnici](https://github.com/MKos-FER/progi_projekt/commit/9fedfe1), [spremanje sportova](https://github.com/MKos-FER/progi_projekt/commit/08aa60f) |
| Dokumentiranje autentifikacijskog ugovora, arhitekture i UML dijagrama | LB | - | [Ugovor #75](https://github.com/MKos-FER/progi_projekt/pull/75), [arhitektura #88](https://github.com/MKos-FER/progi_projekt/pull/88) |
| Modeliranje događaja i usklađivanje ER dijagrama s migracijama baze | RB | - | [Model događaja](meeting-notes/baza-dogadaji-2026-10-29.md), [usklađivanje modela #52](https://github.com/MKos-FER/progi_projekt/pull/52) |
| Implementacija profilne fotografije i provjera pohrane nakon restarta | RB | - | [Profilna fotografija #77](https://github.com/MKos-FER/progi_projekt/pull/77), [rezultati provjere](profile-photo.md#rezultat-provjere-15-11-2026) |
| Priprema README-a s uputama za postavljanje, opisom tima i izjavom o uporabi umjetne inteligencije | NŠ | - | [Upute za postavljanje](https://github.com/MKos-FER/progi_projekt/commit/8bcb44f), [izjava o uporabi UI-ja](https://github.com/MKos-FER/progi_projekt/commit/fb4202d), [opis tima](https://github.com/MKos-FER/progi_projekt/commit/dc2b314) |

## A.4 Pregled promjena u repozitoriju

[GitHub Contributors](https://github.com/MKos-FER/progi_projekt/graphs/contributors) prikazuje autore i njihovu aktivnost na glavnoj grani. Graf u nastavku generiran je iz Git povijesti grane `master` zaključno s promjenom `c6583ed`. Obuhvaća razdoblje od 8. 10. do 20. 11. 2026. Broji zapise promjena prema datumu autora, bez zapisa spajanja grana i automatiziranih zapisa GitHub Actionsa.

```text
Tjedan od  Zapisi promjena
05. 10.    # 1
12. 10.      0
19. 10.    ### 3
26. 10.    ################ 16
02. 11.    #### 4
09. 11.    ########################## 26
16. 11.    ############################ 28
```

U obuhvaćenom razdoblju zabilježeno je 78 zapisa promjena na glavnoj grani. NŠ je radio na grani [`docs/readme`](https://github.com/MKos-FER/progi_projekt/tree/docs/readme), pa su njegovi rezultati u A.3 vidljivi preko izravnih poveznica na promjene na toj grani. Graf daje pregled ritma promjena. Rad na koordinaciji, pregledu i provjeri vidljiv je i u zapisnicima, opisima zadataka i rezultatima testova.

## A.5 Ključni inženjerski izazovi i rješenja

| Izazov | Poduzeta radnja | Ishod ili preostalo ograničenje |
| --- | --- | --- |
| Dvije istodobne prijave mogle bi zauzeti isto posljednje mjesto na događaju. | Backend u transakciji zaključava zapis događaja prije provjere popunjenosti i upisa sudionika. | Drugi zahtjev čeka na zaključavanje, zatim provjerava osvježeni status i dobiva konflikt ako je događaj popunjen. [Zapis odluke](meeting-notes/baza-dogadaji-2026-10-29.md#2-provjera-proširivosti), [implementacija #51](https://github.com/MKos-FER/progi_projekt/pull/51) |
| Migracija V4 imala je SQL sintaksnu pogrešku, a ogledni podaci koristili su vrijednosti spola koje ograničenje baze odbija. | LB je ispravio V4 i podatke za razvoj te provjerio migracije na privremenim bazama. | Zabilježene su uspješna migracija prazne baze do V6 i nadogradnja postojeće baze s V3, obje uz Flyway `validate`. [Ispravak](https://github.com/MKos-FER/progi_projekt/commit/0cc0122), [rezultati provjere](meeting-notes/korisnici-i-oauth.md#4-provjera-migracija) |
| Obrazac za dovršetak registracije mora spremati identifikatore iz baze, a istodobno korisniku prikazati čitljive nazive gradova, sportova i razina. | LB je dodao API šifrarnike, a MK je povezao obrazac sa šifrarnicima i uskladio ga s modelom podataka korisnika. | Obrazac prikazuje nazive iz API-ja i šalje pripadajuće identifikatore pri registraciji. [API šifrarnika](https://github.com/MKos-FER/progi_projekt/commit/9fedfe1), [integracija obrasca #93](https://github.com/MKos-FER/progi_projekt/pull/93) |
| Pri zamjeni profilne fotografije upis datoteke i upis njezina URL-a u PostgreSQL mogu uspjeti ili zakazati neovisno. | RB je svakoj zamjeni dodijelio novi URL. Stara se datoteka briše nakon potvrde transakcije, a nova se uklanja ako spremanje u bazu ne uspije. | Integracijskim testom potvrđeni su dohvat iste fotografije nakon restarta i uspješna zamjena. Nagli prekid procesa između zapisa datoteke i potvrde transakcije može ostaviti nepovezanu datoteku. [Pohrana i rezultati provjere](profile-photo.md) |
