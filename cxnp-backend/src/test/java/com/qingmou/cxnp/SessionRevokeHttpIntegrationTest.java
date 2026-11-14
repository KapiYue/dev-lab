package com.qingmou.cxnp;

import com.fasterxml.jackson.databind.JsonNode;
import com.qingmou.cxnp.constant.PermissionConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionRevokeHttpIntegrationTest {

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
    private TestUser administrator;
    private TestUser unauthorizedUser;
    private TestUser targetUser;
    private final List<String> issuedTokens = new ArrayList<>();

    @BeforeEach
    void setUpFixture() {
        fixtureId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ensureSessionRevokePermission();
        administrator = createUser("administrator");
        unauthorizedUser = createUser("unauthorized");
        targetUser = createUser("target");
        grantPermission(administrator, PermissionConstants.RBAC_USER_SESSION_REVOKE);
    }

    @AfterEach
    void cleanFixture() {
        issuedTokens.forEach(token -> redisTemplate.delete("login:token:" + token));
        if (administrator != null) {
            redisTemplate.delete("login:user-tokens:" + administrator.id());
        }
        if (unauthorizedUser != null) {
            redisTemplate.delete("login:user-tokens:" + unauthorizedUser.id());
        }
        if (targetUser != null) {
            redisTemplate.delete("login:user-tokens:" + targetUser.id());
        }
        if (fixtureId == null) {
            return;
        }
        jdbcTemplate.update("DELETE rp FROM sys_role_permission rp JOIN sys_role r ON r.id = rp.role_id "
                + "WHERE r.role_code LIKE ?", "ROLE_HTTP_SESSION_" + fixtureId + "%");
        jdbcTemplate.update("DELETE ur FROM sys_user_role ur JOIN sys_user u ON u.id = ur.user_id "
                + "WHERE u.username LIKE ?", "http_session_" + fixtureId + "%");
        jdbcTemplate.update("DELETE FROM sys_role WHERE role_code LIKE ?", "ROLE_HTTP_SESSION_" + fixtureId + "%");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE ?", "http_session_" + fixtureId + "%");
    }

    @Test
    void revokesAllTargetSessionsAndEnforcesPermissionAndInputContract()
            throws IOException, InterruptedException {
        String administratorToken = login(administrator);
        String firstTargetToken = login(targetUser);
        String secondTargetToken = login(targetUser);

        assertSuccess(revoke(administratorToken, targetUser.id()));
        assertUnauthorized(logout(firstTargetToken));
        assertUnauthorized(logout(secondTargetToken));

        String retainedTargetToken = login(targetUser);
        assertForbidden(revoke(login(unauthorizedUser), targetUser.id()));
        assertSuccess(logout(retainedTargetToken));

        assertBadRequest(revoke(administratorToken, 0L));
        assertNotFound(revoke(administratorToken, Long.MAX_VALUE));
        assertSuccess(revoke(administratorToken, targetUser.id()));
    }

    private void ensureSessionRevokePermission() {
        jdbcTemplate.update("""
                INSERT INTO sys_permission (permission_code, permission_name)
                VALUES (?, '撤销用户全部会话')
                ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name)
                """, PermissionConstants.RBAC_USER_SESSION_REVOKE);
    }

    private TestUser createUser(String kind) {
        String username = "http_session_" + fixtureId + "_" + kind;
        String password = "HttpSession-" + fixtureId + "!";
        jdbcTemplate.update("INSERT INTO sys_user (username, password, status) VALUES (?, ?, 1)",
                username, passwordEncoder.encode(password));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        return new TestUser(id, username, password);
    }

    private void grantPermission(TestUser user, String permissionKey) {
        String roleCode = "ROLE_HTTP_SESSION_" + fixtureId + "_" + user.username();
        jdbcTemplate.update("INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, 1)",
                roleCode, "HTTP 会话撤销验收角色 " + user.username());
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", user.id(), roleId);
        jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) "
                        + "SELECT ?, id FROM sys_permission WHERE permission_code = ?",
                roleId, permissionKey);
    }

    private String login(TestUser user) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("username", user.username(), "password", user.password()), JsonNode.class);
        assertSuccess(response);
        String token = response.getBody().path("data").asText();
        assertFalse(token.isBlank());
        issuedTokens.add(token);
        return token;
    }

    private ResponseEntity<JsonNode> revoke(String token, Long userId)
            throws IOException, InterruptedException {
        return post("/api/users/" + userId + "/sessions/revoke", token);
    }

    private ResponseEntity<JsonNode> logout(String token)
            throws IOException, InterruptedException {
        return post("/api/auth/logout", token);
    }

    private ResponseEntity<JsonNode> post(String path, String token)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString());
        return new ResponseEntity<>(
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body()),
                HttpStatus.valueOf(response.statusCode()));
    }

    private void assertSuccess(ResponseEntity<JsonNode> response) {
        assertStatus(response, HttpStatus.OK, 200);
    }

    private void assertUnauthorized(ResponseEntity<JsonNode> response) {
        assertError(response, HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");
    }

    private void assertForbidden(ResponseEntity<JsonNode> response) {
        assertError(response, HttpStatus.FORBIDDEN, 403, "无功能权限");
    }

    private void assertBadRequest(ResponseEntity<JsonNode> response) {
        assertStatus(response, HttpStatus.BAD_REQUEST, 400);
    }

    private void assertNotFound(ResponseEntity<JsonNode> response) {
        assertStatus(response, HttpStatus.NOT_FOUND, 404);
    }

    private void assertStatus(ResponseEntity<JsonNode> response, HttpStatus status, int code) {
        assertEquals(status, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(code, response.getBody().path("code").asInt());
    }

    private void assertError(
            ResponseEntity<JsonNode> response,
            HttpStatus status,
            int code,
            String message
    ) {
        assertStatus(response, status, code);
        assertEquals(message, response.getBody().path("message").asText());
        assertTrue(response.getBody().path("data").isNull());
    }

    private record TestUser(Long id, String username, String password) {
    }
}
