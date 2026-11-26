package com.wixblue.backup.model;

/** Vad som hände, eller skulle hända, med en bild vid namnbyte. */
public enum RenameStatus {
    /** Förhandsvisning: bilden skulle få nytt namn men inget har ändrats. */
    PLANNED,
    /** Bilden har fått nytt namn. */
    RENAMED,
    /** Bilden gick inte att döpa om. Anledningen står i meddelandet. */
    FAILED
}
