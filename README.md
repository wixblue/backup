# Projektnamn: Backup

Skapa backup system som kopierar filer.
Ska kunna kontrollera om fil redan finns, om det är uppdaterad version.
Ska ge stöd för att kunna jämföra filer från destination till backup.

## Teknikstack

- Java 21
- Spring Boot
- Maven

## Kom igång

Bygg och kör lokalt:

```
mvn clean verify
mvn spring-boot:run
```

## Arbetssätt

- Ingen kod pushas direkt till `main`. Allt går via pull requests.
- Arbeta i egna grenar och öppna en PR per delmoment.
- Håll PR:erna små och med tester där det är rimligt.
