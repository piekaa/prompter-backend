package com.prompter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompter.IntegrationTestBase;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScriptControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String create(String title, String content) throws Exception {
        MvcResult res = mvc.perform(post("/api/public/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new ScriptControllerIntegrationTest.Payload(title, content))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content))
                .andReturn();
        return res.getResponse().getContentAsString();
    }

    record Payload(String title, String content) {}

    @Test
    @Order(1)
    void create_returns201AndPersistedBody() throws Exception {
        create("Kickoff", "Hello prompter world");
    }

    @Test
    @Order(2)
    void create_blankTitle_returns400WithErrorField() throws Exception {
        mvc.perform(post("/api/public/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"content\":\"x\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @Order(3)
    void list_containsCreatedScript() throws Exception {
        mvc.perform(get("/api/public/scripts"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
           .andExpect(jsonPath("$[0].title").value("Kickoff"));
    }

    @Test
    @Order(4)
    void get_unknownId_returns404WithErrorField() throws Exception {
        mvc.perform(get("/api/public/scripts/999999"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value(containsString("not found")));
    }

    @Test
    @Order(5)
    void update_changesTitleAndContent() throws Exception {
        long id = json.readTree(create("Draft", "v1")).get("id").asLong();

        mvc.perform(put("/api/public/scripts/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Payload("Final", "v2"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.title").value("Final"))
           .andExpect(jsonPath("$.content").value("v2"));

        mvc.perform(get("/api/public/scripts/" + id))
           .andExpect(jsonPath("$.title").value("Final"));
    }

    @Test
    @Order(6)
    void delete_removesScript() throws Exception {
        JsonNode created = json.readTree(create("ToDelete", "bye"));
        long id = created.get("id").asLong();

        mvc.perform(delete("/api/public/scripts/" + id))
           .andExpect(status().isNoContent());

        mvc.perform(get("/api/public/scripts/" + id))
           .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void delete_unknownId_returns404() throws Exception {
        mvc.perform(delete("/api/public/scripts/999999"))
           .andExpect(status().isNotFound());
    }
}
