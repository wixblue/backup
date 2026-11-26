package com.wixblue.backup.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.wixblue.backup.TestFiles;
import com.wixblue.backup.service.BackupSwitch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class BackupControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BackupSwitch backupSwitch;

    @TempDir
    Path temp;

    /** Brytaren är delad mellan testerna i samma Spring-kontext, så den återställs efter varje test. */
    @AfterEach
    void enableBackup() {
        backupSwitch.enable();
    }

    @Test
    void backupEndpointCopiesFilesAndReportsResult() throws Exception {
        Path source = Files.createDirectory(temp.resolve("source"));
        TestFiles.write(source.resolve("a.txt"), "alpha");
        Path destination = temp.resolve("backup");

        mockMvc.perform(post("/api/backup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("source", source, "destination", destination)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copied").value(1))
                .andExpect(jsonPath("$.entries[0].path").value("a.txt"))
                .andExpect(jsonPath("$.entries[0].action").value("COPIED"));

        assertThat(destination.resolve("a.txt")).hasContent("alpha");
    }

    @Test
    void backupEndpointOnlyAddsMissingFilesWhenModeIsOmitted() throws Exception {
        Path source = Files.createDirectory(temp.resolve("source"));
        Path destination = Files.createDirectory(temp.resolve("backup"));
        TestFiles.write(source.resolve("a.txt"), "ny version, längre");
        TestFiles.write(destination.resolve("a.txt"), "gammal");

        mockMvc.perform(post("/api/backup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("source", source, "destination", destination)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ADD_MISSING"))
                .andExpect(jsonPath("$.notUpdated").value(1))
                .andExpect(jsonPath("$.entries[0].action").value("NOT_UPDATED"));

        assertThat(destination.resolve("a.txt")).hasContent("gammal");
    }

    @Test
    void backupEndpointUpdatesChangedFilesWhenModeIsAddAndUpdate() throws Exception {
        Path source = Files.createDirectory(temp.resolve("source"));
        Path destination = Files.createDirectory(temp.resolve("backup"));
        TestFiles.write(source.resolve("a.txt"), "ny version, längre");
        TestFiles.write(destination.resolve("a.txt"), "gammal");

        mockMvc.perform(post("/api/backup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "source", source.toString(),
                                "destination", destination.toString(),
                                "mode", "ADD_AND_UPDATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ADD_AND_UPDATE"))
                .andExpect(jsonPath("$.updated").value(1));

        assertThat(destination.resolve("a.txt")).hasContent("ny version, längre");
    }

    @Test
    void unknownModeGivesBadRequest() throws Exception {
        mockMvc.perform(post("/api/backup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "source", temp.toString(),
                                "destination", temp.resolve("backup").toString(),
                                "mode", "OVERWRITE_EVERYTHING"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compareEndpointReportsDifferences() throws Exception {
        Path source = Files.createDirectory(temp.resolve("source"));
        Path backup = Files.createDirectory(temp.resolve("backup"));
        TestFiles.write(source.resolve("a.txt"), "1");
        TestFiles.write(backup.resolve("a.txt"), "2");

        mockMvc.perform(post("/api/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("source", source, "backup", backup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.different").value(1))
                .andExpect(jsonPath("$.notBackedUp").value(1))
                .andExpect(jsonPath("$.backupUpToDate").value(false))
                .andExpect(jsonPath("$.entries[0].status").value("DIFFERENT"));
    }

    @Test
    void backupIsEnabledByDefault() throws Exception {
        mockMvc.perform(get("/api/backup/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void backupCanBeDisabledAndEnabledAgain() throws Exception {
        mockMvc.perform(put("/api/backup/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        mockMvc.perform(get("/api/backup/enabled"))
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(put("/api/backup/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void backupGivesConflictWhileDisabledButCompareStillWorks() throws Exception {
        Path source = Files.createDirectory(temp.resolve("source"));
        Path destination = Files.createDirectory(temp.resolve("backup"));
        TestFiles.write(source.resolve("a.txt"), "alpha");
        backupSwitch.disable();

        mockMvc.perform(post("/api/backup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("source", source, "destination", destination)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Backup är avstängd. Slå på den innan du kör en backup."));
        assertThat(destination.resolve("a.txt")).doesNotExist();

        mockMvc.perform(post("/api/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("source", source, "backup", destination)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notBackedUp").value(1));
    }

    @Test
    void missingEnabledFieldGivesBadRequestAndKeepsBackupOn() throws Exception {
        mockMvc.perform(put("/api/backup/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(backupSwitch.isEnabled()).isTrue();
    }

    @Test
    void missingSourceGivesBadRequest() throws Exception {
        mockMvc.perform(post("/api/backup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("source", temp.resolve("saknas"), "destination", temp.resolve("backup"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void blankFieldsGiveBadRequest() throws Exception {
        mockMvc.perform(post("/api/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"\",\"backup\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private String json(String firstKey, Path firstValue, String secondKey, Path secondValue) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of(firstKey, firstValue.toString(), secondKey, secondValue.toString()));
    }
}
