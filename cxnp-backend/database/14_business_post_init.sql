
USE cxnp;

-- ============================================
-- 1. 管理岗资格
-- 仅用于本地开发验证
-- authorized_by 使用已有系统管理员
-- ============================================

INSERT INTO sys_user_management_authorization (
    user_id,
    authorized_by,
    status
)
SELECT
    u.id,
    admin.id,
    1
FROM sys_user u
JOIN sys_user admin
    ON admin.username = 'admin'
   AND admin.status = 1
WHERE u.username IN (
    'construction_manager',
    'cross_department_head'
)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_management_authorization existing
      WHERE existing.user_id = u.id
  );



-- ============================================
-- 2. 初始化用户业务任职
-- ============================================

INSERT INTO sys_user_post (
    user_id,
    role_id,
    org_id,
    project_id,
    post_type,
    status
)
SELECT
    u.id,
    r.id,
    o.id,
    p.id,
    source.post_type,
    1
FROM (
    SELECT
        'testuser' AS username,
        'CONSTRUCTION_WORKER' AS role_code,
        '第一施工组' AS org_name,
        'PROJECT_A' AS project_code,
        'BASIC' AS post_type

    UNION ALL SELECT
        'construction_manager',
        'CONSTRUCTION_MANAGER',
        '施工部',
        'PROJECT_A',
        'MANAGEMENT'

    UNION ALL SELECT
        'construction_manager',
        'CONSTRUCTION_WORKER',
        '施工部',
        'PROJECT_B',
        'BASIC'

    UNION ALL SELECT
        'cross_department_head',
        'CONSTRUCTION_MANAGER',
        '施工部',
        'PROJECT_A',
        'MANAGEMENT'

    UNION ALL SELECT
        'cross_department_head',
        'SUPERVISION_MANAGER',
        '监理部',
        'PROJECT_A',
        'MANAGEMENT'
) source

JOIN sys_user u
    ON u.username = source.username

JOIN sys_role r
    ON r.role_code = source.role_code

JOIN sys_org o
    ON o.org_name = source.org_name

JOIN sys_project p
    ON p.project_code = source.project_code

JOIN sys_project_org po
    ON po.project_id = p.id
   AND po.org_id = o.id

WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_post existing
    WHERE existing.user_id = u.id
      AND existing.role_id = r.id
      AND existing.org_id = o.id
      AND existing.project_id = p.id
      AND existing.post_type = source.post_type
);
