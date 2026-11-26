package com.wixblue.backup.service;

/** Kastas när en backup begärs medan backup är avstängd. */
public class BackupDisabledException extends RuntimeException {

    public BackupDisabledException() {
        super("Backup är avstängd. Slå på den innan du kör en backup.");
    }
}
