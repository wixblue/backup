package com.wixblue.backup.web;

import jakarta.validation.constraints.NotBlank;

public record BackupRequest(
        @NotBlank String source,
        @NotBlank String destination) {
}
