package com.wixblue.backup.model;

/** Vad som hände med en enskild fil under en backup. */
public enum BackupAction {
    /** Filen fanns inte i backupen och har kopierats dit. */
    COPIED,
    /** Filen fanns i backupen men med annat innehåll och har skrivits över. */
    UPDATED,
    /** Filen fanns i backupen men med annat innehåll. Den skrevs inte över eftersom läget bara lägger till saknade filer. */
    NOT_UPDATED,
    /** Filen fanns redan i backupen med samma innehåll. */
    SKIPPED,
    /** Filen kunde inte kopieras. */
    FAILED
}
