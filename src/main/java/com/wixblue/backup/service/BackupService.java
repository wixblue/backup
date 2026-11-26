package com.wixblue.backup.service;

import com.wixblue.backup.model.BackupAction;
import com.wixblue.backup.model.BackupEntry;
import com.wixblue.backup.model.BackupMode;
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
 *   <li>Filer vars innehåll skiljer sig från backupens version skrivs över, men bara i läget
 *       {@link BackupMode#ADD_AND_UPDATE}. I {@link BackupMode#ADD_MISSING} lämnas de orörda.</li>
 *   <li>Filer som redan finns med samma innehåll hoppas över.</li>
 *   <li>Filer som bara finns i backupen lämnas orörda.</li>
 * </ul>
 */
@Service
public class BackupService {

    private final BackupSwitch backupSwitch;

    public BackupService(BackupSwitch backupSwitch) {
        this.backupSwitch = backupSwitch;
    }

    /** Lägger bara till filer som saknas i backupen. */
    public BackupResult backup(Path source, Path destination) {
        return backup(source, destination, BackupMode.ADD_MISSING);
    }

    /** @throws BackupDisabledException om backup är avstängd */
    public BackupResult backup(Path source, Path destination, BackupMode mode) {
        if (!backupSwitch.isEnabled()) {
            throw new BackupDisabledException();
        }
        try {
            Path sourceDir = FileSupport.requireDirectory(source, "Källmappen");
            Path destinationDir = FileSupport.resolveReal(destination);
            validateDestination(sourceDir, destinationDir);
            Files.createDirectories(destinationDir);

            List<BackupEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Path> file : FileSupport.listFiles(sourceDir).entrySet()) {
                entries.add(backupFile(file.getKey(), file.getValue(), destinationDir.resolve(file.getKey()), mode));
            }
            return BackupResult.of(sourceDir, destinationDir, mode, entries);
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

    private static BackupEntry backupFile(String relativePath, Path source, Path target, BackupMode mode) {
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
                if (mode == BackupMode.ADD_MISSING) {
                    return new BackupEntry(relativePath, BackupAction.NOT_UPDATED, null);
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
