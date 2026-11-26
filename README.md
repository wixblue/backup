# Projektnamn: Backup

Skapa backup system som kopierar filer.
Ska kunna kontrollera om fil redan finns, om det är uppdaterad version.
Ska ge stöd för att kunna jämföra filer från destination till backup.

## Teknikstack

- Java 25
- Spring Boot 4
- Maven

## Kom igång

Bygg och kör lokalt:

```
mvn clean verify
mvn spring-boot:run
```

## Användning

Applikationen exponerar ett litet REST-API på `http://127.0.0.1:8080`. Det binds bara till lokala anslutningar eftersom det kan läsa och skriva godtyckliga mappar.

### Ta backup

```
curl -X POST http://127.0.0.1:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{"source": "/sökväg/till/källa", "destination": "/sökväg/till/backup", "mode": "ADD_MISSING"}'
```

`mode` styr hur filer som redan finns i backupen behandlas. Utelämnas den används `ADD_MISSING`.

- `ADD_MISSING` – bara filer som saknas i backupen läggs till. Ändrade filer lämnas orörda.
- `ADD_AND_UPDATE` – saknade filer läggs till och ändrade filer skrivs över med den nya versionen.

Backupen går igenom källmappen rekursivt och för varje fil gäller:

| Läge | Resultat (`action`) |
|------|---------------------|
| Filen finns inte i backupen | `COPIED` – filen kopieras |
| Filen finns men innehållet skiljer sig, läge `ADD_AND_UPDATE` | `UPDATED` – filen skrivs över med den nya versionen |
| Filen finns men innehållet skiljer sig, läge `ADD_MISSING` | `NOT_UPDATED` – inget görs, backupens version behålls |
| Filen finns med samma innehåll | `SKIPPED` – inget görs |
| Filen kunde inte kopieras | `FAILED` – anledningen står i `message`, övriga filer påverkas inte |

Innehållet jämförs byte för byte, så en ändring upptäcks även om filstorleken är oförändrad. Filer som bara finns i backupen tas inte bort. Symlänkar hoppas över. Backupmålet får inte ligga i källmappen.

### Stäng av och slå på backup

Backup kan stängas av. Då nekas `/api/backup` med `409` tills den slås på igen. Jämförelse fungerar som vanligt.

```
curl http://127.0.0.1:8080/api/backup/enabled

curl -X PUT http://127.0.0.1:8080/api/backup/enabled \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

Startläget styrs av `backup.enabled` i `application.properties` (standard `true`). Läget sparas bara i minnet, så efter en omstart gäller startläget igen.

### Jämför källa mot backup

```
curl -X POST http://127.0.0.1:8080/api/compare \
  -H "Content-Type: application/json" \
  -d '{"source": "/sökväg/till/källa", "backup": "/sökväg/till/backup"}'
```

Jämförelsen ändrar ingenting och ger en status per fil (`status`):

- `IDENTICAL` – finns på båda ställena med samma innehåll
- `DIFFERENT` – finns på båda ställena men innehållet skiljer sig
- `ONLY_IN_SOURCE` – finns bara i källan, alltså inte säkerhetskopierad ännu
- `ONLY_IN_BACKUP` – finns bara i backupen, till exempel borttagen från källan

`notBackedUp` är antalet filer som inte är säkerhetskopierade: filer som saknas i backupen (`onlyInSource`) plus filer vars innehåll skiljer sig (`different`). `backupUpToDate` är `true` när `notBackedUp` är `0`. Filer som bara finns i backupen påverkar inte det värdet.

Vill du backa upp efter jämförelsen anropar du `/api/backup` med samma mappar. Vill du bara lägga till saknade filer använder du läget `ADD_MISSING`.

Felaktiga indata, till exempel en mapp som inte finns, ger `400` med förklaring i `detail`.

### Backupprojekt

Ett projekt har namn, beskrivning (valfri), bildmapp (`source`) och backupmapp (`destination`). Projekten sparas i en lokal H2-databas i filen `data/backup.mv.db` (mappen `data/` ignoreras av git).

| Metod | Sökväg | Gör |
|-------|--------|-----|
| `GET` | `/api/projects` | Listar projekt sorterade på namn |
| `GET` | `/api/projects/{id}` | Hämtar ett projekt (`404` om det saknas) |
| `POST` | `/api/projects` | Skapar ett projekt (`201`) |
| `PUT` | `/api/projects/{id}` | Ändrar ett projekt |
| `DELETE` | `/api/projects/{id}` | Tar bort ett projekt (`204`) |

```
curl -X POST http://127.0.0.1:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name": "Semester", "description": "Bilder 2026", "source": "/sökväg/till/bilder", "destination": "/sökväg/till/backup"}'
```

Backupmappen får inte ligga i bildmappen. Mapparna behöver inte finnas när projektet sparas, de kontrolleras först när backup eller jämförelse körs.

## Arbetssätt

- Ingen kod pushas direkt till `main`. Allt går via pull requests.
- Arbeta i egna grenar och öppna en PR per delmoment.
- Håll PR:erna små och med tester där det är rimligt.
