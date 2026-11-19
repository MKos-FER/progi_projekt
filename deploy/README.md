# Skripte za CI/CD

## Automatsko postavljanje

```bash
make setup DEPLOY_USER=deploy
```
> [!CAUTION]
> Ova make skripta kreira user-a, postavlja mu permissions, postavlja systemd servise, kreira direktorije i instalira razne softverske pakete!
>
> Također **briše** datoteke definirane varijablama `SUDOERS_FILE` i `CADDY_FILE`, zadane vrijednosti su `/etc/sudoers.d/letsdosports` i `/etc/caddy/Caddyfile`

Ako želite mijenjati zadane vrijednosti, samo je potrebno definirati ih nakon naredbe:
```bash
make setup DEPLOY_USER=andrija PROJECT_DIR=/opt/PROGI_projekt DEPLOY_DIR=./deploy
```

Lista varijabli koje je moguće podesiti:
| Ime varijable | opis                                                                              | zadana vrijednost                |
| -             | -                                                                                 | -                                |
| DEPLOY_USER   | ime linux usera koji će se kreirati (ako ne postoji) s ovlastima za deployment    | deploy                           |
| DEPLOY_DIR    | direktorij u koji će se staviti potrebne deploy skripte koje će servisi pokretati | /home/{DEPLOY_USER}/deploy       |
| PROJECT_DIR   | direktorij u koji će se klonirati git repozitorij projekta                        | /opt/projekt                     |
| FRONTEND_DIR  | direktorij u kojem će se servirati frontend aplikacije                            | /var/www/lets-do-sports          |
| WEBHOOK_PORT  | port na kojem sluša `git_webhook` program                                         | 5999                             |
| JAVA_HOME     | putanja do java instalacije                                                       | /lib/jvm/default                 |
| SYSTEMD_DIR   | direktorij u koji će se spremati systemd servisne datoteke                        | /etc/systemd/system              |
| SUDOERS_FILE  | datoteka u koju će se upisati permission za neovlašteno restartanje backenda      | /etc/sudoers.d/letsdosports      |
| CADDY_FILE    | datoteka u koju se sprema konfiguracija za `caddy`                                | /etc/caddy/Caddyfile             |
| BUN           | putanja do `bun` programa                                                         | /home/{DEPLOY_USER}/.bun/bin/bun |
| GO            | putanja do `go` programa                                                          | go                               |
| SUDO          | putanja do `sudo` programa                                                        | sudo                             |


## Ručno postavljanje
Svi systemd servisi i sudo konfiguracija pretpostavljaju da se linux user koji pokreće aplikaciju zove `mateo`.
Ukoliko želite da se user drugačije zove, pokrenite make skriptu ili ručno mijenjajte u datotekama (nema puno pojavljivanja)

Još jedna pretpostavka je da se deploy folder nalazi na `/opt/PROGI_deployment` a git repozitorij projekta na `/opt/PROGI_projekt`

Prvo je potrebno postaviti sve datoteke:
```bash
make
cp ./systemd-services/PROGI-github-webhook.service /etc/systemd/system/
cp ./systemd-services/PROGI-deploy-backend.service /etc/systemd/system/
cp ./systemd-services/PROGI-deploy-frontend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable PROGI-github-webhook.service
sudo systemctl enable PROGI-deploy-frontend.service
sudo systemctl enable PROGI-deploy-backend.service
```

Skrivena .env datoteka:
```.env
PORT=5999
GITHUB_WEBHOOK_SECRET={nasumicno generiran niz slova i brojeva}
XDG_RUNTIME_DIR=/run/user/1000
DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/1000/bus
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock
JAVA_HOME=/opt/jvm/jdk-23.0.2+7
```

Isti github webhook secret koji se generira potrebo ne postaviti na stranici.

Otvoriti `sudo visudo -f /etc/sudoers.d/letsdosports` i dodati liniju
```sudo
mateo ALL=(root) NOPASSWD: /usr/bin/systemctl restart PROGI-deploy-backend.service
```

Postaviti ispravne ovlasi:
```bash
sudo chmod 0440 /etc/sudoers.d/letsdosports
```

