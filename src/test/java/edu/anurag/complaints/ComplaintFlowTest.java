package edu.anurag.complaints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ComplaintFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode doPost(String url, String token, String payload, int expected) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(payload);
        if (token != null) req.header("Authorization", "Bearer " + token);
        String res = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return res.isBlank() ? null : json.readTree(res);
    }

    private JsonNode doGet(String url, String token, int expected) throws Exception {
        var req = get(url);
        if (token != null) req.header("Authorization", "Bearer " + token);
        String res = mvc.perform(req).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return res.isBlank() ? null : json.readTree(res);
    }

    @Test
    void fullFlow() throws Exception {
        // unauthenticated access is rejected
        doGet("/api/complaints", null, 401);

        // two students register
        String s1 = doPost("/api/auth/register", null,
                "{\"name\":\"Asha\",\"email\":\"asha@test.edu\",\"password\":\"password123\",\"rollNumber\":\"21EG1\",\"department\":\"CSE\"}", 201)
                .get("token").asText();
        String s2 = doPost("/api/auth/register", null,
                "{\"name\":\"Ravi\",\"email\":\"ravi@test.edu\",\"password\":\"password123\"}", 201)
                .get("token").asText();

        // duplicate email and short password
        doPost("/api/auth/register", null, "{\"name\":\"X\",\"email\":\"asha@test.edu\",\"password\":\"password123\"}", 409);
        doPost("/api/auth/register", null, "{\"name\":\"X\",\"email\":\"x@test.edu\",\"password\":\"short\"}", 400);

        // wrong password
        doPost("/api/auth/login", null, "{\"email\":\"asha@test.edu\",\"password\":\"nope\"}", 401);

        // student creates a complaint
        JsonNode created = doPost("/api/complaints", s1,
                "{\"title\":\"Hostel water\",\"description\":\"No water on 3rd floor\",\"category\":\"HOSTEL\",\"priority\":\"HIGH\"}", 201);
        long id = created.get("id").asLong();
        assertEquals("OPEN", created.get("status").asText());

        // student sees own, other student sees none and cannot open it
        assertEquals(1, doGet("/api/complaints", s1, 200).size());
        assertEquals(0, doGet("/api/complaints", s2, 200).size());
        doGet("/api/complaints/" + id, s2, 404);

        // student cannot use admin endpoints
        doGet("/api/complaints/stats", s1, 403);
        mvc.perform(patch("/api/complaints/" + id + "/status").header("Authorization", "Bearer " + s1)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden());

        // admin logs in, updates, sees stats
        String admin = doPost("/api/auth/login", null, "{\"email\":\"admin@test.edu\",\"password\":\"AdminPass123\"}", 200)
                .get("token").asText();
        mvc.perform(patch("/api/complaints/" + id + "/status").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"adminResponse\":\"Plumber assigned\"}"))
                .andExpect(status().isOk());
        assertEquals(1, doGet("/api/complaints?status=IN_PROGRESS", admin, 200).size());
        JsonNode stats = doGet("/api/complaints/stats", admin, 200);
        assertEquals(1, stats.get("total").asInt());
        assertEquals(1, stats.get("byStatus").get("IN_PROGRESS").asInt());

        // admin cannot file complaints; student cannot withdraw once in progress
        doPost("/api/complaints", admin, "{\"title\":\"t\",\"description\":\"d\",\"category\":\"OTHER\"}", 403);
        mvc.perform(delete("/api/complaints/" + id).header("Authorization", "Bearer " + s1))
                .andExpect(status().isForbidden());

        // student sees admin response
        assertEquals("Plumber assigned", doGet("/api/complaints/" + id, s1, 200).get("adminResponse").asText());
    }
}
