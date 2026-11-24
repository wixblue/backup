package com.wixblue.backup.project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final ProjectService service;
    private final BackupProjectRepository repository;

    @TempDir
    Path temp;

    ProjectControllerTest(
            MockMvc mockMvc, ObjectMapper objectMapper, ProjectService service, BackupProjectRepository repository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.service = service;
        this.repository = repository;
    }

    @BeforeEach
    void emptyDatabase() {
        repository.deleteAll();
    }

    @Test
    void createReturns201WithIdAndFields() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Semester")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Semester"))
                .andExpect(jsonPath("$.description").value("Beskrivning"))
                .andExpect(jsonPath("$.source").value(source()))
                .andExpect(jsonPath("$.destination").value(destination()));
    }

    @Test
    void listReturnsStoredProjectsSortedByName() throws Exception {
        service.create(request("Bilder"));
        service.create(request("Arkiv"));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Arkiv"))
                .andExpect(jsonPath("$[1].name").value("Bilder"));
    }

    @Test
    void getReturnsProject() throws Exception {
        var created = service.create(request("Semester"));

        mockMvc.perform(get("/api/projects/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Semester"));
    }

    @Test
    void getUnknownProjectReturns404() throws Exception {
        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void updateChangesProject() throws Exception {
        var created = service.create(request("Gammalt"));

        mockMvc.perform(put("/api/projects/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nytt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.name").value("Nytt"));
    }

    @Test
    void deleteReturns204AndProjectIsGone() throws Exception {
        var created = service.create(request("Tas bort"));

        mockMvc.perform(delete("/api/projects/{id}", created.id()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/projects/{id}", created.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void destinationInsideSourceReturns400() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "name", "Fel",
                "description", "Beskrivning",
                "source", source(),
                "destination", temp.resolve("bilder").resolve("backup").toString()));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    private ProjectRequest request(String name) {
        return new ProjectRequest(name, "Beskrivning", source(), destination());
    }

    private String json(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "description", "Beskrivning",
                "source", source(),
                "destination", destination()));
    }

    private String source() {
        return temp.resolve("bilder").toString();
    }

    private String destination() {
        return temp.resolve("backup").toString();
    }
}