I provjeriti da je sudo pročitao taj novi file:
```bash
sudo visudo -c
```

Konfiguracijska datoteka caddy servera `/etc/caddy/Caddyfile`:
```caddy
letsdosports.eu www.letsdosports.eu {
        root * /var/www/lets-do-sports

        encode zstd gzip

        # Github webhook redirect
        @webhook path /github_webhook

        handle @webhook {
                reverse_proxy 127.0.0.1:5999
        }

        # Backend API
        handle /api/* {
                reverse_proxy http://127.0.0.1:8080
        }

        # OAuth endpoints
        handle /oauth2/* {
                reverse_proxy http://127.0.0.1:8080
        }

        # Frontend
        handle {
                try_files {path} /index.html
                file_server
        }
}
```
Nakon postavljanja datoteke potrebno je u istom folderu napraviti `caddy reload` kako bi se konfiguracija primjenila.

Nakon toga potrebno je pokrenuti naredbe:
```bash
sudo apt update && sudo apt install inotify-tools rsync -y
mkdir -p /opt/PROGI_deployment/frontend_staging/
sudo systemctl start PROGI-github-webhook.service
sudo systemctl start PROGI-deploy-frontend.service
# Nije potrebno startat deploy-backend jer to zapravo pokreće backend proces i riješeno je u deploy.sh skripti
```

Sada je CI/CD postavljen i automatski pull-a promjene s `master` grane te ih stavlja u produkciju.



# Struktura

```bash
/opt/PROGI_deployment/
|-- .env
|-- Makefile
|-- README.md
|-- deploy.sh
|-- deploy_frontend.sh
|-- frontend_staging/
|-- git_webhook
|-- systemd-services/
|   |-- PROGI-deploy-backend.service
|   |-- PROGI-deploy-frontend.service
|   `-- PROGI-github-webhook.service
`-- webhook_handler.go
```

`Makefile`, generira `git_webhook` iz `webhook_handler.go`.

`git_webhook` je program koji sluša na portu (konfiguriranom u `.env` datoteci) dok od github-a ne dobije informaciju da se na branch-u `master` dogodio push. I kada prepozna da se dogodio push, pokreće skriptu `deploy.sh`.

`deploy.sh` je skripta koja kada ju se pokrene, prebaci se u `/opt/PROGI_projekt` (gdje treba biti kloniran repozitorij projekta) i pull-a sve promjene na masteru (odbacivajući lokalne promjene ako postoje) i onda ako ima novih promjena (ako se commit hash promjeni) radi sljedeće korake:

1. builda frontend aplikaciju i kopira ju u `/opt/PROGI_deployment/frontend_staging/` i dodatno kreira (tj. ako postoji modificira) datoteku `.deploy_ready`
1. pokrene bazu i migracije
1. pokrene `PROGI-deploy-backend` servis (za pokretanje servisa potreban je sudo, zato se u sudoconfig-u dodana linija za dopuštanje ove naredbe bez pitanja za password)

`deploy-frontend.sh` je skripta koju pokreće `PROGI-deploy-frontend` servis kao root user i prati promjene u `/opt/PROGI_deployment/frontend_staging/` direktoriju dok se ne kreira ili promjeni `.deploy_ready` datoteka. Kada se datoteka promijeni, kopira se sadržaj direktorija `frontend_staging` u direktorij `/var/www/lets-do-sports` (što je vrijednost varijable okruženja postavljene servisom). Ta radnja kopiranja radi se kao `root` i to je glavni razlog zašto je potreban `root` access.

`frontend_staging/` je prazan direktorij u koji se sprema build frontend-a kako bi se mogao kopirati u zaštićeni direktorij kada je gotov.

## Servisi

`PROGI-deploy-backend` - pokreće backend (kao user `mateo`) unutar servisa
`PROGI-deploy-frontend` - pokreće `deploy_frontend.sh` skriptu unutar servisa za lakši pregled log-ova i automatsko paljenje nakon reboot-a sustava
`PROGI-github_webhook` - pokreće `git_webhook` program (kao user `mateo`) unutar servise za laši pregled log-ova i automatsko paljenje nakon reboot-a sustava
