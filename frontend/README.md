# This is where the frontend code lives

Quick start:
```bash
bun install
bun run dev
```

## Dev-env

Ova konfiguracija koristi Vite za razvojni server i za kreiranje datoteka u produkciji.

Vite server pokreće se s gore navedenom `bun run dev` naredom ili skraćeno `bun dev`.

## Priprema za produkciju

Naredbom
```bash
bun prod
```
kreira se direktorij `./dist/` i u njega se stavljaju statične datoteke spremne za 
hosting na web serveru. 

Kako bi se provjerila ispravnost statičnih datoteka potrebno je pokrenuti:
```bash
bun preview
```
Ova naredba kreira privremeni web server s datotekama iz tog direktorija.

## Profilna fotografija (PROFILE-05)

Komponenta `src/profile/ProfilePhoto.tsx` prikazuje opcionalnu fotografiju, omogućuje upload/zamjenu te prikazuje greške validacije. Vite prosljeđuje `/api` na backend na portu 8080. Konfiguracija razvojnog identiteta, sesije i trajne pohrane opisana je u [uputama](../docs/profile-photo.md).
