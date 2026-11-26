package com.wixblue.backup.web;

import com.wixblue.backup.project.ProjectNotFoundException;
import com.wixblue.backup.service.BackupDisabledException;
import java.io.UncheckedIOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    /** Ogiltiga indata, till exempel en mapp som inte finns eller en ogiltig sökväg. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Projektet finns inte. */
    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail handleProjectNotFound(ProjectNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Backup är avstängd. */
    @ExceptionHandler(BackupDisabledException.class)
    public ProblemDetail handleBackupDisabled(BackupDisabledException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Oväntade fel vid filåtkomst. */
    @ExceptionHandler(UncheckedIOException.class)
    public ProblemDetail handleIoError(UncheckedIOException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Fel vid filåtkomst: " + e.getCause().getMessage());
    }
}
