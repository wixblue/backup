package com.wixblue.backup.model;

import java.nio.file.Path;
import java.util.List;

/** Sammanfattning av en körd backup. */
public record BackupResult(
        String source,
        String destination,
        int copied,
        int updated,
        int skipped,
        int failed,
        List<BackupEntry> entries) {

    public static BackupResult of(Path source, Path destination, List<BackupEntry> entries) {
        return new BackupResult(
                source.toString(),
                destination.toString(),
                count(entries, BackupAction.COPIED),
                count(entries, BackupAction.UPDATED),
                count(entries, BackupAction.SKIPPED),
                count(entries, BackupAction.FAILED),
                List.copyOf(entries));
    }

    private static int count(List<BackupEntry> entries, BackupAction action) {
        return (int) entries.stream().filter(entry -> entry.action() == action).count();
    }
}
