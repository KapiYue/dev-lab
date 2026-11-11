package com.qingmou.cxnp;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.exception.PermissionDeniedException;
import com.qingmou.cxnp.service.DataScopeService;
import com.qingmou.cxnp.service.PostPermissionDelegationService;
import com.qingmou.cxnp.service.ProjectAuthorizationService;
import com.qingmou.cxnp.vo.DataScopeResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.mybatis.spring.SqlSessionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uses the local cxnp fixture data. Every mutation is rolled back after the
 * test, so validation leaves neither delegated privileges nor user-status
 * changes in the database.
 */
@SpringBootTest
@Transactional
class AuthorizationIntegrationTest {

    @Autowired
    private ProjectAuthorizationService projectAuthorizationService;
    @Autowired
    private DataScopeService dataScopeService;
    @Autowired
    private PostPermissionDelegationService delegationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void enforcesProjectBoundaryAndCalculatesSelfOrganizationScope() {
        Long testUserId = userId("testuser");
        Long projectAId = projectId("PROJECT_A");
        Long projectBId = projectId("PROJECT_B");
        Long expectedOrgId = postOrgId(testUserId, projectAId, "CONSTRUCTION_WORKER");

        UserContext.setUserId(testUserId);

        assertDoesNotThrow(() -> projectAuthorizationService.checkProjectAccess(projectAId));
        assertThrows(PermissionDeniedException.class,
                () -> projectAuthorizationService.checkProjectAccess(projectBId));

        DataScopeResultVO scope = dataScopeService.getAccessibleScope(
                projectAId, "CONSTRUCTION", "construction:progress:view");
        assertFalse(scope.all());
        assertFalse(scope.self(), "SELF is materialized as an organization set, not a personal-record flag");
        assertEquals(1, scope.orgIds().size());
        assertTrue(scope.canAccessOrg(expectedOrgId));
    }

    @Test
    void delegatesAndRevokesWithinOneTransactionWithoutLeavingDataBehind() {
        Long sourceUserId = userId("construction_manager");
        Long projectAId = projectId("PROJECT_A");
        Long sourcePostId = postId(sourceUserId, projectAId, "CONSTRUCTION_MANAGER");
        Long targetPostId = postId(userId("testuser"), projectAId, "CONSTRUCTION_WORKER");
        Long permissionId = permissionId("construction:progress:view");

        // Fixture managers start disabled; this change is rolled back with the test.
        jdbcTemplate.update("UPDATE sys_user SET status = 1 WHERE id = ?", sourceUserId);
        // The fixture already grants this permission to testuser. Remove it only
        // inside this rollback-only test to prove delegation writes it back.
        jdbcTemplate.update("DELETE FROM sys_post_permission_grant WHERE post_id = ? AND permission_id = ?",
                targetPostId, permissionId);
        UserContext.setUserId(sourceUserId);

        delegationService.delegatePermission(sourcePostId, targetPostId, projectAId,
                "CONSTRUCTION", "construction:progress:view");

        assertEquals(1, count("SELECT COUNT(*) FROM sys_post_permission_grant "
                + "WHERE post_id = ? AND permission_id = ?", targetPostId, permissionId));
        assertEquals(1, count("SELECT COUNT(*) FROM sys_post_permission_delegation "
                + "WHERE source_post_id = ? AND target_post_id = ? AND permission_id = ? AND status = 'ACTIVE'",
                sourcePostId, targetPostId, permissionId));
        assertThrows(PermissionDeniedException.class,
                () -> delegationService.delegatePermission(sourcePostId, targetPostId, projectAId,
                        "CONSTRUCTION", "construction:progress:view"));

        delegationService.revokePermission(sourcePostId, targetPostId, projectAId,
                "CONSTRUCTION", "construction:progress:view");

        assertEquals(0, count("SELECT COUNT(*) FROM sys_post_permission_grant "
                + "WHERE post_id = ? AND permission_id = ?", targetPostId, permissionId));
        assertEquals(1, count("SELECT COUNT(*) FROM sys_post_permission_delegation "
                + "WHERE source_post_id = ? AND target_post_id = ? AND permission_id = ? AND status = 'REVOKED'",
                sourcePostId, targetPostId, permissionId));
    }

