// 📁 src/test/java/com/training/blogapi/AuthTest.java
package com.example.blogapi;

import com.example.blogapi.BlogApiApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blogapi.controller.AuthController;
import com.example.blogapi.model.AppUser;
import com.example.blogapi.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BlogApiApplication.class)
@AutoConfigureMockMvc
@Import(TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class AuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clear repository
        userRepository.deleteAll();

        // Create test users
        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(Set.of("ADMIN", "READER", "USER"));
        userRepository.save(admin);

        AppUser berta = new AppUser();
        berta.setUsername("berta");
        berta.setPassword(passwordEncoder.encode("berta123"));
        berta.setRoles(Set.of("READER", "USER"));
        userRepository.save(berta);

        AppUser max15 = new AppUser();
        max15.setUsername("max15");
        max15.setPassword(passwordEncoder.encode("pass123"));
        max15.setRoles(Set.of("USER"));
        userRepository.save(max15);
    }

    @Test
    void login_WithValidAdminCredentials_ShouldSucceed() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("admin", "admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_WithValidBertaCredentials_ShouldSucceed() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("berta", "berta123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_WithValidMax15Credentials_ShouldSucceed() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("max15", "pass123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_WithInvalidUsername_ShouldFail() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("wronguser", "admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Ungültige Anmeldedaten"));
    }

    @Test
    void login_WithInvalidPassword_ShouldFail() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("admin", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Ungültige Anmeldedaten"));
    }

    @Test
    void login_WithEmptyCredentials_ShouldFail() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithNullCredentials_ShouldFail() throws Exception {
        AuthController.AuthRequest request = new AuthController.AuthRequest(null, null);

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_WithoutToken_ShouldFail() throws Exception {
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"Test content with enough characters\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_WithInvalidToken_ShouldFail() throws Exception {
        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer invalid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"Test content with enough characters\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_WithMalformedAuthHeader_ShouldFail() throws Exception {
        mockMvc.perform(post("/posts")
                        .header("Authorization", "InvalidFormat token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"content\":\"Test content with enough characters\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meEndpoint_WithoutToken_ShouldFail() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Nicht eingeloggt"));
    }

    @Test
    void getToken_AndCheckMeEndpoint_ShouldReturnUserInfo() throws Exception {
        // First, login to get token
        AuthController.AuthRequest request = new AuthController.AuthRequest("admin", "admin123");

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token from response
        String token = objectMapper.readTree(response).get("token").asText();

        // Use token to access /me endpoint
        mockMvc.perform(get("/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[*]").value(org.hamcrest.Matchers.hasItems("ROLE_ADMIN", "ROLE_READER", "ROLE_USER")));
    }
}