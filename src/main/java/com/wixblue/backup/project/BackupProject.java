package com.wixblue.backup.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Ett backupprojekt: vilken mapp som innehåller bilderna och vart de ska säkerhetskopieras. */
@Entity
public class BackupProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String sourcePath;

    @Column(nullable = false)
    private String destinationPath;

    /** Krävs av JPA. */
    protected BackupProject() {
    }

    public BackupProject(String name, String description, String sourcePath, String destinationPath) {
        this.name = name;
        this.description = description;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }

    public void update(String name, String description, String sourcePath, String destinationPath) {
        this.name = name;
        this.description = description;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }
}
