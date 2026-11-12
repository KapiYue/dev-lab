package com.qingmou.cxnp;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.entity.PartyMember;
import com.qingmou.cxnp.entity.SysRoleDataScope;
import com.qingmou.cxnp.enums.RoleDataScopeType;
import com.qingmou.cxnp.mapper.PartyMemberMapper;
import com.qingmou.cxnp.mapper.RoleDataScopeMapper;
import com.qingmou.cxnp.service.RoleDataScopeService;
import com.qingmou.cxnp.vo.RoleDataScopeResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the role data-scope SQL against the local MySQL fixture. All rows
 * created here belong to the enclosing test transaction and are rolled back.
 */
@SpringBootTest
@Transactional
class RoleDataScopeMySqlIntegrationTest {

    private static final String PARTY_MEMBER_LIST = "party:member:list";
    private static final long ROOT_ORG_ID = 9L;
    private static final long FIRST_BRANCH_ORG_ID = 10L;
    private static final long SECOND_BRANCH_ORG_ID = 11L;
    private static final long THIRD_BRANCH_ORG_ID = 12L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleDataScopeService roleDataScopeService;

    @Autowired
    private RoleDataScopeMapper roleDataScopeMapper;

    @Autowired
    private PartyMemberMapper partyMemberMapper;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void allScopeReturnsEveryPartyMember() {
        TestUser user = createUser();
        Long roleId = addRole(user.id(), RoleDataScopeType.ALL, true);

        UserContext.setUserId(user.id());

        RoleDataScopeResultVO scope = roleDataScopeService.getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertEffectiveRoleIds(user.id(), Set.of(roleId));
        assertTrue(scope.all());
        assertFalse(scope.includeSelf());
        assertEquals(allPartyMemberIds(), partyMemberIds(scope));
        assertTrue(scope.orgIds().isEmpty());
    }

    @Test
    void orgAndChildScopeReturnsTheExistingOrganizationTreeMembers() {
        assertRequiredOrganizationTree();
        TestUser user = createUser();
        Long roleId = addRole(user.id(), RoleDataScopeType.ORG_AND_CHILD, true, ROOT_ORG_ID);

        UserContext.setUserId(user.id());

        RoleDataScopeResultVO scope = roleDataScopeService.getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertEffectiveRoleIds(user.id(), Set.of(roleId));
        assertFalse(scope.all());
        assertEquals(Set.of(ROOT_ORG_ID, FIRST_BRANCH_ORG_ID, SECOND_BRANCH_ORG_ID, THIRD_BRANCH_ORG_ID),
                scope.orgIds());
        assertEquals(partyMemberIdsInOrganizations(Set.of(
                ROOT_ORG_ID, FIRST_BRANCH_ORG_ID, SECOND_BRANCH_ORG_ID, THIRD_BRANCH_ORG_ID)), partyMemberIds(scope));
    }

    @Test
    void customScopeReturnsOnlyItsConfiguredOrganizationMembers() {
        assertRequiredOrganizationTree();
        TestUser user = createUser();
        Long roleId = addRole(user.id(), RoleDataScopeType.CUSTOM, true, SECOND_BRANCH_ORG_ID);

        UserContext.setUserId(user.id());

        RoleDataScopeResultVO scope = roleDataScopeService.getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertEffectiveRoleIds(user.id(), Set.of(roleId));
        assertEquals(Set.of(SECOND_BRANCH_ORG_ID), scope.orgIds());
        assertEquals(partyMemberIdsInOrganizations(Set.of(SECOND_BRANCH_ORG_ID)), partyMemberIds(scope));
        assertFalse(scope.all());
        assertFalse(scope.includeSelf());
        assertEquals(Set.of(SECOND_BRANCH_ORG_ID), scope.orgIds());
    }

    @Test
    void selfScopeReturnsOnlyThePartyMemberLinkedToTheCurrentTestUser() {
        assertRequiredOrganizationTree();
        TestUser user = createUser();
        Long roleId = addRole(user.id(), RoleDataScopeType.SELF, true);
        Long selfPartyMemberId = addPartyMemberForUser(user, THIRD_BRANCH_ORG_ID);

        UserContext.setUserId(user.id());

        RoleDataScopeResultVO scope = roleDataScopeService.getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertEffectiveRoleIds(user.id(), Set.of(roleId));
        assertFalse(scope.all());
        assertTrue(scope.includeSelf());
        assertEquals(Set.of(selfPartyMemberId), partyMemberIds(scope));
    }

    @Test
    void rolesWithTheSamePermissionContributeTheUnionOfTheirScopes() {
        assertRequiredOrganizationTree();
        TestUser user = createUser();
        Long firstRoleId = addRole(user.id(), RoleDataScopeType.CUSTOM, true, FIRST_BRANCH_ORG_ID);
        Long secondRoleId = addRole(user.id(), RoleDataScopeType.CUSTOM, true, THIRD_BRANCH_ORG_ID);

        UserContext.setUserId(user.id());

        RoleDataScopeResultVO scope = roleDataScopeService.getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertEffectiveRoleIds(user.id(), Set.of(firstRoleId, secondRoleId));
        assertEquals(Set.of(FIRST_BRANCH_ORG_ID, THIRD_BRANCH_ORG_ID), scope.orgIds());
        assertEquals(partyMemberIdsInOrganizations(Set.of(FIRST_BRANCH_ORG_ID, THIRD_BRANCH_ORG_ID)),
                partyMemberIds(scope));
assertFalse(scope.all());
assertFalse(scope.includeSelf());
    }

