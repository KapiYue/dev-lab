package com.qingmou.cxnp;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthLoginLockHttpIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String username;

    @BeforeEach
    void setUpFixture() {
        username = "http_lock_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("INSERT INTO sys_user (username, password, status) VALUES (?, ?, 1)",
                username, passwordEncoder.encode("Correct-password-1!"));
    }

    @AfterEach
    void cleanFixture() {
        if (username != null) {
            redisTemplate.delete("login:failure:" + username);
            redisTemplate.delete("login:lock:" + username);
            jdbcTemplate.update("DELETE FROM sys_user WHERE username = ?", username);
        }
    }

    @Test
    void returnsUnifiedTooManyRequestsResponseForTemporarilyLockedLogin()
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertUnauthorized(login("incorrect-password"));
        }

        assertTooManyRequests(login("Correct-password-1!"));
    }

    private ResponseEntity<JsonNode> login(String password)
            throws IOException, InterruptedException {
        String body = "{\"username\":\"" + username
                + "\",\"password\":\"" + password + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString());
        return new ResponseEntity<>(
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body()),
                HttpStatus.valueOf(response.statusCode()));
    }

    private void assertUnauthorized(ResponseEntity<JsonNode> response) {
        assertError(response, HttpStatus.UNAUTHORIZED, 401);
    }

    private void assertTooManyRequests(ResponseEntity<JsonNode> response) {
        assertError(response, HttpStatus.TOO_MANY_REQUESTS, 429);
    }

    private void assertError(ResponseEntity<JsonNode> response, HttpStatus status, int code) {
        assertEquals(status, response.getStatusCode());
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals(code, body.path("code").asInt());
        assertEquals("用户名或密码错误", body.path("message").asText());
        assertTrue(body.path("data").isNull());
    }
}
