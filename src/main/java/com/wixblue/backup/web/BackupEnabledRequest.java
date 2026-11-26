package com.wixblue.backup.web;

import jakarta.validation.constraints.NotNull;

/** Boxad {@code Boolean} så att ett utelämnat fält ger 400 i stället för att tyst stänga av backupen. */
public record BackupEnabledRequest(@NotNull Boolean enabled) {
}
