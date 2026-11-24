package com.wixblue.backup.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wixblue.backup.TestFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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

    @TempDir
    Path temp;

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
                .andExpect(jsonPath("$.backupUpToDate").value(false))
                .andExpect(jsonPath("$.entries[0].status").value("DIFFERENT"));
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
