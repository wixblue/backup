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
  -d '{"source": "/sökväg/till/källa", "destination": "/sökväg/till/backup"}'
```

Backupen går igenom källmappen rekursivt och för varje fil gäller:

| Läge | Resultat (`action`) |
|------|---------------------|
| Filen finns inte i backupen | `COPIED` – filen kopieras |
| Filen finns men innehållet skiljer sig | `UPDATED` – filen skrivs över med den nya versionen |
| Filen finns med samma innehåll | `SKIPPED` – inget görs |
| Filen kunde inte kopieras | `FAILED` – anledningen står i `message`, övriga filer påverkas inte |

Innehållet jämförs byte för byte, så en ändring upptäcks även om filstorleken är oförändrad. Filer som bara finns i backupen tas inte bort. Symlänkar hoppas över. Backupmålet får inte ligga i källmappen.

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

`backupUpToDate` är `true` när inga filer skiljer sig och inga filer saknas i backupen. Filer som bara finns i backupen påverkar inte det värdet.

Felaktiga indata, till exempel en mapp som inte finns, ger `400` med förklaring i `detail`.

## Arbetssätt

- Ingen kod pushas direkt till `main`. Allt går via pull requests.
- Arbeta i egna grenar och öppna en PR per delmoment.
- Håll PR:erna små och med tester där det är rimligt.
