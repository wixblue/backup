package com.wixblue.backup.project;

import jakarta.validation.constraints.NotBlank;

/** Indata när ett projekt skapas eller ändras. Beskrivningen är valfri. */
public record ProjectRequest(
        @NotBlank String name,
        String description,
        @NotBlank String source,
        @NotBlank String destination) {
}
