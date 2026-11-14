
USE cxnp;

START TRANSACTION;

-- ========================================
-- 1. 清理测试角色的组织绑定
-- ========================================

DELETE rso
FROM sys_role_scope_org rso
JOIN sys_role r
    ON rso.role_id = r.id
JOIN sys_org o
    ON rso.org_id = o.id
JOIN sys_org parent
    ON parent.id = o.parent_id
WHERE r.role_code = 'USER'
  AND parent.org_name = '党委组织'
  AND parent.parent_id IS NULL
  AND o.org_name = '第一党支部';

-- ========================================
-- 2. 清理角色数据范围
-- ========================================

DELETE ds
FROM sys_role_data_scope ds
JOIN sys_role r
    ON ds.role_id = r.id
WHERE (
    r.role_code = 'ADMIN'
    AND ds.scope_type = 'ALL'
) OR (
    r.role_code = 'USER'
    AND ds.scope_type = 'CUSTOM'
);

-- ========================================
-- 3. 清理测试组织
-- ========================================

-- 先删除党支部
DELETE child
FROM sys_org child
JOIN sys_org parent
    ON child.parent_id = parent.id
WHERE parent.org_name = '党委组织'
  AND parent.parent_id IS NULL
  AND child.org_name IN (
      '第一党支部',
      '第二党支部',
      '第三党支部'
  );

-- 再删除测试党委
DELETE FROM sys_org
WHERE org_name = '党委组织'
  AND parent_id IS NULL
  AND org_type = 'ORG';

COMMIT;
