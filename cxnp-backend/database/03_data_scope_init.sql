
USE cxnp;

-- ========================================
-- 1. 初始化组织数据
-- ========================================

INSERT INTO sys_org (
    org_name,
    parent_id,
    org_type,
    status
)
SELECT
    '党委组织',
    NULL,
    'ORG',
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_org
    WHERE org_name = '党委组织'
      AND parent_id IS NULL
      AND org_type = 'ORG'
);

-- 初始化三个党支部
INSERT INTO sys_org (
    org_name,
    parent_id,
    org_type,
    status
)
SELECT
    branch.branch_name,
    parent.id,
    'BRANCH',
    1
FROM (
    SELECT '第一党支部' AS branch_name
    UNION ALL
    SELECT '第二党支部'
    UNION ALL
    SELECT '第三党支部'
) branch
JOIN sys_org parent
    ON parent.org_name = '党委组织'
   AND parent.parent_id IS NULL
   AND parent.org_type = 'ORG'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_org existing
    WHERE existing.org_name = branch.branch_name
      AND existing.parent_id = parent.id
      AND existing.org_type = 'BRANCH'
);

-- ========================================
-- 2. 初始化角色数据范围
-- ========================================

-- ADMIN：全部数据
INSERT INTO sys_role_data_scope (
    role_id,
    scope_type
)
SELECT
    r.id,
    'ALL'
FROM sys_role r
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_data_scope ds
      WHERE ds.role_id = r.id
  );

-- USER：指定组织
INSERT INTO sys_role_data_scope (
    role_id,
    scope_type
)
SELECT
    r.id,
    'CUSTOM'
FROM sys_role r
WHERE r.role_code = 'USER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_data_scope ds
      WHERE ds.role_id = r.id
  );

-- ========================================
-- 3. 绑定 USER 的指定组织范围
-- ========================================

INSERT INTO sys_role_scope_org (
    role_id,
    org_id
)
SELECT
    r.id,
    o.id
FROM sys_role r
JOIN sys_role_data_scope ds
    ON ds.role_id = r.id
JOIN sys_org o
    ON o.org_name = '第一党支部'
JOIN sys_org parent
    ON parent.id = o.parent_id
   AND parent.org_name = '党委组织'
   AND parent.parent_id IS NULL
WHERE r.role_code = 'USER'
  AND ds.scope_type = 'CUSTOM'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_scope_org existing
      WHERE existing.role_id = r.id
        AND existing.org_id = o.id
  );
