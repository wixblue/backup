package com.wixblue.backup.model;

import java.nio.file.Path;
import java.util.List;

/** Sammanfattning av ett namnbyte av bilder i en mapp. */
public record RenameResult(
        String directory,
        boolean dryRun,
        int planned,
        int renamed,
        int failed,
        List<RenameEntry> entries) {

    public static RenameResult of(Path directory, boolean dryRun, List<RenameEntry> entries) {
        return new RenameResult(
                directory.toString(),
                dryRun,
                count(entries, RenameStatus.PLANNED),
                count(entries, RenameStatus.RENAMED),
                count(entries, RenameStatus.FAILED),
                List.copyOf(entries));
    }

    private static int count(List<RenameEntry> entries, RenameStatus status) {
        return (int) entries.stream().filter(entry -> entry.status() == status).count();
    }
}
