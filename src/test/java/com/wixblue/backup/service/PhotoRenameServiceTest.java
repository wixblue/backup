package com.wixblue.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.wixblue.backup.TestFiles;
import com.wixblue.backup.model.RenameEntry;
import com.wixblue.backup.model.RenameResult;
import com.wixblue.backup.model.RenameStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhotoRenameServiceTest {

    private static final LocalDateTime TAKEN = LocalDateTime.of(2024, 7, 4, 14, 32, 59);

    private final PhotoRenameService service = new PhotoRenameService();

    @TempDir
    Path temp;

    private Path folder;

    @BeforeEach
    void setUp() throws IOException {
        folder = Files.createDirectory(temp.resolve("Semester"));
    }

    private Path photo(String name, LocalDateTime modified) throws IOException {
        return TestFiles.writeModifiedAt(folder.resolve(name), name, modified);
    }

    private static List<String> newNames(RenameResult result) {
        return result.entries().stream().map(RenameEntry::to).toList();
    }

    @Test
    void namesPhotoAfterModifiedTimeAndFolder() throws IOException {
        photo("IMG_0001.jpg", TAKEN);

        RenameResult result = service.rename(folder, false);

        assertThat(newNames(result)).containsExactly("2024-07-04_14-32_Semester.jpg");
        assertThat(result.renamed()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        assertThat(folder.resolve("2024-07-04_14-32_Semester.jpg")).hasContent("IMG_0001.jpg");
        assertThat(folder.resolve("IMG_0001.jpg")).doesNotExist();
    }

    @Test
    void dryRunOnlyPlansTheRenames() throws IOException {
        photo("IMG_0001.jpg", TAKEN);

        RenameResult result = service.rename(folder, true);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.planned()).isEqualTo(1);
        assertThat(result.renamed()).isZero();
        assertThat(result.entries().get(0).status()).isEqualTo(RenameStatus.PLANNED);
        assertThat(result.entries().get(0).from()).isEqualTo("IMG_0001.jpg");
        assertThat(folder.resolve("IMG_0001.jpg")).exists();
        assertThat(folder.resolve("2024-07-04_14-32_Semester.jpg")).doesNotExist();
    }

    @Test
    void keepsTheFileExtensionAsItIsWritten() throws IOException {
        photo("IMG_0001.JPEG", TAKEN);

        assertThat(newNames(service.rename(folder, true))).containsExactly("2024-07-04_14-32_Semester.JPEG");
    }

    @Test
    void recognizesCommonImageExtensions() throws IOException {
        photo("a.png", TAKEN);
        photo("b.heic", TAKEN.plusMinutes(1));
        photo("c.TIFF", TAKEN.plusMinutes(2));

        assertThat(service.rename(folder, true).planned()).isEqualTo(3);
    }

    @Test
    void numbersPhotosTakenInTheSameMinuteOldestFirst() throws IOException {
        photo("a.jpg", TAKEN.withSecond(10));
        photo("b.jpg", TAKEN.withSecond(50));
        photo("c.jpg", TAKEN.withSecond(30));

        RenameResult result = service.rename(folder, false);

        assertThat(newNames(result)).containsExactly(
                "2024-07-04_14-32_Semester.jpg",
                "2024-07-04_14-32_Semester_2.jpg",
                "2024-07-04_14-32_Semester_3.jpg");
        assertThat(folder.resolve("2024-07-04_14-32_Semester.jpg")).hasContent("a.jpg");
        assertThat(folder.resolve("2024-07-04_14-32_Semester_2.jpg")).hasContent("c.jpg");
        assertThat(folder.resolve("2024-07-04_14-32_Semester_3.jpg")).hasContent("b.jpg");
    }

    @Test
    void sameMinuteWithDifferentExtensionsNeedsNoNumber() throws IOException {
        photo("a.jpg", TAKEN.withSecond(10));
        photo("b.png", TAKEN.withSecond(20));

        assertThat(newNames(service.rename(folder, true))).containsExactly(
                "2024-07-04_14-32_Semester.jpg",
                "2024-07-04_14-32_Semester.png");
    }

    @Test
    void treatsNamesThatOnlyDifferInCaseAsTheSameName() throws IOException {
        photo("a.jpg", TAKEN.withSecond(10));
        photo("b.JPG", TAKEN.withSecond(20));

        assertThat(newNames(service.rename(folder, true))).containsExactly(
                "2024-07-04_14-32_Semester.jpg",
                "2024-07-04_14-32_Semester_2.JPG");
    }

    @Test
    void usesFileNameToOrderPhotosWithTheSameTime() throws IOException {
        photo("b.jpg", TAKEN);
        photo("a.jpg", TAKEN);

        RenameResult result = service.rename(folder, true);

        assertThat(result.entries()).extracting(RenameEntry::from).containsExactly("a.jpg", "b.jpg");
    }

    @Test
    void doesNotNumberPhotosFromDifferentMinutes() throws IOException {
        photo("a.jpg", TAKEN);
        photo("b.jpg", TAKEN.plusMinutes(1));

        assertThat(newNames(service.rename(folder, true))).containsExactly(
                "2024-07-04_14-32_Semester.jpg",
                "2024-07-04_14-33_Semester.jpg");
    }

    @Test
    void skipsPhotosThatAlreadyHaveTheRightName() throws IOException {
        photo("2024-07-04_14-32_Semester.jpg", TAKEN);
        photo("2024-07-04_14-32_Semester_2.jpg", TAKEN);

        RenameResult result = service.rename(folder, false);

        assertThat(result.entries()).isEmpty();
        assertThat(folder.resolve("2024-07-04_14-32_Semester_2.jpg")).exists();
    }

    @Test
    void avoidsNamesThatAlreadyExistInTheFolder() throws IOException {
        photo("2024-07-04_14-32_Semester.jpg", TAKEN);
        photo("new.jpg", TAKEN.withSecond(10));

        assertThat(newNames(service.rename(folder, true))).containsExactly("2024-07-04_14-32_Semester_2.jpg");
    }

    @Test
    void secondRunHasNothingLeftToRename() throws IOException {
        photo("a.jpg", TAKEN);
        photo("b.jpg", TAKEN);
        service.rename(folder, false);

        RenameResult second = service.rename(folder, false);

        assertThat(second.entries()).isEmpty();
    }

    @Test
    void ignoresFilesThatAreNotImagesAndSubfolders() throws IOException {
        TestFiles.write(folder.resolve("notes.txt"), "text");
        TestFiles.write(folder.resolve("README"), "text");
        TestFiles.write(folder.resolve("jpg"), "inget filnamnstillägg");
        Files.createDirectory(folder.resolve("album.jpg"));
        photo("sub/deep.jpg", TAKEN);

        RenameResult result = service.rename(folder, false);

        assertThat(result.entries()).isEmpty();
        assertThat(folder.resolve("sub/deep.jpg")).exists();
    }

    @Test
    void emptyFolderGivesEmptyResult() throws IOException {
        RenameResult result = service.rename(folder, false);

        assertThat(result.entries()).isEmpty();
        assertThat(result.directory()).isEqualTo(folder.toRealPath().toString());
    }

    @Test
    void reportsPhotosThatCouldNotBeRenamed() throws IOException {
        photo("IMG_0001.jpg", TAKEN);
        try {
            // Som root går skrivskydd på mappen inte att testa, då hoppas testet över
            assumeTrue(folder.toFile().setWritable(false) && !Files.isWritable(folder));

            RenameResult result = service.rename(folder, false);

            assertThat(result.failed()).isEqualTo(1);
            assertThat(result.entries().get(0).status()).isEqualTo(RenameStatus.FAILED);
            assertThat(result.entries().get(0).message()).isNotBlank();
            assertThat(folder.resolve("IMG_0001.jpg")).exists();
        } finally {
            folder.toFile().setWritable(true);
        }
    }

    @Test
    void rejectsMissingFolder() {
        assertThatThrownBy(() -> service.rename(temp.resolve("saknas"), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAFileInsteadOfAFolder() throws IOException {
        Path file = TestFiles.write(temp.resolve("fil.jpg"), "x");

        assertThatThrownBy(() -> service.rename(file, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFolderWithoutName() {
        Path filesystemRoot = temp.getRoot();

        assertThatThrownBy(() -> service.rename(filesystemRoot, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saknar namn");
    }
}
