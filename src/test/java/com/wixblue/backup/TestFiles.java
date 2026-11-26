package com.wixblue.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Hjälpmetoder för tester som skapar filer. */
public final class TestFiles {

    private TestFiles() {
    }

    /** Skriver en textfil och skapar mappar som saknas. */
    public static Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    /** Skriver en textfil med en bestämd senast-ändrad-tid i systemets tidszon. */
    public static Path writeModifiedAt(Path file, String content, LocalDateTime modified) throws IOException {
        write(file, content);
        Files.setLastModifiedTime(file, FileTime.from(modified.atZone(ZoneId.systemDefault()).toInstant()));
        return file;
    }
}
