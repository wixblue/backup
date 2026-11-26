package com.wixblue.backup.web;

import jakarta.validation.constraints.NotBlank;

/**
 * @param directory mappen med bilder som ska döpas om
 * @param dryRun    om sant (standard) visas bara vad som skulle ändras. Sätt {@code false} för att döpa om på riktigt.
 */
public record RenameRequest(@NotBlank String directory, Boolean dryRun) {

    public boolean dryRunOrDefault() {
        return dryRun == null || dryRun;
    }
}
