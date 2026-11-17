package com.wixblue.backup.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Sammanfattning av en jämförelse mellan källmapp och backup.
 *
 * @param backupUpToDate true om inga filer skiljer sig och inga filer saknas i backupen.
 *                       Filer som bara finns i backupen räknas inte som att backupen är inaktuell.
 */
public record ComparisonResult(
        String source,
        String backup,
        int identical,
        int different,
        int onlyInSource,
        int onlyInBackup,
        boolean backupUpToDate,
        List<ComparisonEntry> entries) {

    public static ComparisonResult of(Path source, Path backup, List<ComparisonEntry> entries) {
        int different = count(entries, ComparisonStatus.DIFFERENT);
        int onlyInSource = count(entries, ComparisonStatus.ONLY_IN_SOURCE);
        return new ComparisonResult(
                source.toString(),
                backup.toString(),
                count(entries, ComparisonStatus.IDENTICAL),
                different,
                onlyInSource,
                count(entries, ComparisonStatus.ONLY_IN_BACKUP),
                different == 0 && onlyInSource == 0,
                List.copyOf(entries));
    }

    private static int count(List<ComparisonEntry> entries, ComparisonStatus status) {
        return (int) entries.stream().filter(entry -> entry.status() == status).count();
    }
}
