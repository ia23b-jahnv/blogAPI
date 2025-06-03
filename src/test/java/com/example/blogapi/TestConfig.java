// 📁 src/test/java/com/training/blogapi/TestConfig.java
package com.example.blogapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test configuration class for optimizing test performance
 * and providing test-specific beans.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides a faster password encoder for tests.
     * Uses BCrypt with strength 4 instead of default 10 for faster test execution.
     * The @Primary annotation ensures this bean is used instead of the main one during tests.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        // Use lower strength (4) for faster tests - default is 10
        return new BCryptPasswordEncoder(4);
    }

    /**
     * Optional: You can add more test-specific configurations here if needed
     * For example:
     * - Mock services
     * - Test-specific JWT settings
     * - Custom test data configurations
     */
}