package com.smartpantry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpantry.domain.PantryItem;
import com.smartpantry.domain.PantryStatus;
import com.smartpantry.repository.PantryItemRepository;
import com.smartpantry.repository.PantryUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smart-pantry-auth;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String PASSWORD = "VerySecurePantryPass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PantryItemRepository itemRepository;

    @Autowired
    private PantryUserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registrationClaimsLegacyItemsAndAccountsCannotAccessEachOthersPantries() throws Exception {
        PantryItem legacyItem = itemRepository.save(new PantryItem(
                "Oat milk", "Beverages", 2.0, "L", LocalDate.now().plusDays(2), PantryStatus.AVAILABLE));
        Csrf csrf = getCsrf();

        MvcResult firstRegistration = register("first@example.com", PASSWORD, csrf)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("first@example.com"))
                .andReturn();
        assertNull(firstRegistration.getRequest().getSession(false));
        MockHttpSession firstSession = session(login("first@example.com", PASSWORD, csrf));

        assertNotNull(userRepository.findByEmail("first@example.com").orElseThrow().getPasswordHash());
        assertNotEquals(PASSWORD, userRepository.findByEmail("first@example.com").orElseThrow().getPasswordHash());

        mockMvc.perform(get("/api/v1/pantry-items").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(legacyItem.getId()));

        register("second@example.com", PASSWORD, csrf)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("second@example.com"));
        MockHttpSession secondSession = session(login("second@example.com", PASSWORD, csrf));

        mockMvc.perform(get("/api/v1/pantry-items").session(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/pantry-items/{id}", legacyItem.getId()).session(secondSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/pantry-items").session(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void pantryApiRequiresLoginAndLogoutAllowsAReauthentication() throws Exception {
        Csrf csrf = getCsrf();
        mockMvc.perform(get("/api/v1/pantry-items"))
                .andExpect(status().isUnauthorized());

        register("user@example.com", PASSWORD, csrf)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"));
        MockHttpSession session = session(login("user@example.com", PASSWORD, csrf));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("email", "user@example.com", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    private Csrf getCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        return new Csrf(cookie, body.get("token").asText(), body.get("headerName").asText());
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String email, String password, Csrf csrf) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .cookie(csrf.cookie())
                .header(csrf.headerName(), csrf.token())
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private MvcResult login(String email, String password, Csrf csrf) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private MockHttpSession session(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private record Csrf(Cookie cookie, String token, String headerName) {
    }
}
