package com.volcano.blog.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return security headers")
    void shouldReturnSecurityHeaders() throws Exception {
        // Since HSTS is usually only sent over HTTPS, and Spring Security's HstsHeaderWriter
        // respects that, we might not see HSTS in a standard mockMvc request unless we simulate HTTPS.
        // However, X-Frame-Options, X-Content-Type-Options, and CSP should be present.

        mockMvc.perform(get("/health").secure(true)) // Simulate HTTPS for HSTS
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                // Match HSTS header allowing for optional whitespace around the semicolon
                .andExpect(header().string("Strict-Transport-Security", matchesPattern("max-age=31536000\\s*;\\s*includeSubDomains")))
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';"));
    }
}
