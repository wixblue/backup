package com.wixblue.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wixblue.backup.TestFiles;
import com.wixblue.backup.model.ComparisonEntry;
import com.wixblue.backup.model.ComparisonResult;
import com.wixblue.backup.model.ComparisonStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileComparisonServiceTest {

    private final FileComparisonService service = new FileComparisonService();

    @TempDir
    Path temp;

    private Path source;
    private Path backup;

    @BeforeEach
    void setUp() throws IOException {
        source = Files.createDirectory(temp.resolve("source"));
        backup = Files.createDirectory(temp.resolve("backup"));
    }

    @Test
    void classifiesEachFile() throws IOException {
        TestFiles.write(source.resolve("same.txt"), "x");
        TestFiles.write(backup.resolve("same.txt"), "x");
        TestFiles.write(source.resolve("changed.txt"), "1");
        TestFiles.write(backup.resolve("changed.txt"), "2");
        TestFiles.write(source.resolve("new.txt"), "n");
        TestFiles.write(backup.resolve("old.txt"), "o");

        ComparisonResult result = service.compare(source, backup);

        assertThat(result.entries()).containsExactly(
                new ComparisonEntry("changed.txt", ComparisonStatus.DIFFERENT),
                new ComparisonEntry("new.txt", ComparisonStatus.ONLY_IN_SOURCE),
                new ComparisonEntry("old.txt", ComparisonStatus.ONLY_IN_BACKUP),
                new ComparisonEntry("same.txt", ComparisonStatus.IDENTICAL));
        assertThat(result.identical()).isEqualTo(1);
        assertThat(result.different()).isEqualTo(1);
        assertThat(result.onlyInSource()).isEqualTo(1);
        assertThat(result.onlyInBackup()).isEqualTo(1);
        assertThat(result.backupUpToDate()).isFalse();
    }

    @Test
    void comparesFilesInSubdirectories() throws IOException {
        TestFiles.write(source.resolve("sub/a.txt"), "1");
        TestFiles.write(backup.resolve("sub/a.txt"), "1");

        ComparisonResult result = service.compare(source, backup);

        assertThat(result.entries())
                .containsExactly(new ComparisonEntry("sub/a.txt", ComparisonStatus.IDENTICAL));
    }

    @Test
    void detectsDifferenceEvenWhenFileSizeIsUnchanged() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "aaaa");
        TestFiles.write(backup.resolve("a.txt"), "bbbb");

        ComparisonResult result = service.compare(source, backup);

        assertThat(result.different()).isEqualTo(1);
    }

    @Test
    void backupIsUpToDateAfterRunningBackup() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");
        TestFiles.write(source.resolve("sub/b.txt"), "beta");
        new BackupService().backup(source, backup);

        ComparisonResult result = service.compare(source, backup);

        assertThat(result.backupUpToDate()).isTrue();
        assertThat(result.identical()).isEqualTo(2);
    }

    @Test
    void extraFilesInBackupDoNotMakeItOutOfDate() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");
        TestFiles.write(backup.resolve("a.txt"), "alpha");
        TestFiles.write(backup.resolve("borttagen.txt"), "x");

        ComparisonResult result = service.compare(source, backup);

        assertThat(result.onlyInBackup()).isEqualTo(1);
        assertThat(result.backupUpToDate()).isTrue();
    }

    @Test
    void rejectsMissingBackupDirectory() {
        assertThatThrownBy(() -> service.compare(source, temp.resolve("saknas")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSourceDirectory() {
        assertThatThrownBy(() -> service.compare(temp.resolve("saknas"), backup))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
