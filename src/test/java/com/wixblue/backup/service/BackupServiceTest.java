package com.wixblue.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wixblue.backup.TestFiles;
import com.wixblue.backup.model.BackupAction;
import com.wixblue.backup.model.BackupMode;
import com.wixblue.backup.model.BackupResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupServiceTest {

    private final BackupService service = new BackupService();

    @TempDir
    Path temp;

    private Path source;
    private Path destination;

    @BeforeEach
    void setUp() throws IOException {
        source = Files.createDirectory(temp.resolve("source"));
        destination = temp.resolve("backup");
    }

    @Test
    void copiesNewFilesIncludingSubdirectories() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");
        TestFiles.write(source.resolve("sub/dir/b.txt"), "beta");

        BackupResult result = service.backup(source, destination);

        assertThat(result.copied()).isEqualTo(2);
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(destination.resolve("a.txt")).hasContent("alpha");
        assertThat(destination.resolve("sub/dir/b.txt")).hasContent("beta");
    }

    @Test
    void skipsFilesThatAreAlreadyUpToDate() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");
        service.backup(source, destination);

        BackupResult second = service.backup(source, destination);

        assertThat(second.skipped()).isEqualTo(1);
        assertThat(second.copied()).isZero();
        assertThat(second.updated()).isZero();
    }

    @Test
    void updatesFilesWhoseContentHasChanged() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "version 1");
        service.backup(source, destination);
        TestFiles.write(source.resolve("a.txt"), "version 2, något längre");

        BackupResult result = service.backup(source, destination, BackupMode.ADD_AND_UPDATE);

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.entries().get(0).action()).isEqualTo(BackupAction.UPDATED);
        assertThat(destination.resolve("a.txt")).hasContent("version 2, något längre");
    }

    @Test
    void detectsChangesEvenWhenFileSizeIsUnchanged() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "aaaa");
        service.backup(source, destination);
        TestFiles.write(source.resolve("a.txt"), "bbbb");

        BackupResult result = service.backup(source, destination, BackupMode.ADD_AND_UPDATE);

        assertThat(result.updated()).isEqualTo(1);
        assertThat(destination.resolve("a.txt")).hasContent("bbbb");
    }

    @Test
    void copiesOnlyMissingFilesWhenBackupIsPartial() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");
        service.backup(source, destination);
        TestFiles.write(source.resolve("b.txt"), "beta");

        BackupResult result = service.backup(source, destination);

        assertThat(result.copied()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void addMissingLeavesChangedFilesUntouched() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "version 1");
        service.backup(source, destination);
        TestFiles.write(source.resolve("a.txt"), "version 2, något längre");
        TestFiles.write(source.resolve("b.txt"), "ny fil");

        BackupResult result = service.backup(source, destination, BackupMode.ADD_MISSING);

        assertThat(result.copied()).isEqualTo(1);
        assertThat(result.notUpdated()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.entries().get(0).action()).isEqualTo(BackupAction.NOT_UPDATED);
        assertThat(destination.resolve("a.txt")).hasContent("version 1");
        assertThat(destination.resolve("b.txt")).hasContent("ny fil");
    }

    @Test
    void defaultsToAddingOnlyMissingFiles() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "version 1");
        service.backup(source, destination);
        TestFiles.write(source.resolve("a.txt"), "version 2, något längre");

        BackupResult result = service.backup(source, destination);

        assertThat(result.mode()).isEqualTo(BackupMode.ADD_MISSING);
        assertThat(result.notUpdated()).isEqualTo(1);
        assertThat(destination.resolve("a.txt")).hasContent("version 1");
    }

    @Test
    void addMissingStillSkipsIdenticalFiles() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");
        service.backup(source, destination);

        BackupResult result = service.backup(source, destination, BackupMode.ADD_MISSING);

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.notUpdated()).isZero();
    }

    @Test
    void resultReportsTheModeThatWasUsed() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");

        BackupResult result = service.backup(source, destination, BackupMode.ADD_AND_UPDATE);

        assertThat(result.mode()).isEqualTo(BackupMode.ADD_AND_UPDATE);
    }

    @Test
    void keepsFilesThatOnlyExistInBackup() throws IOException {
        TestFiles.write(source.resolve("a.txt"), "alpha");
        TestFiles.write(destination.resolve("old.txt"), "gammal");

        service.backup(source, destination);

        assertThat(destination.resolve("old.txt")).hasContent("gammal");
    }

    @Test
    void rejectsDestinationInsideSource() {
        assertThatThrownBy(() -> service.backup(source, source.resolve("backup")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDestinationThatIsTheSource() {
        assertThatThrownBy(() -> service.backup(source, source))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSource() {
        assertThatThrownBy(() -> service.backup(temp.resolve("saknas"), destination))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
