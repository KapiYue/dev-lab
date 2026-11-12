package com.qingmou.cxnp;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs through the HTTP authentication and MVC interceptor chain. The fixture
 * names are intentionally isolated from business data and are removed after
 * each test, including roles, mappings, and party members.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartyMemberHttpAuthorizationIntegrationTest {

    private static final String LIST_PERMISSION = "party:member:list";
    private static final String DETAIL_PERMISSION = "party:member:detail";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String fixtureToken;
    private Long allowedMemberId;
    private Long forbiddenMemberId;
    private Long forbiddenOrgId;
    private TestUser listOnlyUser;
    private TestUser scopedUser;
    private TestUser differentScopeUser;
    private TestUser detailOnlyUser;
    private final List<String> issuedTokens = new ArrayList<>();

    @BeforeEach
    void setUpFixture() {
        fixtureToken = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        List<Long> branchOrgIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_org WHERE status = 1 AND org_type = 'BRANCH' ORDER BY id LIMIT 2",
                Long.class);
        assertEquals(2, branchOrgIds.size(), "HTTP 验收测试需要两个启用的党支部");

        Long allowedOrgId = branchOrgIds.get(0);
        forbiddenOrgId = branchOrgIds.get(1);
        allowedMemberId = addPartyMember("允许访问", allowedOrgId);
        forbiddenMemberId = addPartyMember("越权党员", forbiddenOrgId);

        listOnlyUser = createUser("list_only");
        scopedUser = createUser("scoped");
        differentScopeUser = createUser("different_scope");
        detailOnlyUser = createUser("detail_only");

        addRole(listOnlyUser, Set.of(LIST_PERMISSION), allowedOrgId);
        addRole(scopedUser, Set.of(LIST_PERMISSION, DETAIL_PERMISSION), allowedOrgId);
        addRole(differentScopeUser, Set.of(LIST_PERMISSION, DETAIL_PERMISSION), forbiddenOrgId);
        addRole(detailOnlyUser, Set.of(DETAIL_PERMISSION), allowedOrgId);
    }

    @AfterEach
    void cleanFixture() {
        issuedTokens.forEach(token -> redisTemplate.delete("login:token:" + token));
        if (fixtureToken == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM party_member WHERE name LIKE ?", "验收-党员越权-" + fixtureToken + "%");
        jdbcTemplate.update("DELETE rso FROM sys_role_scope_org rso JOIN sys_role r ON r.id = rso.role_id "
                + "WHERE r.role_code LIKE ?", "ROLE_HTTP_MEMBER_" + fixtureToken + "%");
        jdbcTemplate.update("DELETE ds FROM sys_role_data_scope ds JOIN sys_role r ON r.id = ds.role_id "
                + "WHERE r.role_code LIKE ?", "ROLE_HTTP_MEMBER_" + fixtureToken + "%");
        jdbcTemplate.update("DELETE rp FROM sys_role_permission rp JOIN sys_role r ON r.id = rp.role_id "
                + "WHERE r.role_code LIKE ?", "ROLE_HTTP_MEMBER_" + fixtureToken + "%");
        jdbcTemplate.update("DELETE ur FROM sys_user_role ur JOIN sys_user u ON u.id = ur.user_id "
                + "WHERE u.username LIKE ?", "http_party_" + fixtureToken + "%");
        jdbcTemplate.update("DELETE FROM sys_role WHERE role_code LIKE ?", "ROLE_HTTP_MEMBER_" + fixtureToken + "%");
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE ?", "http_party_" + fixtureToken + "%");
    }

    @Test
    void authenticatesRequestsAndEnforcesIndependentListAndDetailScopes() {
        ResponseEntity<JsonNode> normalList = get("/api/party-members", login(listOnlyUser));
        assertSuccess(normalList);
        Set<Long> listOnlyIds = responseIds(normalList.getBody());
        assertTrue(listOnlyIds.contains(allowedMemberId));
        assertFalse(listOnlyIds.contains(forbiddenMemberId), "列表必须过滤其他组织党员");

        ResponseEntity<JsonNode> otherScopeList = get("/api/party-members", login(differentScopeUser));
        assertSuccess(otherScopeList);
        Set<Long> otherScopeIds = responseIds(otherScopeList.getBody());
        assertTrue(otherScopeIds.contains(forbiddenMemberId));
        assertFalse(otherScopeIds.contains(allowedMemberId), "有列表功能权限也不能读取范围外党员");

        ResponseEntity<JsonNode> allowedDetail = get("/api/party-members/" + allowedMemberId, login(scopedUser));
        assertSuccess(allowedDetail);
        assertEquals(allowedMemberId.longValue(), allowedDetail.getBody().path("data").path("id").asLong());

        ResponseEntity<JsonNode> deniedKnownId = get("/api/party-members/" + forbiddenMemberId, login(scopedUser));
        assertEquals(HttpStatus.NOT_FOUND, deniedKnownId.getStatusCode());
        assertEquals(404, deniedKnownId.getBody().path("code").asInt(), "范围外详情必须作为不可见资源拒绝");

        ResponseEntity<JsonNode> listOnlyDetail = get("/api/party-members/" + allowedMemberId, login(listOnlyUser));
        assertForbiddenForMissingPermission(listOnlyDetail);

        ResponseEntity<JsonNode> detailOnlyList = get("/api/party-members", login(detailOnlyUser));
        assertForbiddenForMissingPermission(detailOnlyList);
    }

    @Test
    void ignoresForgedSingleValueDataScopeParametersForList() {
        String token = login(scopedUser);
        Set<Long> allowedIds = permittedMemberIds(token);

        ResponseEntity<JsonNode> forgedRequest = get("/api/party-members?orgIds=" + forbiddenOrgId
                + "&orgId=" + forbiddenOrgId + "&scopeType=ALL&dataScope=ALL", token);

        assertSuccess(forgedRequest);
        assertEquals(allowedIds, responseIds(forgedRequest.getBody()), "伪造数据范围参数不得扩大列表访问范围");
        assertFalse(responseIds(forgedRequest.getBody()).contains(forbiddenMemberId), "不得返回越权组织党员");
    }

    @Test
    void ignoresRepeatedDataScopeParametersForList() {
        String token = login(scopedUser);
        Set<Long> allowedIds = permittedMemberIds(token);

        ResponseEntity<JsonNode> forgedRequest = get("/api/party-members?orgIds=10&orgIds=11&scopeType=ALL", token);

        assertSuccess(forgedRequest);
        assertEquals(allowedIds, responseIds(forgedRequest.getBody()), "重复参数不得扩大列表访问范围");
        assertFalse(responseIds(forgedRequest.getBody()).contains(forbiddenMemberId), "不得返回越权组织党员");
    }

    @Test
    void ignoresArrayStyleDataScopeParametersForList() {
        String token = login(scopedUser);
        Set<Long> allowedIds = permittedMemberIds(token);

        ResponseEntity<JsonNode> forgedRequest = get("/api/party-members?orgIds[]=11&dataScope=ALL", token);

        assertSuccess(forgedRequest);
        assertEquals(allowedIds, responseIds(forgedRequest.getBody()), "数组参数不得扩大列表访问范围");
        assertFalse(responseIds(forgedRequest.getBody()).contains(forbiddenMemberId), "不得返回越权组织党员");
    }

    @Test
    void doesNotAllowForgedDataScopeParametersToReadForbiddenDetail() {
        ResponseEntity<JsonNode> response = get("/api/party-members/" + forbiddenMemberId
                + "?orgIds=11&scopeType=ALL", login(scopedUser));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().path("code").asInt());
    }

    @Test
    void stillRejectsForgedDataScopeParametersWithoutListPermission() {
        ResponseEntity<JsonNode> response = get("/api/party-members?orgIds=11&scopeType=ALL&dataScope=ALL",
                login(detailOnlyUser));

        assertForbiddenForMissingPermission(response);
    }

    @Test
    void stillRejectsForgedDataScopeParametersWithoutToken() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/party-members?orgIds=11&scopeType=ALL&dataScope=ALL", JsonNode.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().path("code").asInt());
    }

    private TestUser createUser(String kind) {
        String username = "http_party_" + fixtureToken + "_" + kind;
        String password = "HttpPass-" + fixtureToken + "!";
        jdbcTemplate.update("INSERT INTO sys_user (username, password, status) VALUES (?, ?, 1)",
                username, passwordEncoder.encode(password));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        return new TestUser(id, username, password);
    }

    private void addRole(TestUser user, Set<String> permissionKeys, Long orgId) {
        String roleCode = "ROLE_HTTP_MEMBER_" + fixtureToken + "_" + user.username();
        jdbcTemplate.update("INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, 1)",
                roleCode, "HTTP 党员权限验收角色 " + user.username());
        Long roleId = jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", user.id(), roleId);
        jdbcTemplate.update("INSERT INTO sys_role_data_scope (role_id, scope_type) VALUES (?, 'CUSTOM')", roleId);
        jdbcTemplate.update("INSERT INTO sys_role_scope_org (role_id, org_id) VALUES (?, ?)", roleId, orgId);
        for (String permissionKey : permissionKeys) {
            jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) "
                            + "SELECT ?, id FROM sys_permission WHERE permission_code = ?",
                    roleId, permissionKey);
        }
    }

    private Long addPartyMember(String label, Long orgId) {
        String name = "验收-党员越权-" + fixtureToken + "-" + label;
        String branchName = jdbcTemplate.queryForObject("SELECT org_name FROM sys_org WHERE id = ?", String.class, orgId);
        jdbcTemplate.update("INSERT INTO party_member (name, gender, phone, join_date, party_branch, org_id) "
                        + "VALUES (?, '男', ?, CURRENT_DATE, ?, ?)",
                name, "139" + fixtureToken.substring(0, 8), branchName, orgId);
        return jdbcTemplate.queryForObject("SELECT id FROM party_member WHERE name = ?", Long.class, name);
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

    private ResponseEntity<JsonNode> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private Set<Long> permittedMemberIds(String token) {
        ResponseEntity<JsonNode> response = get("/api/party-members", token);
        assertSuccess(response);
        Set<Long> memberIds = responseIds(response.getBody());
        assertTrue(memberIds.contains(allowedMemberId), "受限用户应能读取其授权组织党员");
        assertFalse(memberIds.contains(forbiddenMemberId), "受限用户不得读取越权组织党员");
        return memberIds;
    }

    private void assertSuccess(ResponseEntity<JsonNode> response) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().path("code").asInt());
    }

    private void assertForbiddenForMissingPermission(ResponseEntity<JsonNode> response) {
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().path("code").asInt());
        assertEquals("无功能权限", response.getBody().path("message").asText());
    }

    private Set<Long> responseIds(JsonNode responseBody) {
        return StreamSupport.stream(responseBody.path("data").spliterator(), false)
                .map(member -> member.path("id").asLong())
                .collect(Collectors.toSet());
    }

    private record TestUser(Long id, String username, String password) {
    }
}
