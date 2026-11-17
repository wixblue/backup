package com.wixblue.backup.web;

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

    /** Oväntade fel vid filåtkomst. */
    @ExceptionHandler(UncheckedIOException.class)
    public ProblemDetail handleIoError(UncheckedIOException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Fel vid filåtkomst: " + e.getCause().getMessage());
    }
}
