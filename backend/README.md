# Backend

Spring Boot aplikacija nalazi se u `LetsDoSports/` i koristi JDK 23, Maven, Spring Security i Spring JDBC. PostgreSQL shemu održava Flyway iz direktorija `db/`; aplikacija ne stvara tablice. Verzije ovisnosti određene su u [pom.xml](LetsDoSports/pom.xml).

## Što je implementirano

Backend podržava F-005: pregled i kombinirano filtriranje igrača.

| Metoda i putanja | Ponašanje |
| --- | --- |
| `GET /api/v1/users` | Popis korisnika uz opcionalne filtre `sportId`, `cityId`, `ageMin`, `ageMax` i `skillLevelId`. |
| `GET /api/v1/users/{userId}` | Podaci za pregled pojedinog korisnika. |

Svi poslani filtri vrijede zajedno. `skillLevelId` zahtijeva `sportId` i označava točnu razinu iskustva za taj sport. ID-evi šifrarnika imaju raspon 1–32767; dobne granice su nenegativne, uključive i moraju zadovoljiti `ageMin <= ageMax`.

Popis i pojedinačni pregled vraćaju `id`, `username`, `name`, `profilePhotoUrl`, `age`, `city` i `sports`. Vraćaju se svi sportovi korisnika, čak i kada je pretraga filtrirana po jednom sportu. Datum rođenja, spol, e-pošta, OAuth podaci i uloga izostavljaju se. Dob se računa prema datumu u zoni `Europe/Zagreb`.

