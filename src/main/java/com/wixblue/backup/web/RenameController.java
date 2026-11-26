package com.wixblue.backup.web;

import com.wixblue.backup.model.RenameResult;
import com.wixblue.backup.service.PhotoRenameService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RenameController {

    private final PhotoRenameService renameService;

    public RenameController(PhotoRenameService renameService) {
        this.renameService = renameService;
    }

    /** Döper om bilderna i en mapp till YYYY-MM-DD_HH-MM_mappnamn. Förhandsvisar som standard. */
    @PostMapping("/rename")
    public RenameResult rename(@Valid @RequestBody RenameRequest request) {
        return renameService.rename(Path.of(request.directory()), request.dryRunOrDefault());
    }
}
