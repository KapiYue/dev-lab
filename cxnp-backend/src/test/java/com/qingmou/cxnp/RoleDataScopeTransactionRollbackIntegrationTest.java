package com.qingmou.cxnp;

import com.qingmou.cxnp.dto.RoleDataScopeConfigDTO;
import com.qingmou.cxnp.enums.RoleDataScopeType;
import com.qingmou.cxnp.service.RoleDataScopeConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the service transaction against the Docker MySQL database. This
 * test deliberately has no outer transaction: {@code configure} must roll
 * back its own multi-table transaction after the database rejects its second
 * new organization binding.
 */
@SpringBootTest
@Timeout(30)
class RoleDataScopeTransactionRollbackIntegrationTest {

    private static final String FIRST_INSERT_SEEN = "CXNP_FIRST_SCOPE_INSERT_SEEN";
    private static final String FIRST_INSERT_NOT_SEEN = "CXNP_FIRST_SCOPE_INSERT_NOT_SEEN";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleDataScopeConfigService roleDataScopeConfigService;

    @Test
    void configureRollsBackScopeAndOrganizationBindingsWhenSecondInsertFails() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String triggerName = "trg_cxnp_tx_" + token.substring(0, 20);
        String firstInsertSessionVariable = "@cxnp_first_scope_insert_" + token.substring(0, 16);
        Long roleId = null;
        List<Long> organizationIds = new ArrayList<>();

        try {
            assertTrue(AopUtils.isAopProxy(roleDataScopeConfigService),
                    "必须通过 Spring 注入的事务代理调用 configure");

            roleId = createRole(token);
            Long oldOrgId = createOrganization(token, "old");
            organizationIds.add(oldOrgId);
            Long candidateOrgId1 = createOrganization(token, "new-a");
            organizationIds.add(candidateOrgId1);
            Long candidateOrgId2 = createOrganization(token, "new-b");
            organizationIds.add(candidateOrgId2);
            List<Long> orderedNewOrgIds = orderAsConfigureWillInsert(candidateOrgId1, candidateOrgId2);
            Long newOrgId1 = orderedNewOrgIds.get(0);
            Long newOrgId2 = orderedNewOrgIds.get(1);
            Long configuredRoleId = roleId;

            jdbcTemplate.update("INSERT INTO sys_role_data_scope (role_id, scope_type) VALUES (?, ?)",
                    configuredRoleId, RoleDataScopeType.ORG_AND_CHILD.name());
            jdbcTemplate.update("INSERT INTO sys_role_scope_org (role_id, org_id) VALUES (?, ?)", configuredRoleId, oldOrgId);
            DatabaseSnapshot before = snapshot(configuredRoleId);
            assertEquals(new DatabaseSnapshot(RoleDataScopeType.ORG_AND_CHILD.name(), Set.of(oldOrgId)), before);

            createSecondInsertFailureTrigger(triggerName, firstInsertSessionVariable, configuredRoleId, newOrgId1, newOrgId2);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> roleDataScopeConfigService.configure(new RoleDataScopeConfigDTO(
                            configuredRoleId, RoleDataScopeType.CUSTOM, List.of(newOrgId1, newOrgId2))));

            // The trigger can emit this message only after its earlier branch observed
            // the first new binding INSERT on the same configure transaction connection.
            assertTrue(exceptionMessageContains(thrown, FIRST_INSERT_SEEN),
                    "第二条 INSERT 的数据库错误必须证明第一条新组织 INSERT 已实际执行");

