// 📁 src/test/java/com/training/blogapi/ValidationTest.java
package com.example.blogapi;

import com.example.blogapi.BlogApiApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blogapi.dto.PostDto;
import com.example.blogapi.model.AppUser;
import com.example.blogapi.repository.AppUserRepository;
import com.example.blogapi.security.JwtTokenProvider;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BlogApiApplication.class)
@AutoConfigureMockMvc
@Import(TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class ValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clear repository
        userRepository.deleteAll();

        AppUser adminUser = new AppUser();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRoles(Set.of("ADMIN", "READER", "USER"));
        userRepository.save(adminUser);

        adminToken = jwtTokenProvider.createToken("admin");
    }

    @Test
    void createPost_WithEmptyTitle_ShouldFail() throws Exception {
        PostDto invalidPost = new PostDto();
        invalidPost.setTitle(""); // Empty title
        invalidPost.setContent("This is valid content with enough characters");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPost)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_WithNullTitle_ShouldFail() throws Exception {
        PostDto invalidPost = new PostDto();
        invalidPost.setTitle(null); // Null title
        invalidPost.setContent("This is valid content with enough characters");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPost)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_WithTooLongTitle_ShouldFail() throws Exception {
        PostDto invalidPost = new PostDto();
        // Create a title with 256 characters (exceeds max 255)
        invalidPost.setTitle("a".repeat(256));
        invalidPost.setContent("This is valid content with enough characters");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPost)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_WithEmptyContent_ShouldFail() throws Exception {
        PostDto invalidPost = new PostDto();
        invalidPost.setTitle("Valid Title");
        invalidPost.setContent(""); // Empty content

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPost)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_WithTooShortContent_ShouldFail() throws Exception {
        PostDto invalidPost = new PostDto();
        invalidPost.setTitle("Valid Title");
        invalidPost.setContent("Short"); // Only 5 characters (min is 10)

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPost)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_WithTooLongContent_ShouldFail() throws Exception {
        PostDto invalidPost = new PostDto();
        invalidPost.setTitle("Valid Title");
        // Create content with 5001 characters (exceeds max 5000)
        invalidPost.setContent("a".repeat(5001));

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPost)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_WithValidData_ShouldSucceed() throws Exception {
        PostDto validPost = new PostDto();
        validPost.setTitle("Valid Title");
        validPost.setContent("This is valid content with exactly enough characters for validation");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPost)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Valid Title"))
                .andExpect(jsonPath("$.content").value("This is valid content with exactly enough characters for validation"));
    }

    @Test
    void createPost_WithMinimumValidContent_ShouldSucceed() throws Exception {
        PostDto validPost = new PostDto();
        validPost.setTitle("Valid Title");
        validPost.setContent("1234567890"); // Exactly 10 characters (minimum)

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPost)))
                .andExpect(status().isCreated());
    }

    @Test
    void createPost_WithMaximumValidContent_ShouldSucceed() throws Exception {
        PostDto validPost = new PostDto();
        validPost.setTitle("Valid Title");
        validPost.setContent("a".repeat(5000)); // Exactly 5000 characters (maximum)

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPost)))
                .andExpect(status().isCreated());
    }

    @Test
    void createPost_WithMaximumValidTitle_ShouldSucceed() throws Exception {
        PostDto validPost = new PostDto();
        validPost.setTitle("a".repeat(255)); // Exactly 255 characters (maximum)
        validPost.setContent("This is valid content with enough characters");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPost)))
                .andExpect(status().isCreated());
    }
}