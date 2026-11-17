package com.wixblue.backup.web;

import jakarta.validation.constraints.NotBlank;

public record CompareRequest(
        @NotBlank String source,
        @NotBlank String backup) {
}
