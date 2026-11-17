package com.wixblue.backup.service;

import com.wixblue.backup.model.ComparisonEntry;
import com.wixblue.backup.model.ComparisonResult;
import com.wixblue.backup.model.ComparisonStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

/** Jämför en källmapp med en backupmapp, fil för fil, utan att ändra något. */
@Service
public class FileComparisonService {

    public ComparisonResult compare(Path source, Path backup) {
        try {
            Path sourceDir = FileSupport.requireDirectory(source, "Källmappen");
            Path backupDir = FileSupport.requireDirectory(backup, "Backupmappen");

            SortedMap<String, Path> sourceFiles = FileSupport.listFiles(sourceDir);
            SortedMap<String, Path> backupFiles = FileSupport.listFiles(backupDir);

            SortedSet<String> allPaths = new TreeSet<>(sourceFiles.keySet());
            allPaths.addAll(backupFiles.keySet());

            List<ComparisonEntry> entries = new ArrayList<>();
            for (String path : allPaths) {
                entries.add(new ComparisonEntry(path, statusOf(sourceFiles.get(path), backupFiles.get(path))));
            }
            return ComparisonResult.of(sourceDir, backupDir, entries);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ComparisonStatus statusOf(Path sourceFile, Path backupFile) throws IOException {
        if (backupFile == null) {
            return ComparisonStatus.ONLY_IN_SOURCE;
        }
        if (sourceFile == null) {
            return ComparisonStatus.ONLY_IN_BACKUP;
        }
        return FileSupport.sameContent(sourceFile, backupFile)
                ? ComparisonStatus.IDENTICAL
                : ComparisonStatus.DIFFERENT;
    }
}
