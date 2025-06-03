// 📁 src/test/java/com/training/blogapi/PostControllerTest.java
package com.example.blogapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blogapi.BlogApiApplication;
import com.example.blogapi.dto.PostDto;
import com.example.blogapi.model.AppUser;
import com.example.blogapi.model.Post;
import com.example.blogapi.repository.AppUserRepository;
import com.example.blogapi.repository.PostRepository;
import com.example.blogapi.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

@SpringBootTest(classes = BlogApiApplication.class)
@AutoConfigureMockMvc
@Import(TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String readerToken;
    private String userToken;
    private AppUser adminUser;
    private AppUser readerUser;
    private AppUser normalUser;

    @BeforeEach
    void setUp() {
        // Clear repositories
        postRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        adminUser = new AppUser();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRoles(Set.of("ADMIN", "READER", "USER"));
        adminUser = userRepository.save(adminUser);

        readerUser = new AppUser();
        readerUser.setUsername("reader");
        readerUser.setPassword(passwordEncoder.encode("reader123"));
        readerUser.setRoles(Set.of("READER", "USER"));
        readerUser = userRepository.save(readerUser);

        normalUser = new AppUser();
        normalUser.setUsername("user");
        normalUser.setPassword(passwordEncoder.encode("user123"));
        normalUser.setRoles(Set.of("USER"));
        normalUser = userRepository.save(normalUser);

        // Generate JWT tokens
        adminToken = jwtTokenProvider.createToken("admin");
        readerToken = jwtTokenProvider.createToken("reader");
        userToken = jwtTokenProvider.createToken("user");

        // Create test post
        Post testPost = new Post();
        testPost.setTitle("Test Post");
        testPost.setContent("This is a test post content");
        testPost.setUser(adminUser);
        postRepository.save(testPost);
    }

    @Test
    void getAllPosts_WithoutAuth_ShouldReturnTitlesOnly() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Post"))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    @Test
    void getAllPosts_WithAuth_ShouldReturnFullContent() throws Exception {
        mockMvc.perform(get("/posts")
                        .header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Post"))
                .andExpect(jsonPath("$[0].content").value("This is a test post content"));
    }

    @Test
    void createPost_AsAdmin_ShouldSucceed() throws Exception {
        PostDto newPost = new PostDto();
        newPost.setTitle("New Post");
        newPost.setContent("This is a new post with enough content");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPost)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Post"))
                .andExpect(jsonPath("$.content").value("This is a new post with enough content"));
    }

    @Test
    void createPost_AsReader_ShouldFail() throws Exception {
        PostDto newPost = new PostDto();
        newPost.setTitle("New Post");
        newPost.setContent("This is a new post with enough content");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + readerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPost)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPost_AsUser_ShouldFail() throws Exception {
        PostDto newPost = new PostDto();
        newPost.setTitle("New Post");
        newPost.setContent("This is a new post with enough content");

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPost)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPost_WithoutAuth_ShouldFail() throws Exception {
        PostDto newPost = new PostDto();
        newPost.setTitle("New Post");
        newPost.setContent("This is a new post with enough content");

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPost)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePost_AsAdmin_ShouldSucceed() throws Exception {
        PostDto updatePost = new PostDto();
        updatePost.setTitle("Updated Title");
        updatePost.setContent("Updated content with enough characters");

        mockMvc.perform(put("/posts/1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePost)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updatePost_AsReader_ShouldFail() throws Exception {
        PostDto updatePost = new PostDto();
        updatePost.setTitle("Updated Title");
        updatePost.setContent("Updated content with enough characters");

        mockMvc.perform(put("/posts/1")
                        .header("Authorization", "Bearer " + readerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePost)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePost_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(delete("/posts/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePost_AsReader_ShouldFail() throws Exception {
        mockMvc.perform(delete("/posts/1")
                        .header("Authorization", "Bearer " + readerToken))
                .andExpect(status().isForbidden());
    }
}