            DatabaseSnapshot after = snapshot(configuredRoleId);
            assertEquals(before.scopeType(), after.scopeType(), "范围类型更新必须回滚");
            assertEquals(before.orgIds(), after.orgIds(), "旧组织删除和新组织写入必须整体回滚");
            assertTrue(after.orgIds().contains(oldOrgId), "旧组织绑定必须仍存在");
            assertTrue(!after.orgIds().contains(newOrgId1) && !after.orgIds().contains(newOrgId2),
                    "两个新组织绑定都不得提交");
        } finally {
            cleanup(triggerName, firstInsertSessionVariable, roleId, organizationIds);
        }
    }

    private Long createRole(String token) {
        String roleCode = "IT_TX_ROLLBACK_" + token.substring(0, 20);
        jdbcTemplate.update("INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, 1)",
                roleCode, "事务回滚集成测试角色 " + token.substring(0, 12));
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
    }

    private Long createOrganization(String token, String suffix) {
        String orgName = "IT_TX_ROLLBACK_" + suffix + "_" + token.substring(0, 20);
        jdbcTemplate.update("INSERT INTO sys_org (org_name, org_type, status) VALUES (?, 'BRANCH', 1)", orgName);
        return jdbcTemplate.queryForObject("SELECT id FROM sys_org WHERE org_name = ?", Long.class, orgName);
    }

    private List<Long> orderAsConfigureWillInsert(Long firstCandidate, Long secondCandidate) {
        // configure normalizes with HashSet before iterating. Mirror that order so
        // newOrgId2 is deterministically the INSERT which the trigger rejects.
        return new java.util.ArrayList<>(new HashSet<>(List.of(firstCandidate, secondCandidate)));
    }

    private void createSecondInsertFailureTrigger(
            String triggerName,
            String firstInsertSessionVariable,
            Long roleId,
            Long newOrgId1,
            Long newOrgId2
    ) {
        jdbcTemplate.execute("""
                CREATE TRIGGER `%s`
                BEFORE INSERT ON sys_role_scope_org
                FOR EACH ROW
                BEGIN
                    IF NEW.role_id = %d AND NEW.org_id = %d THEN
                        SET %s = 1;
                    END IF;
                    IF NEW.role_id = %d AND NEW.org_id = %d THEN
                        IF COALESCE(%s, 0) = 1 THEN
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '%s';
                        ELSE
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '%s';
                        END IF;
                    END IF;
                END
                """.formatted(triggerName, roleId, newOrgId1, firstInsertSessionVariable,
                roleId, newOrgId2, firstInsertSessionVariable, FIRST_INSERT_SEEN, FIRST_INSERT_NOT_SEEN));
    }

    private DatabaseSnapshot snapshot(Long roleId) {
        String scopeType = jdbcTemplate.queryForObject(
                "SELECT scope_type FROM sys_role_data_scope WHERE role_id = ?", String.class, roleId);
        Set<Long> orgIds = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT org_id FROM sys_role_scope_org WHERE role_id = ?", Long.class, roleId));
        return new DatabaseSnapshot(scopeType, orgIds);
    }

    private boolean exceptionMessageContains(Throwable throwable, String expectedMessage) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expectedMessage)) {
                return true;
            }
        }
        return false;
    }

    private void cleanup(
            String triggerName,
            String firstInsertSessionVariable,
            Long roleId,
            List<Long> organizationIds
    ) {
        try {
            try {
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS `" + triggerName + "`");
            } finally {
                jdbcTemplate.execute("SET " + firstInsertSessionVariable + " = NULL");
            }
        } finally {
            if (roleId != null) {
                try {
                    jdbcTemplate.update("DELETE FROM sys_role_scope_org WHERE role_id = ?", roleId);
                    jdbcTemplate.update("DELETE FROM sys_role_data_scope WHERE role_id = ?", roleId);
                } finally {
                    jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", roleId);
                }
            }
            for (Long organizationId : organizationIds) {
                jdbcTemplate.update("DELETE FROM sys_org WHERE id = ?", organizationId);
            }
            if (roleId != null) {
                assertEquals(0, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_role_data_scope WHERE role_id = ?", Integer.class, roleId),
                        "清理后不得残留角色数据范围");
                assertEquals(0, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_role_scope_org WHERE role_id = ?", Integer.class, roleId),
                        "清理后不得残留角色组织绑定");
            }
            for (Long organizationId : organizationIds) {
                assertEquals(0, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_org WHERE id = ?", Integer.class, organizationId),
                        "清理后不得残留测试组织");
            }
        }
    }

    private record DatabaseSnapshot(String scopeType, Set<Long> orgIds) {
    }
}
