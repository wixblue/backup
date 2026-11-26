package com.wixblue.backup.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wixblue.backup.TestFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
class RenameControllerTest {

    private static final LocalDateTime TAKEN = LocalDateTime.of(2024, 7, 4, 14, 32, 59);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @TempDir
    Path temp;

    @Test
    void onlyPreviewsWhenDryRunIsOmitted() throws Exception {
        Path folder = Files.createDirectory(temp.resolve("Semester"));
        TestFiles.writeModifiedAt(folder.resolve("IMG_0001.jpg"), "bild", TAKEN);

        mockMvc.perform(post("/api/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("directory", folder.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.planned").value(1))
                .andExpect(jsonPath("$.entries[0].from").value("IMG_0001.jpg"))
                .andExpect(jsonPath("$.entries[0].to").value("2024-07-04_14-32_Semester.jpg"))
                .andExpect(jsonPath("$.entries[0].status").value("PLANNED"));

        assertThat(folder.resolve("IMG_0001.jpg")).exists();
    }

    @Test
    void renamesPhotosWhenDryRunIsFalse() throws Exception {
        Path folder = Files.createDirectory(temp.resolve("Semester"));
        TestFiles.writeModifiedAt(folder.resolve("IMG_0001.jpg"), "bild", TAKEN);

        mockMvc.perform(post("/api/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("directory", folder.toString(), "dryRun", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.renamed").value(1))
                .andExpect(jsonPath("$.entries[0].status").value("RENAMED"));

        assertThat(folder.resolve("2024-07-04_14-32_Semester.jpg")).hasContent("bild");
        assertThat(folder.resolve("IMG_0001.jpg")).doesNotExist();
    }

    @Test
    void missingFolderGivesBadRequest() throws Exception {
        mockMvc.perform(post("/api/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("directory", temp.resolve("saknas").toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void blankDirectoryGivesBadRequest() throws Exception {
        mockMvc.perform(post("/api/rename")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"directory\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
