package com.wixblue.backup.model;

/**
 * Resultatet för en enskild fil.
 *
 * @param path    sökväg relativt källmappen, alltid med snedstreck som avgränsare
 * @param action  vad som hände med filen
 * @param message felmeddelande, bara satt när action är {@link BackupAction#FAILED}
 */
public record BackupEntry(String path, BackupAction action, String message) {
}
