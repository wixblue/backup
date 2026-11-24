package com.wixblue.backup.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProjectServiceTest {

    private final ProjectService service;
    private final BackupProjectRepository repository;

    @TempDir
    Path temp;

    ProjectServiceTest(ProjectService service, BackupProjectRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @BeforeEach
    void emptyDatabase() {
        repository.deleteAll();
    }

    @Test
    void createStoresProjectInDatabaseAndAssignsId() {
        var created = service.create(request("Semester"));

        assertThat(created.id()).isNotNull();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(service.get(created.id())).isEqualTo(created);
    }

    @Test
    void descriptionIsOptional() {
        var created = service.create(new ProjectRequest("Utan text", null, source(), destination()));

        assertThat(created.description()).isNull();
    }

    @Test
    void findAllReturnsProjectsSortedByName() {
        service.create(request("Bilder"));
        service.create(request("Arkiv"));

        assertThat(service.findAll()).extracting(ProjectResponse::name).containsExactly("Arkiv", "Bilder");
    }

    @Test
    void updateChangesAllFields() {
        var created = service.create(request("Gammalt namn"));
        var changed = new ProjectRequest(
                "Nytt namn", "Ny beskrivning", temp.resolve("nya-bilder").toString(), temp.resolve("ny-backup").toString());

        var updated = service.update(created.id(), changed);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(service.get(created.id())).isEqualTo(updated);
        assertThat(updated.name()).isEqualTo("Nytt namn");
        assertThat(updated.description()).isEqualTo("Ny beskrivning");
        assertThat(updated.source()).isEqualTo(changed.source());
        assertThat(updated.destination()).isEqualTo(changed.destination());
    }

    @Test
    void deleteRemovesProject() {
        var created = service.create(request("Tas bort"));

        service.delete(created.id());

        assertThat(repository.count()).isZero();
    }

    @Test
    void unknownProjectGivesNotFound() {
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("999");
        assertThatThrownBy(() -> service.update(999L, request("Finns inte")))
                .isInstanceOf(ProjectNotFoundException.class);
        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void destinationInsideSourceIsRejected() {
        var request = new ProjectRequest(
                "Fel", null, source(), temp.resolve("bilder").resolve("backup").toString());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Backupmappen");
        assertThat(repository.count()).isZero();
    }

    @Test
    void sameFolderForSourceAndDestinationIsRejected() {
        var request = new ProjectRequest("Fel", null, source(), source());

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRejectsDestinationInsideSource() {
        var created = service.create(request("Giltigt"));
        var invalid = new ProjectRequest(
                "Giltigt", null, source(), temp.resolve("bilder").resolve("backup").toString());

        assertThatThrownBy(() -> service.update(created.id(), invalid))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.get(created.id()).destination()).isEqualTo(destination());
    }

    private ProjectRequest request(String name) {
        return new ProjectRequest(name, "Beskrivning", source(), destination());
    }

    private String source() {
        return temp.resolve("bilder").toString();
    }

    private String destination() {
        return temp.resolve("backup").toString();
    }
}