    @Test
    void calculatesAllAndCustomScopesInsideTheActiveProjectOrganizationBoundary() {
        Long testUserId = userId("testuser");
        Long projectAId = projectId("PROJECT_A");
        Long ownOrgId = postOrgId(testUserId, projectAId, "CONSTRUCTION_WORKER");
        Long scopeId = jdbcTemplate.queryForObject("SELECT id FROM sys_post_data_scope WHERE post_id = "
                + "(SELECT id FROM sys_user_post WHERE user_id = ? AND project_id = ?)",
                Long.class, testUserId, projectAId);
        Long disabledProjectOrgId = jdbcTemplate.queryForObject("""
                SELECT org_id FROM sys_project_org
                WHERE project_id = ? AND org_id <> ? LIMIT 1
                """, Long.class, projectAId, ownOrgId);
        Long outsideProjectOrgId = jdbcTemplate.queryForObject("""
                SELECT o.id FROM sys_org o
                WHERE NOT EXISTS (
                    SELECT 1 FROM sys_project_org po
                    WHERE po.project_id = ? AND po.org_id = o.id
                ) LIMIT 1
                """, Long.class, projectAId);

        UserContext.setUserId(testUserId);
        jdbcTemplate.update("UPDATE sys_post_data_scope SET scope_type = 'ALL' WHERE id = ?", scopeId);
        jdbcTemplate.update("UPDATE sys_org SET status = 0 WHERE id = ?", disabledProjectOrgId);

        DataScopeResultVO allScope = dataScopeService.getAccessibleScope(
                projectAId, "CONSTRUCTION", "construction:progress:view");
        assertTrue(allScope.all());
        assertTrue(allScope.canAccessOrg(ownOrgId));
        assertFalse(allScope.canAccessOrg(disabledProjectOrgId));

        jdbcTemplate.update("UPDATE sys_org SET status = 1 WHERE id = ?", disabledProjectOrgId);
        jdbcTemplate.update("UPDATE sys_post_data_scope SET scope_type = 'CUSTOM' WHERE id = ?", scopeId);
        jdbcTemplate.update("INSERT INTO sys_post_scope_org (post_scope_id, org_id) VALUES (?, ?)",
                scopeId, ownOrgId);
        jdbcTemplate.update("INSERT INTO sys_post_scope_org (post_scope_id, org_id) VALUES (?, ?)",
                scopeId, outsideProjectOrgId);
        // JdbcTemplate mutations bypass the mapper session; clear its local
        // cache so this assertion exercises the changed CUSTOM configuration.
        sqlSessionTemplate.clearCache();

        DataScopeResultVO customScope = dataScopeService.getAccessibleScope(
                projectAId, "CONSTRUCTION", "construction:progress:view");
        assertFalse(customScope.all());
        assertEquals(1, customScope.orgIds().size());
        assertTrue(customScope.canAccessOrg(ownOrgId));
        assertFalse(customScope.canAccessOrg(outsideProjectOrgId));
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    private Long projectId(String projectCode) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_project WHERE project_code = ?", Long.class, projectCode);
    }

    private Long permissionId(String permissionCode) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_permission WHERE permission_code = ?",
                Long.class, permissionCode);
    }

    private Long postId(Long userId, Long projectId, String roleCode) {
        return jdbcTemplate.queryForObject("""
                SELECT up.id
                FROM sys_user_post up
                JOIN sys_role r ON r.id = up.role_id
                WHERE up.user_id = ? AND up.project_id = ? AND r.role_code = ?
                """, Long.class, userId, projectId, roleCode);
    }

    private Long postOrgId(Long userId, Long projectId, String roleCode) {
        return jdbcTemplate.queryForObject("""
                SELECT up.org_id
                FROM sys_user_post up
                JOIN sys_role r ON r.id = up.role_id
                WHERE up.user_id = ? AND up.project_id = ? AND r.role_code = ?
                """, Long.class, userId, projectId, roleCode);
    }

    private Integer count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Integer.class, args);
    }
}
