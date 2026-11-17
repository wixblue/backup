package com.wixblue.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Hjälpmetoder för tester som skapar filer. */
public final class TestFiles {

    private TestFiles() {
    }

    /** Skriver en textfil och skapar mappar som saknas. */
    public static Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }
}
