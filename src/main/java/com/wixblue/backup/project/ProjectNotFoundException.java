package com.wixblue.backup.project;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(long id) {
        super("Projekt med id " + id + " finns inte");
    }
}
