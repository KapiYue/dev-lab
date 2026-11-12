package com.qingmou.cxnp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the MySQL unique constraints against the local Docker MySQL database.
 * Each test creates its own UUID-prefixed fixture rows; Spring rolls them back
 * with the enclosing test transaction.
 */
@SpringBootTest
@Transactional(timeout = 10)
@Timeout(30)
class UniqueConstraintMySqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void userRoleUniqueConstraintRejectsDuplicateUserAndRole() {
        Long userId = createUser();
        Long roleId = createRole();

        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);

        assertThrows(DuplicateKeyException.class,
                () -> jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                        userId, roleId));
    }

    @Test
    void rolePermissionUniqueConstraintRejectsDuplicateRoleAndPermission() {
        Long roleId = createRole();
        Long permissionId = createPermission();

        jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                roleId, permissionId);

        assertThrows(DuplicateKeyException.class,
                () -> jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                        roleId, permissionId));
    }

    @Test
    void roleDataScopeUniqueConstraintRejectsSecondScopeForRole() {
        Long roleId = createRole();

        jdbcTemplate.update("INSERT INTO sys_role_data_scope (role_id, scope_type) VALUES (?, ?)", roleId, "ALL");

        assertThrows(DuplicateKeyException.class,
                () -> jdbcTemplate.update("INSERT INTO sys_role_data_scope (role_id, scope_type) VALUES (?, ?)",
                        roleId, "SELF"));
    }

    @Test
    void roleScopeOrganizationUniqueConstraintRejectsDuplicateRoleAndOrganization() {
        Long roleId = createRole();
        Long orgId = createOrganization();

        jdbcTemplate.update("INSERT INTO sys_role_scope_org (role_id, org_id) VALUES (?, ?)", roleId, orgId);

        assertThrows(DuplicateKeyException.class,
                () -> jdbcTemplate.update("INSERT INTO sys_role_scope_org (role_id, org_id) VALUES (?, ?)",
                        roleId, orgId));
    }

    @Test
    void partyMemberUniqueConstraintRejectsSecondMemberForUser() {
        String token = newToken();
        Long userId = createUser(token);
        Long orgId = createOrganization(token);

        insertPartyMember(userId, orgId, token, "first");

        assertThrows(DuplicateKeyException.class,
                () -> insertPartyMember(userId, orgId, token, "second"));
    }

    private Long createUser() {
        return createUser(newToken());
    }

    private Long createUser(String token) {
        String username = "it_unique_user_" + token;
        jdbcTemplate.update("INSERT INTO sys_user (username, password, status) VALUES (?, ?, ?)",
                username, "integration-test-password", 1);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    private Long createRole() {
        String token = newToken();
        String roleCode = "IT_UNIQUE_ROLE_" + token;
        jdbcTemplate.update("INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, ?)",
                roleCode, "唯一约束集成测试角色 " + token, 1);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
    }

    private Long createPermission() {
        String token = newToken();
        String permissionCode = "it:unique:" + token;
        jdbcTemplate.update("INSERT INTO sys_permission (permission_code, permission_name) VALUES (?, ?)",
                permissionCode, "唯一约束集成测试权限 " + token);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_permission WHERE permission_code = ?",
                Long.class, permissionCode);
    }

    private Long createOrganization() {
        return createOrganization(newToken());
    }

    private Long createOrganization(String token) {
        String orgName = "唯一约束集成测试组织 " + token;
        jdbcTemplate.update("INSERT INTO sys_org (org_name, org_type, status) VALUES (?, ?, ?)",
                orgName, "BRANCH", 1);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_org WHERE org_name = ?", Long.class, orgName);
    }

    private void insertPartyMember(Long userId, Long orgId, String token, String suffix) {
        jdbcTemplate.update("""
                        INSERT INTO party_member (name, gender, phone, join_date, party_branch, org_id, user_id)
                        VALUES (?, ?, ?, CURRENT_DATE, ?, ?, ?)
                        """,
                "it-unique-member-" + suffix + "-" + token,
                "男",
                "139" + token.substring(0, 8),
                "唯一约束集成测试组织 " + token,
                orgId,
                userId);
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
