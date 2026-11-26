package com.collective404.bankclient.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.collective404.bankclient.model.AppUser;
import com.collective404.bankclient.repository.AppUserRepository;
import com.collective404.bankclient.repository.ClientBudgetRepository;
import com.collective404.bankclient.repository.PurchaseMetadataRepository;
import com.collective404.bankclient.repository.TrackedAccountRepository;
import com.collective404.bankclient.service.DemoDataSeeder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bankclient-api;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class ClientApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private TrackedAccountRepository accounts;

    @Autowired
    private ClientBudgetRepository budgets;

    @Autowired
    private PurchaseMetadataRepository purchases;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void clearData() {
        purchases.deleteAll();
        budgets.deleteAll();
        accounts.deleteAll();
        users.deleteAll();
    }

    @Test
    void signedInUsersCanOnlySeeTheirOwnTrackingAccounts() throws Exception {
        final String firstToken = signUp("first-user");
        final String secondToken = signUp("second-user");
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Everyday",
                                  "type": "checking",
                                  "institution": "Local"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Everyday"));

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Everyday"));
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoCredentialsCanLogInAndHaveSeededBudgets() throws Exception {
        demoDataSeeder.run(new DefaultApplicationArguments(new String[0]));
        demoDataSeeder.run(new DefaultApplicationArguments(new String[0]));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "harbor.alex",
                                  "password": "HarborDemo!2026"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("harbor.alex"));
        final AppUser user = users
                .findByUsernameIgnoreCase("harbor.alex")
                .orElseThrow();
        assertEquals(4, budgets.findByOwnerIdOrderByNameAsc(user.getId()).size(),
                "demo user has their assigned pre-seeded budgets");
    }

    private String signUp(final String username) throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Credentials(username,
                                        "long-enough-password"))))
                .andExpect(status().isOk())
                .andReturn();
        final JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private record Credentials(String username, String password) {
    }
}
