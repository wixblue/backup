package com.wixblue.backup.web;

import com.wixblue.backup.model.BackupMode;
import jakarta.validation.constraints.NotBlank;

/**
 * @param mode hur filer som redan finns i backupen ska behandlas. Utelämnas den läggs bara saknade filer till.
 */
public record BackupRequest(
        @NotBlank String source,
        @NotBlank String destination,
        BackupMode mode) {

    public BackupMode modeOrDefault() {
        return mode == null ? BackupMode.ADD_MISSING : mode;
    }
}
