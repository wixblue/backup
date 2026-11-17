package com.wixblue.backup.model;

/** Hur en fil förhåller sig mellan källmappen och backupen. */
public enum ComparisonStatus {
    /** Filen finns på båda ställena med samma innehåll. */
    IDENTICAL,
    /** Filen finns på båda ställena men innehållet skiljer sig. */
    DIFFERENT,
    /** Filen finns bara i källmappen, alltså inte säkerhetskopierad ännu. */
    ONLY_IN_SOURCE,
    /** Filen finns bara i backupen, till exempel för att den har tagits bort från källan. */
    ONLY_IN_BACKUP
}
