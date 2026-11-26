package com.wixblue.backup.web;

import com.wixblue.backup.model.BackupResult;
import com.wixblue.backup.model.ComparisonResult;
import com.wixblue.backup.service.BackupService;
import com.wixblue.backup.service.FileComparisonService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BackupController {

    private final BackupService backupService;
    private final FileComparisonService comparisonService;

    public BackupController(BackupService backupService, FileComparisonService comparisonService) {
        this.backupService = backupService;
        this.comparisonService = comparisonService;
    }

    /** Kopierar filer från källmappen till backupmappen. Läget bestämmer om ändrade filer också skrivs över. */
    @PostMapping("/backup")
    public BackupResult backup(@Valid @RequestBody BackupRequest request) {
        return backupService.backup(
                Path.of(request.source()), Path.of(request.destination()), request.modeOrDefault());
    }

    /** Jämför källmappen med backupmappen utan att ändra något. */
    @PostMapping("/compare")
    public ComparisonResult compare(@Valid @RequestBody CompareRequest request) {
        return comparisonService.compare(Path.of(request.source()), Path.of(request.backup()));
    }
}