Bez filtara vraćaju se svi korisnici, uključujući prijavljenog korisnika. Popis je sortiran po ID-u korisnika, sportovi po ID-u sporta; paginacija nije uvedena. Prazan rezultat vraća `200 []`, nevaljan unos `400`, a nepostojeći korisnik `404`. Detalji JSON odgovora opisani su u [API ugovoru, §3.2](../docs/meeting-notes/api-endpoints.md#32-odgovor-za-pregled-igrača-f-005).

Obje rute zahtijevaju autentificiranu sesiju s lokalnim principalom `ApplicationUserPrincipal`; bez nje vraćaju `401`. Stvarna OAuth prijava, uređivanje vlastitog profila (F-004), javni šifrarnici i upload fotografija još nisu implementirani.

## Struktura paketa i odgovornosti

Java source code nalazi se u `LetsDoSports/src/main/java/hr/fer/letsdosports/`. Svaka mapa ispod tog korijena odgovara dijelu Java paketa: primjerice, klasa u `user/controller` pripada paketu `hr.fer.letsdosports.user.controller`.

```text
hr/fer/letsdosports/
├── LetsDoSportsApplication.java
├── api/
│   ├── ApiError.java
│   └── ApiExceptionHandler.java
├── security/
│   ├── ApiSecurityConfiguration.java
│   └── ApplicationUserPrincipal.java
└── user/
    ├── application/
    │   ├── UserSearchService.java
    │   ├── DefaultUserSearchService.java
    │   └── UserSearchRepository.java
    ├── controller/
    │   ├── UserSearchController.java
    │   └── UserSummaryResponse.java
    ├── domain/
    │   ├── UserSummary.java
    │   ├── UserSearchFilter.java
    │   └── exception/
    │       ├── InvalidUserSearchFilterException.java
    │       └── UserNotFoundException.java
    └── jdbc/
        └── JdbcUserSearchRepository.java
```

### Korijenski paket: pokretanje aplikacije

`LetsDoSportsApplication` sadrži `main` metodu i anotaciju `@SpringBootApplication`. To je ulazna točka koja pokreće Spring Boot i otkrivanje komponenti u podpaketima.

Ovdje pripada početna konfiguracija pokretanja. Obrada HTTP zahtjeva, pravila filtriranja i SQL imaju vlastite pakete opisane u nastavku.

### `api`: zajednički HTTP odgovor na pogreške

Ovdje pripadaju dijelovi HTTP ugovora koji su zajednički funkcionalnostima. Trenutačno su to `ApiError`, zapis oblika `{code,message}`, i `ApiExceptionHandler`, globalni obrađivač iznimaka s anotacijom `@RestControllerAdvice`.

Primjer: servis prijavi `UserNotFoundException`, a globalni handler pretvori tu iznimku u HTTP `404` s kodom `USER_NOT_FOUND`. Tako servis može opisati problem, dok HTTP sloj određuje kako ga prikazati klijentu.

Kontroler pretrage pripada `user/controller` jer služi konkretnoj funkcionalnosti. Korijenski `api` sadrži zajedničku obradu. Sigurnosne pogreške koje nastanu prije kontrolera obrađuje konfiguracija u `security`.

### `security`: identitet korisnika i pristup rutama

Ovdje pripadaju konfiguracija Spring Securityja i podaci o autentificiranom identitetu.

`ApiSecurityConfiguration` definira sigurnosni lanac: dopušta implementirane GET rute samo sesiji s odgovarajućim lokalnim principalom, a ostale rute zatvara. Vraća JSON pogreške `401 UNAUTHENTICATED` i `403 ACCESS_DENIED`. CSRF zaštita ostaje uključena; CORS dopušta vjerodajnice samo za izričito konfigurirana ishodišta.

CORS dopušta metode `GET`, `POST`, `PATCH` i `OPTIONS`. Dopuštenje kroz CORS ne otvara rute za pisanje; njihovi kontroleri i pravila pristupa još nisu implementirani.

`ApplicationUserPrincipal` predstavlja lokalni identitet i sadrži `localUserId`. Principal odgovara na pitanje „tko šalje zahtjev?”. ID u putanji `/users/{userId}` određuje čiji se profil pregledava; sam po sebi ne dokazuje identitet pošiljatelja.

Buduća OAuth integracija treba provjeriti vanjski identitet, odrediti lokalni račun, zaštititi sesiju pri prijavi i spremiti sigurnosni kontekst u sesiju. Trenutačno nema form/basic ni razvojnog logina. Poslovna pravila pretrage pripadaju korisničkom modulu.

### `user`: cjelina vezana uz korisnike

Ovo je paket funkcionalnosti koji okuplja kod vezan uz korisnike. Podjela prvo po funkcionalnosti omogućuje da se korisnički kod čita na jednom mjestu, a unutarnji podpaketi razdvajaju njegove odgovornosti.

Trenutačno sadrži F-005. Naziv `UserSummary` naglašava da se za pregled dohvaća ograničen skup korisničkih podataka, bez privatnih polja.

### `user/controller`: HTTP ulaz i izlaz

Ovdje pripadaju kontroleri korisničkih ruta i njihovi request/response DTO-i. DTO je objekt koji određuje oblik podataka razmijenjenih preko API-ja.

`UserSearchController` čita query i path parametre, provjerava njihov oblik Jakarta Validation anotacijama te poziva `UserSearchService`. `UserSummaryResponse` definira JSON odgovor i mapira podatke domenskog modela u taj odgovor.

Primjer: `ageMin=18` prvo se pretvara u cijeli broj i provjerava kao nenegativna vrijednost. Pravilo da iskustvo zahtijeva odabrani sport provjerava se i kroz servis, kako bi vrijedilo za svaki način pozivanja. Kontroler ne izvršava SQL.

### `user/application`: provedba slučaja uporabe

Ovdje pripadaju servisni ugovori, njihova provedba, koordinacija domenskih pravila i transakcijske granice.

`UserSearchService` je ulazni port: sučelje koje opisuje što modul nudi pozivatelju, ovdje pretragu i dohvat po ID-u. `DefaultUserSearchService` ga implementira. Provjerava filtre, određuje zajednički referentni datum za dob i otvara transakciju `@Transactional(readOnly = true)`. Za nepostojećeg korisnika baca domensku iznimku.

`UserSearchRepository` je izlazni port: sučelje koje opisuje koje podatke servis treba dohvatiti. Servis ovisi o tom sučelju, a Spring mu konstruktorom predaje JDBC implementaciju. Zato servisni test može koristiti zamjenski repozitorij bez pokretanja baze.

Port je ugovor, a adapter je konkretna veza s vanjskim svijetom. Zato je repozitorijsko sučelje ovdje, dok je SQL implementacija u `user/jdbc`.

### `user/domain`: podaci i pravila korisničkog područja

Ovdje pripadaju modeli i pravila koja imaju značenje u funkcionalnosti, neovisno o HTTP-u i načinu pohrane.

`UserSummary` opisuje korisnika za pregled, uključujući grad i sportove. To je Java model podataka; nema JPA mapiranja. `UserSearchFilter` opisuje kriterije i kroz `validate()` provjerava njihove raspone i odnose.

Primjer domenskog pravila je „razina iskustva mora se odnositi na odabrani sport”. Takvo pravilo treba vrijediti i kada servis pozove neki drugi dio aplikacije, a ne samo HTTP kontroler. Ovaj paket ne ovisi o Spring MVC-u ni JDBC-u.

### `user/domain/exception`: očekivane domenske pogreške

Ovdje pripadaju iznimke koje imenuju problem u korisničkoj funkcionalnosti. `InvalidUserSearchFilterException` označava nevaljane kriterije, a `UserNotFoundException` da traženi korisnik ne postoji.

Iznimka opisuje problem, dok `api/ApiExceptionHandler` određuje HTTP status i JSON. Primjerice, rezultat pretrage bez podudaranja običan je prazan popis; dohvat pojedinog nepostojećeg korisnika završava iznimkom.

### `user/jdbc`: pristup PostgreSQL bazi

Ovdje pripadaju SQL upiti, vezanje parametara i pretvorba redaka baze u domenske objekte.

`JdbcUserSearchRepository` implementira `UserSearchRepository` pomoću `NamedParameterJdbcTemplate`. Vrijednosti filtara šalje kao parametre upita. Sport i iskustvo provjerava zajedno kroz `EXISTS`, a zasebnim spajanjima dohvaća sve sportove korisnika. Više SQL redaka zatim objedini u jedan `UserSummary`.

Ovaj adapter poznaje tablice i stupce. HTTP statusi i JSON oblik odgovora odgovornost su HTTP sloja.

### Kako zahtjev prolazi kroz te pakete

Za zahtjev `GET /api/v1/users?sportId=1&skillLevelId=2` tok je:

```text
security → user/controller → user/application → user/jdbc → PostgreSQL
```

Sigurnosni lanac provjerava sesiju. Kontroler čita parametre, servis provjerava kriterije i poziva repozitorijski port, a JDBC adapter izvršava upit. Podaci se vraćaju kao `UserSummary`, koje kontroler pretvara u `UserSummaryResponse`; Spring ih serijalizira u JSON. Ako nastane očekivana domenska iznimka, `api` je pretvara u dogovoreni odgovor.

Takvo razdvajanje slijedi heksagonalni pristup: aplikacijska i domenska pravila nalaze se u sredini, a HTTP i baza povezani su adapterima preko sučelja.

## Testiranje

Naredbe u ovom poglavlju pokreću se iz **korijena repozitorija** u PowerShellu. Jednom po terminalu postaviti JDK 23:

```powershell
$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\openjdk-23.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Putanja odgovara lokalnoj instalaciji; na drugom računalu prilagoditi `JAVA_HOME`.

### Brzi testovi tijekom razvoja

```powershell
Push-Location .\backend\LetsDoSports
try {
    .\mvnw.cmd test
} finally {
    Pop-Location
}
```

Ako wrapper nije dostupan, koristiti `mvn test`. Baza i Docker nisu potrebni. Test konteksta očekivano se preskače bez `LDS_INTEGRATION_TESTS=true`. Za samo HTTP testove umjesto `test` koristiti `'-Dtest=UserSearchControllerTest' test`.

| Test | Što provjerava |
| --- | --- |
| `DefaultUserSearchServiceTest` | Pravila filtara i ponašanje servisa uz zamjenski repozitorij. |
| `UserSearchControllerTest` | HTTP ugovor, validaciju, sesije, privatnost odgovora, CSRF i CORS uz zamjenski repozitorij. |
| `JdbcUserSearchRepositoryIT` | Stvarne PostgreSQL upite, kombinacije filtara, dobne granice i put od HTTP-a do baze. |
| `LetsDoSportsApplicationTests` | Podizanje Spring konteksta kada je omogućena integracijska provjera. |

Testovi se nalaze u `src/test/java/hr/fer/letsdosports/`, odvojeno od produkcijskog koda.

### Cjelovita provjera s bazom

Pokrenuti Docker Desktop i osigurati da je `mvn` na `PATH`, zatim:

```powershell
.\backend\LetsDoSports\test-user-persistence.ps1 -JdkHome $env:JAVA_HOME
```

Skripta sama postavlja testne varijable i stvara izoliranu bazu pomoću [compose.test.yml](../db/compose.test.yml). Provjerava nadogradnju prazne sheme V3 do V5 i migracije na svježoj bazi, dvostruko izvršavanje seeda, `mvn -Pintegration verify` i trajnost nakon restarta baze. Na kraju uklanja vlastite testne kontejnere i volumen te vraća prethodne varijable okruženja. Ne koristi razvojni `db/.env` ni razvojni volumen.

Očekivani završetak je `BUILD SUCCESS` za oba scenarija, `upgrade` i `fresh`. Stvarna Google prijava nije obuhvaćena; HTTP testovi koriste testnu sesiju s lokalnim principalom.

## Pokretanje aplikacije

Za pokretanje aplikacije potrebna je razvojna baza. Pripremiti `db/.env` prema [uputama baze](../db/README.md) i primijeniti migracije. Iz korijena repozitorija:

```powershell
Push-Location .\db
try {
    docker compose up -d
    docker compose logs migrate
} finally {
    Pop-Location
}
```

Nastaviti kada migracije uspješno završe. Aplikacija učitava lokalni `db/.env` kada je radni direktorij korijen repozitorija ili `backend/LetsDoSports`. Koristi `POSTGRES_DB`, `APP_DB_USER` i `APP_DB_PASSWORD`, uz adresu `localhost:5432`.

U IntelliJ-u pokrenuti `LetsDoSportsApplication` uz JDK 23 i jedan od tih radnih direktorija. Nakon promjene `.env` ponovno pokrenuti aplikaciju. Vrijednosti u `.env` trebaju biti obične `KEY=value` vrijednosti bez navodnika; datoteka se čita kao Java properties.

Iz PowerShella, nakon postavljanja JDK-a kao u poglavlju o testiranju:

```powershell
Push-Location .\backend\LetsDoSports
try {
    mvn spring-boot:run
} finally {
    Pop-Location
}
```

Za drugu bazu ili pokretanje izvan tih direktorija postaviti `LDS_DB_URL`, `LDS_DB_USER` i `LDS_DB_PASSWORD` u okruženju procesa (u IntelliJ-u: Run → Edit Configurations → Environment variables). Te varijable imaju prednost pred lokalnim `.env`:

```powershell
$env:LDS_DB_URL = 'jdbc:postgresql://localhost:5432/lets_do_sports'
$env:LDS_DB_USER = 'lds_app'
$env:LDS_DB_PASSWORD = '<lozinka aplikacijskog korisnika>'
```

Postaviti ih prije pokretanja Maven naredbe. URL mora počinjati s `jdbc:postgresql://`; pogreška `'url' must start with "jdbc"` upućuje na neispravnu vrijednost `LDS_DB_URL`. Vjerodajnice ne spremati u repozitorij. Aplikaciju zaustaviti s `Ctrl+C`.

`src/main/resources/application.properties` povezuje te varijable s konfiguracijom aplikacije. `LDS_CORS_ALLOWED_ORIGINS` opcionalno određuje dopuštena frontend ishodišta; zadane vrijednosti su `http://localhost:5173` i `http://127.0.0.1:5173`.

Pokrenuta aplikacija i dalje zahtijeva valjanu sesiju za korisničke rute. Dok OAuth integracija nije povezana, anonimni pregled tih ruta vraća `401`.