    @Test
    void roleWithoutTheCurrentPermissionDoesNotContributeItsAllScope() {
        assertRequiredOrganizationTree();
        TestUser user = createUser();
        Long permittedCustomRoleId = addRole(user.id(), RoleDataScopeType.CUSTOM, true, FIRST_BRANCH_ORG_ID);
        addRole(user.id(), RoleDataScopeType.ALL, false);

        UserContext.setUserId(user.id());

        RoleDataScopeResultVO scope = roleDataScopeService.getCurrentUserAccessibleScope(PARTY_MEMBER_LIST);

        assertEffectiveRoleIds(user.id(), Set.of(permittedCustomRoleId));
        assertFalse(scope.all());
        assertEquals(Set.of(FIRST_BRANCH_ORG_ID), scope.orgIds());
        assertEquals(partyMemberIdsInOrganizations(Set.of(FIRST_BRANCH_ORG_ID)), partyMemberIds(scope));
    }

    private TestUser createUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String username = "it_scope_" + token;
        jdbcTemplate.update("INSERT INTO sys_user (username, password, status) VALUES (?, ?, 1)",
                username, "integration-test-password");
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        return new TestUser(userId, token);
    }

    private Long addRole(Long userId, RoleDataScopeType scopeType, boolean grantListPermission, Long... orgIds) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String roleCode = "IT_SCOPE_" + token;
        jdbcTemplate.update("INSERT INTO sys_role (role_code, role_name, status) VALUES (?, ?, 1)",
                roleCode, "数据范围集成测试角色 " + token);
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);

        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        jdbcTemplate.update("INSERT INTO sys_role_data_scope (role_id, scope_type) VALUES (?, ?)",
                roleId, scopeType.name());
        if (grantListPermission) {
            jdbcTemplate.update("""
                    INSERT INTO sys_role_permission (role_id, permission_id)
                    SELECT ?, id FROM sys_permission WHERE permission_code = ?
                    """, roleId, PARTY_MEMBER_LIST);
        }
        for (Long orgId : orgIds) {
            jdbcTemplate.update("INSERT INTO sys_role_scope_org (role_id, org_id) VALUES (?, ?)", roleId, orgId);
        }
        return roleId;
    }

    private Long addPartyMemberForUser(TestUser user, Long orgId) {
        String partyBranch = jdbcTemplate.queryForObject(
                "SELECT org_name FROM sys_org WHERE id = ?", String.class, orgId);
        jdbcTemplate.update("""
                INSERT INTO party_member (name, gender, phone, join_date, party_branch, org_id, user_id)
                VALUES (?, '男', ?, CURRENT_DATE, ?, ?, ?)
                """, "SELF-" + user.token(), "138" + user.token().substring(0, 8), partyBranch, orgId, user.id());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM party_member WHERE user_id = ?", Long.class, user.id());
    }

    private void assertEffectiveRoleIds(Long userId, Set<Long> expectedRoleIds) {
        Set<Long> actualRoleIds = roleDataScopeMapper
                .selectEffectiveScopesByUserIdAndPermissionKey(userId, PARTY_MEMBER_LIST)
                .stream()
                .map(SysRoleDataScope::getRoleId)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedRoleIds, actualRoleIds);
    }

    private Set<Long> partyMemberIds(RoleDataScopeResultVO scope) {
        return partyMemberMapper.selectByDataScope(
                        scope.all(), scope.includeSelf(), scope.userId(), scope.orgIds())
                .stream()
                .map(PartyMember::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<Long> allPartyMemberIds() {
        return new HashSet<>(jdbcTemplate.queryForList("SELECT id FROM party_member", Long.class));
    }

    private Set<Long> partyMemberIdsInOrganizations(Set<Long> orgIds) {
        String placeholders = String.join(",", java.util.Collections.nCopies(orgIds.size(), "?"));
        return new HashSet<>(jdbcTemplate.queryForList(
                "SELECT id FROM party_member WHERE org_id IN (" + placeholders + ")",
                Long.class,
                orgIds.toArray()));
    }

    private void assertRequiredOrganizationTree() {
        List<Long> activeOrgIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_org WHERE id IN (9, 10, 11, 12) AND status = 1", Long.class);
        assertEquals(Set.of(ROOT_ORG_ID, FIRST_BRANCH_ORG_ID, SECOND_BRANCH_ORG_ID, THIRD_BRANCH_ORG_ID),
                new HashSet<>(activeOrgIds), "测试依赖现有启用组织树 9、10、11、12");
        assertEquals(ROOT_ORG_ID, jdbcTemplate.queryForObject(
                "SELECT parent_id FROM sys_org WHERE id = ?", Long.class, FIRST_BRANCH_ORG_ID));
        assertEquals(ROOT_ORG_ID, jdbcTemplate.queryForObject(
                "SELECT parent_id FROM sys_org WHERE id = ?", Long.class, SECOND_BRANCH_ORG_ID));
        assertEquals(ROOT_ORG_ID, jdbcTemplate.queryForObject(
                "SELECT parent_id FROM sys_org WHERE id = ?", Long.class, THIRD_BRANCH_ORG_ID));
    }

    private record TestUser(Long id, String token) {
    }
}
