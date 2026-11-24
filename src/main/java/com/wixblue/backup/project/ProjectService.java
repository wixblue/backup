package com.wixblue.backup.project;

import java.nio.file.Path;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final BackupProjectRepository repository;

    public ProjectService(BackupProjectRepository repository) {
        this.repository = repository;
    }

    public List<ProjectResponse> findAll() {
        return repository.findAll(Sort.by("name")).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    public ProjectResponse get(long id) {
        return ProjectResponse.from(find(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        validatePaths(request);
        var project = new BackupProject(
                request.name(), request.description(), request.source(), request.destination());
        return ProjectResponse.from(repository.save(project));
    }

    @Transactional
    public ProjectResponse update(long id, ProjectRequest request) {
        validatePaths(request);
        var project = find(id);
        project.update(request.name(), request.description(), request.source(), request.destination());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(long id) {
        repository.delete(find(id));
    }

    private BackupProject find(long id) {
        return repository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    /** Backupen får inte ligga i bildmappen, annars skulle den kopieras in i sig själv. */
    private static void validatePaths(ProjectRequest request) {
        Path source = Path.of(request.source()).toAbsolutePath().normalize();
        Path destination = Path.of(request.destination()).toAbsolutePath().normalize();
        if (destination.startsWith(source)) {
            throw new IllegalArgumentException(
                    "Backupmappen får inte ligga i bildmappen: " + request.destination());
        }
    }
}
