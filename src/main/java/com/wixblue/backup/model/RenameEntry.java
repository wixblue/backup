package com.wixblue.backup.model;

/**
 * Namnbyte för en enskild bild.
 *
 * @param from    nuvarande filnamn
 * @param to      nytt filnamn
 * @param status  vad som hände med bilden
 * @param message felmeddelande, bara satt när status är {@link RenameStatus#FAILED}
 */
public record RenameEntry(String from, String to, RenameStatus status, String message) {
}
