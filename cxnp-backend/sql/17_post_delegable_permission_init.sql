
USE cxnp;

-- ============================================
-- 任务 5.9.3.7.2
-- 初始化管理岗可委派功能权限
-- 预期新增：6 条
-- ============================================

INSERT INTO sys_post_delegable_permission (
    post_id,
    permission_id
)
SELECT
    up.id,
    perm.id
FROM (
    SELECT
        'construction_manager' AS username,
        'PROJECT_A' AS project_code,
        'CONSTRUCTION_MANAGER' AS role_code,
        'MANAGEMENT' AS post_type,
        'construction:progress:view' AS permission_code

    UNION ALL SELECT
        'construction_manager',
        'PROJECT_A',
        'CONSTRUCTION_MANAGER',
        'MANAGEMENT',
        'construction:progress:report'

    UNION ALL SELECT
        'cross_department_head',
        'PROJECT_A',
        'CONSTRUCTION_MANAGER',
        'MANAGEMENT',
        'construction:progress:view'

    UNION ALL SELECT
        'cross_department_head',
        'PROJECT_A',
        'CONSTRUCTION_MANAGER',
        'MANAGEMENT',
        'construction:progress:report'

    UNION ALL SELECT
        'cross_department_head',
        'PROJECT_A',
        'SUPERVISION_MANAGER',
        'MANAGEMENT',
        'supervision:record:view'

    UNION ALL SELECT
        'cross_department_head',
        'PROJECT_A',
        'SUPERVISION_MANAGER',
        'MANAGEMENT',
        'supervision:quality:inspect'
) source

INNER JOIN sys_user u
    ON u.username = source.username

INNER JOIN sys_project proj
    ON proj.project_code = source.project_code

INNER JOIN sys_role r
    ON r.role_code = source.role_code

INNER JOIN sys_user_post up
    ON up.user_id = u.id
   AND up.project_id = proj.id
   AND up.role_id = r.id
   AND up.post_type = source.post_type
   AND up.status = 1

INNER JOIN sys_permission perm
    ON perm.permission_code = source.permission_code

-- 必须已经拥有该功能权限
INNER JOIN sys_post_permission_grant pg
    ON pg.post_id = up.id
   AND pg.permission_id = perm.id

-- 必须已有对应模块授权
INNER JOIN sys_module_permission mp
    ON mp.permission_id = perm.id

INNER JOIN sys_post_module_access pma
    ON pma.post_id = up.id
   AND pma.module_id = mp.module_id

WHERE NOT EXISTS (
    SELECT 1
    FROM sys_post_delegable_permission existing
    WHERE existing.post_id = up.id
      AND existing.permission_id = perm.id
);


-- ============================================
-- 验证 1：可委派权限总数
-- 预期：6
-- ============================================

SELECT
    COUNT(*) AS total_delegable_permissions
FROM sys_post_delegable_permission;


-- ============================================
-- 验证 2：查看全部可委派权限
-- 预期：6 rows
-- ============================================

SELECT
    up.id AS post_id,
    u.username,
    proj.project_code,
    r.role_code,
    perm.permission_code

FROM sys_post_delegable_permission dp

JOIN sys_user_post up
    ON dp.post_id = up.id

JOIN sys_user u
    ON up.user_id = u.id

JOIN sys_project proj
    ON up.project_id = proj.id

JOIN sys_role r
    ON up.role_id = r.id

JOIN sys_permission perm
    ON dp.permission_id = perm.id

ORDER BY
    u.username,
    proj.project_code,
    r.role_code,
    perm.permission_code;


-- ============================================
-- 验证 3：检查普通施工员是否被授予委派资格
-- 预期：0 rows
-- ============================================

SELECT
    u.username,
    proj.project_code,
    r.role_code,
    perm.permission_code

FROM sys_post_delegable_permission dp

JOIN sys_user_post up
    ON dp.post_id = up.id

JOIN sys_user u
    ON up.user_id = u.id

JOIN sys_project proj
    ON up.project_id = proj.id

JOIN sys_role r
    ON up.role_id = r.id

JOIN sys_permission perm
    ON dp.permission_id = perm.id

WHERE up.post_type = 'BASIC';


-- ============================================
-- 验证 4：检查是否错误允许委派审批或授权配置权限
-- 预期：0 rows
-- ============================================

SELECT
    dp.post_id,
    perm.permission_code
FROM sys_post_delegable_permission dp
JOIN sys_permission perm
    ON dp.permission_id = perm.id
WHERE perm.permission_code LIKE '%:approve'
   OR perm.permission_code LIKE '%:permission:configure';


-- ============================================
-- 验证 5：检查可委派权限是否属于已有功能权限
-- 预期：0 rows
-- ============================================

SELECT
    dp.post_id,
    dp.permission_id
FROM sys_post_delegable_permission dp
LEFT JOIN sys_post_permission_grant pg
    ON dp.post_id = pg.post_id
   AND dp.permission_id = pg.permission_id
WHERE pg.id IS NULL;
