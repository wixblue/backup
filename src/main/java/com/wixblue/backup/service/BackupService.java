package com.wixblue.backup.service;

import com.wixblue.backup.model.BackupAction;
import com.wixblue.backup.model.BackupEntry;
import com.wixblue.backup.model.BackupResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Kopierar filer från en källmapp till en backupmapp.
 *
 * <ul>
 *   <li>Filer som saknas i backupen kopieras.</li>
 *   <li>Filer vars innehåll skiljer sig från backupens version skrivs över.</li>
 *   <li>Filer som redan finns med samma innehåll hoppas över.</li>
 *   <li>Filer som bara finns i backupen lämnas orörda.</li>
 * </ul>
 */
@Service
public class BackupService {

    public BackupResult backup(Path source, Path destination) {
        try {
            Path sourceDir = FileSupport.requireDirectory(source, "Källmappen");
            Path destinationDir = FileSupport.resolveReal(destination);
            validateDestination(sourceDir, destinationDir);
            Files.createDirectories(destinationDir);

            List<BackupEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Path> file : FileSupport.listFiles(sourceDir).entrySet()) {
                entries.add(backupFile(file.getKey(), file.getValue(), destinationDir.resolve(file.getKey())));
            }
            return BackupResult.of(sourceDir, destinationDir, entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void validateDestination(Path sourceDir, Path destinationDir) {
        if (destinationDir.startsWith(sourceDir)) {
            throw new IllegalArgumentException(
                    "Backupmålet får inte vara källmappen eller ligga inuti den: " + destinationDir);
        }
        if (Files.exists(destinationDir) && !Files.isDirectory(destinationDir)) {
            throw new IllegalArgumentException("Backupmålet finns men är inte en mapp: " + destinationDir);
        }
    }

    private static BackupEntry backupFile(String relativePath, Path source, Path target) {
        try {
            boolean exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            if (exists) {
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                    return new BackupEntry(relativePath, BackupAction.FAILED,
                            "Målet finns redan men är inte en vanlig fil");
                }
                if (FileSupport.sameContent(source, target)) {
                    return new BackupEntry(relativePath, BackupAction.SKIPPED, null);
                }
            }
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return new BackupEntry(relativePath, exists ? BackupAction.UPDATED : BackupAction.COPIED, null);
        } catch (IOException e) {
            return new BackupEntry(relativePath, BackupAction.FAILED, e.getMessage());
        }
    }
}
