# Projektkontext

Du hjälper mig bygga ett **backupsystem för bilder** i repot https://github.com/wixblue/backup.

## Mål
- Kopiera bilder från en källmapp till en backupmapp.
- Avgöra om en fil redan finns i backupen och om källan är en nyare/ändrad version.
- Kunna jämföra källa mot backup och rapportera skillnader (saknas, ändrad, extra, identisk).

## Teknikstack
- Java 21 (använd moderna språkfunktioner, se nedan), Spring Boot, Maven.
- Tester: JUnit 5, AssertJ, Mockito endast där det behövs. Använd `@TempDir` för filsystemstester.
- Kodtäckning: JaCoCo, med krav på minst 90 % rad- och branch-täckning i `mvn verify`.

## Kodprinciper
- **Läsbarhet först.** Enkel kod slår smart kod. Små klasser och korta metoder med tydliga namn.
- Använd moderna Java-funktioner när de gör koden enklare:
  records, sealed interfaces, pattern matching (`instanceof`, `switch`), switch-uttryck,
  `var` där typen är uppenbar, text blocks, `Optional` som returvärde, Stream API med måtta,
  `java.nio.file` (`Path`, `Files`), virtual threads vid parallell filhantering.
- Oföränderliga datatyper som standard. Resultat modelleras som records eller sealed types
  (t.ex. `CompareResult`: `Identical`, `Modified`, `MissingInBackup`, `ExtraInBackup`).
- Konstruktorinjektion, inga `@Autowired` på fält.
- Inga onödiga abstraktioner, ramverk eller beroenden. Fråga innan du lägger till ett nytt beroende.
- Kommentarer förklarar *varför*, inte *vad*. Namn ska göra resten.
- Paketstruktur per funktion (t.ex. `backup`, `compare`, `scan`), inte per teknisk lager.
- Tydlig felhantering med egna, beskrivande exceptions. Svälj aldrig fel tyst.

## Domänregler för bilder
- Jämförelse av filer: först storlek + senast ändrad (snabbt), sedan hash (SHA-256) när det behövs.
- Hantera vanliga bildformat (jpg, jpeg, png, heic, gif, tiff, raw m.fl.), konfigurerbart.
- Bevara mappstruktur och ändringstid vid kopiering.
- Skriv aldrig över eller radera originalfiler. Källan är skrivskyddad i all logik.
- Stöd `dry-run` så att jag kan se vad som skulle hända utan att något kopieras.

## Tester
- Varje ny klass och metod med logik ska ha tester. Testa gränsfall (tom mapp, dubbletter,
  filer med samma namn men olika innehåll, saknade rättigheter, mycket stora filer).
- Skriv testerna först eller tillsammans med koden (TDD där det är rimligt).
- Tester ska vara snabba, oberoende och läsbara (given/when/then, tydliga testnamn).
- Kör `mvn clean verify` och bekräfta att allt är grönt och att täckningskravet uppfylls innan du säger att något är klart.

## Arbetssätt (Git)
- Pusha aldrig direkt till `main`. Allt går via pull requests.
- Skapa en egen gren per delmoment (t.ex. `feature/file-scanner`) och öppna en liten PR per delmoment.
- Tydliga commit-meddelanden i imperativ ("Lägg till hashberäkning av filer").
- PR-beskrivning: vad som ändrats, varför, och hur det testats.

## Så här vill jag att du jobbar
1. Läs README och befintlig kod innan du föreslår något.
2. Föreslå en kort plan med små delmoment innan du börjar koda på något större.
3. Gör ett delmoment i taget. Visa vad du ändrat och varför, kort och konkret.
4. Fråga om något är otydligt i stället för att gissa.
5. Svara på svenska. Kod, klass- och metodnamn skrivs på engelska.

## Förslag på delmoment
1. Projektskelett: Spring Boot, Maven, JaCoCo med täckningskrav, CI-workflow.
2. Skanna en mapp efter bildfiler (`FileScanner`).
3. Beräkna filmetadata och hash (`FileFingerprint`).
4. Avgöra om en fil är ny, ändrad eller identisk.
5. Kopiera filer till backupen (med dry-run).
6. Jämför källa mot backup och skapa en rapport.
7. CLI/REST-gränssnitt och konfiguration via `application.yml`.