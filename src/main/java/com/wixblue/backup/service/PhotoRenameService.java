package com.wixblue.backup.service;

import com.wixblue.backup.model.RenameEntry;
import com.wixblue.backup.model.RenameResult;
import com.wixblue.backup.model.RenameStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Döper om bilder direkt i en mapp till {@code YYYY-MM-DD_HH-MM_mappnamn.ext}.
 *
 * <p>Tidpunkten är filens senast ändrade tid. Den är inte alltid samma som när fotot togs,
 * eftersom en kopierad eller nedladdad bild får en ny ändringstid.
 *
 * <ul>
 *   <li>Tar flera bilder samma minut får de efterföljande ett löpnummer, till exempel {@code _2}.</li>
 *   <li>Bilder som redan har rätt namn hoppas över, så en andra körning ändrar ingenting.</li>
 *   <li>Befintliga filer skrivs aldrig över och underkataloger berörs inte.</li>
 * </ul>
 */
@Service
public class PhotoRenameService {

    private static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "heic", "tif", "tiff", "webp");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    /** @param dryRun om sant planeras namnbytena bara, utan att något döps om */
    public RenameResult rename(Path directory, boolean dryRun) {
        try {
            Path dir = FileSupport.requireDirectory(directory, "Mappen");
            String folderName = folderNameOf(dir);
            List<RenameEntry> entries = new ArrayList<>();
            for (Rename rename : plan(dir, folderName)) {
                entries.add(dryRun ? rename.planned() : rename.execute());
            }
            return RenameResult.of(dir, dryRun, entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String folderNameOf(Path dir) {
        Path name = dir.getFileName();
        if (name == null) {
            throw new IllegalArgumentException("Mappen saknar namn och kan inte användas: " + dir);
        }
        return name.toString();
    }

    private static List<Rename> plan(Path dir, String folderName) throws IOException {
        List<Path> files = listFiles(dir);
        Set<String> takenNames = new HashSet<>();
        files.forEach(file -> takenNames.add(lowerCaseName(file)));

        List<Rename> renames = new ArrayList<>();
        for (Path photo : photosInRenameOrder(files)) {
            String base = TIMESTAMP.format(modifiedAt(photo)) + "_" + folderName;
            String fileName = photo.getFileName().toString();
            String extension = extensionOf(fileName);
            if (hasNameFor(fileName, base, extension)) {
                continue;
            }
            String newName = uniqueName(base, extension, takenNames);
            takenNames.add(newName.toLowerCase(Locale.ROOT));
            renames.add(new Rename(photo, dir.resolve(newName)));
        }
        return renames;
    }

    private static List<Path> listFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList();
        }
    }

    /** Äldst först, och filnamn som tiebreaker, så att löpnumren blir desamma varje gång. */
    private static List<Path> photosInRenameOrder(List<Path> files) {
        return files.stream()
                .filter(PhotoRenameService::isPhoto)
                .sorted(Comparator.comparing(PhotoRenameService::modifiedAt)
                        .thenComparing(file -> file.getFileName().toString()))
                .toList();
    }

    private static boolean isPhoto(Path file) {
        return IMAGE_EXTENSIONS.contains(extensionOf(file.getFileName().toString()).toLowerCase(Locale.ROOT));
    }

    private static LocalDateTime modifiedAt(Path file) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(file).toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    private static String lowerCaseName(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT);
    }

    /** Rätt namn är basnamnet, eventuellt med löpnummer, till exempel {@code _2}. */
    private static boolean hasNameFor(String fileName, String base, String extension) {
        return fileName.matches(Pattern.quote(base) + "(_\\d+)?\\." + Pattern.quote(extension));
    }

    private static String uniqueName(String base, String extension, Set<String> takenNames) {
        String candidate = base + "." + extension;
        for (int number = 2; takenNames.contains(candidate.toLowerCase(Locale.ROOT)); number++) {
            candidate = base + "_" + number + "." + extension;
        }
        return candidate;
    }

    private record Rename(Path from, Path to) {

        RenameEntry planned() {
            return entry(RenameStatus.PLANNED, null);
        }

        RenameEntry execute() {
            try {
                // Ingen REPLACE_EXISTING: en befintlig fil får aldrig skrivas över
                Files.move(from, to);
                return entry(RenameStatus.RENAMED, null);
            } catch (IOException e) {
                return entry(RenameStatus.FAILED, e.getMessage());
            }
        }

        private RenameEntry entry(RenameStatus status, String message) {
            return new RenameEntry(from.getFileName().toString(), to.getFileName().toString(), status, message);
        }
    }
}
