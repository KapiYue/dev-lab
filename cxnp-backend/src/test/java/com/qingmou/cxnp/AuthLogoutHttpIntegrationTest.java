package com.qingmou.cxnp;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthLogoutHttpIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String fixtureId;
    private TestUser user;
    private final List<String> issuedTokens = new ArrayList<>();

    @BeforeEach
    void setUpFixture() {
        fixtureId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        user = createUser();
    }

    @AfterEach
    void cleanFixture() {
        issuedTokens.forEach(token -> redisTemplate.delete("login:token:" + token));
        if (user != null) {
            redisTemplate.delete("login:user-tokens:" + user.id());
        }
        if (fixtureId != null) {
            jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE ?", "http_logout_" + fixtureId + "%");
        }
    }

    @Test
    void logoutInvalidatesOnlyCurrentTokenAndRequiresAuthentication() throws IOException, InterruptedException {
        String loggedOutToken = login();
        String retainedToken = login();

        ResponseEntity<JsonNode> logoutResponse = postLogout(loggedOutToken);
        assertSuccess(logoutResponse);

        ResponseEntity<JsonNode> revokedTokenResponse = postLogout(loggedOutToken);
        assertUnauthorized(revokedTokenResponse);

        ResponseEntity<JsonNode> retainedTokenResponse = postLogout(retainedToken);
        assertSuccess(retainedTokenResponse);

        ResponseEntity<JsonNode> missingTokenResponse = postLogout(null);
        assertUnauthorized(missingTokenResponse);
    }

    private TestUser createUser() {
        String username = "http_logout_" + fixtureId;
        String password = "HttpLogout-" + fixtureId + "!";
        jdbcTemplate.update("INSERT INTO sys_user (username, password, status) VALUES (?, ?, 1)",
                username, passwordEncoder.encode(password));
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        return new TestUser(userId, username, password);
    }

    private String login() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("username", user.username(), "password", user.password()), JsonNode.class);
        assertSuccess(response);
        String token = response.getBody().path("data").asText();
        assertFalse(token.isBlank());
        issuedTokens.add(token);
        return token;
    }

    private ResponseEntity<JsonNode> postLogout(String token) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/auth/logout"))
                .POST(HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request.build(), HttpResponse.BodyHandlers.ofString());
        return new ResponseEntity<>(
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body()),
                HttpStatus.valueOf(response.statusCode()));
    }

    private void assertSuccess(ResponseEntity<JsonNode> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().path("code").asInt());
    }

    private void assertUnauthorized(ResponseEntity<JsonNode> response) {
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().path("code").asInt());
    }

    private record TestUser(Long id, String username, String password) {
    }
}
