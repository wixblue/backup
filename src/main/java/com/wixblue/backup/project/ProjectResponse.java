package com.wixblue.backup.project;

public record ProjectResponse(Long id, String name, String description, String source, String destination) {

    static ProjectResponse from(BackupProject project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getSourcePath(),
                project.getDestinationPath());
    }
}
