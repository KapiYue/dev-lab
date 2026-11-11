
USE cxnp;

-- ============================================
-- 任务 5.9.3.5
-- 初始化任职业务模块授权
--
-- 不包含事务语句
-- 不修改现有系统管理员角色权限
-- ============================================

INSERT INTO sys_post_module_access (
    post_id,
    module_id,
    authorized_by
)
SELECT
    up.id,
    bm.id,
    admin.id
FROM (
    SELECT
        'testuser' AS username,
        'PROJECT_A' AS project_code,
        '第一施工组' AS org_name,
        'CONSTRUCTION_WORKER' AS role_code,
        'BASIC' AS post_type,
        'CONSTRUCTION' AS module_code

    UNION ALL SELECT
        'construction_manager',
        'PROJECT_A',
        '施工部',
        'CONSTRUCTION_MANAGER',
        'MANAGEMENT',
        'CONSTRUCTION'

    UNION ALL SELECT
        'construction_manager',
        'PROJECT_B',
        '施工部',
        'CONSTRUCTION_WORKER',
        'BASIC',
        'CONSTRUCTION'

    UNION ALL SELECT
        'cross_department_head',
        'PROJECT_A',
        '施工部',
        'CONSTRUCTION_MANAGER',
        'MANAGEMENT',
        'CONSTRUCTION'

    UNION ALL SELECT
        'cross_department_head',
        'PROJECT_A',
        '监理部',
        'SUPERVISION_MANAGER',
        'MANAGEMENT',
        'SUPERVISION'
) source

INNER JOIN sys_user u
    ON u.username = source.username

INNER JOIN sys_user_post up
    ON up.user_id = u.id
   AND up.post_type = source.post_type
   AND up.status = 1

INNER JOIN sys_project p
    ON up.project_id = p.id
   AND p.project_code = source.project_code
   AND p.status = 1

INNER JOIN sys_org o
    ON up.org_id = o.id
   AND o.org_name = source.org_name
   AND o.status = 1

INNER JOIN sys_role r
    ON up.role_id = r.id
   AND r.role_code = source.role_code
   AND r.status = 1

INNER JOIN sys_business_module bm
    ON bm.module_code = source.module_code
   AND bm.status = 1

INNER JOIN sys_user admin
    ON admin.username = 'admin'
   AND admin.status = 1

WHERE NOT EXISTS (
    SELECT 1
    FROM sys_post_module_access existing
    WHERE existing.post_id = up.id
      AND existing.module_id = bm.id
);
