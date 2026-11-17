package com.wixblue.backup.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Gemensamma filhjälpmedel för backup och jämförelse. */
final class FileSupport {

    private FileSupport() {
    }

    /** Kräver att sökvägen är en befintlig mapp och returnerar dess verkliga sökväg. */
    static Path requireDirectory(Path path, String description) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException(description + " finns inte eller är inte en mapp: " + path);
        }
        return path.toRealPath();
    }

    /**
     * Gör en sökväg absolut och normaliserad, med symlänkar upplösta för den del som finns.
     * Fungerar även när själva sökvägen (ännu) inte finns, till exempel en backupmapp som ska skapas.
     */
    static Path resolveReal(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path existing = absolute;
        Deque<Path> missing = new ArrayDeque<>();
        while (existing != null && !Files.exists(existing)) {
            missing.push(existing.getFileName());
            existing = existing.getParent();
        }
        Path result = existing == null ? absolute : existing.toRealPath();
        for (Path part : missing) {
            result = result.resolve(part);
        }
        return result;
    }

    /**
     * Listar alla vanliga filer under rotmappen, nycklade på sökväg relativt roten (med snedstreck).
     * Symlänkar och andra specialfiler hoppas över, så att inga cykler kan uppstå.
     */
    static SortedMap<String, Path> listFiles(Path root) throws IOException {
        SortedMap<String, Path> files = new TreeMap<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .forEach(path -> files.put(toKey(root.relativize(path)), path));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return files;
    }

    /** Jämför innehållet byte för byte. */
    static boolean sameContent(Path first, Path second) throws IOException {
        return Files.size(first) == Files.size(second) && Files.mismatch(first, second) == -1L;
    }

    private static String toKey(Path relative) {
        return relative.toString().replace(File.separatorChar, '/');
    }
}
