package com.wixblue.backup.model;

/** Hur en backup ska behandla filer som redan finns i backupen. */
public enum BackupMode {
    /** Bara filer som saknas i backupen läggs till. Filer som finns lämnas orörda. */
    ADD_MISSING,
    /** Saknade filer läggs till och filer med ändrat innehåll skrivs över. */
    ADD_AND_UPDATE
}
