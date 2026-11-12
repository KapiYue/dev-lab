-- 角色数据范围初始化及党员历史组织映射。
-- 前置条件：已执行 sql/20_role_data_scope_schema.sql 与 database/rbac_init.sql。

USE cxnp;

-- 1. 初始化根组织与支部；使用名称和父节点共同确定唯一组织。
INSERT INTO sys_org (org_name, parent_id, org_type, status)
SELECT '党委组织', NULL, 'ORG', 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_org
    WHERE org_name = '党委组织'
      AND parent_id IS NULL
      AND org_type = 'ORG'
);

INSERT INTO sys_org (org_name, parent_id, org_type, status)
SELECT branch.branch_name, parent.id, 'BRANCH', 1
FROM (
    SELECT '第一党支部' AS branch_name
    UNION ALL SELECT '第二党支部'
    UNION ALL SELECT '第三党支部'
) branch
INNER JOIN sys_org parent
    ON parent.org_name = '党委组织'
   AND parent.parent_id IS NULL
   AND parent.org_type = 'ORG'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_org existing
    WHERE existing.org_name = branch.branch_name
      AND existing.parent_id = parent.id
      AND existing.org_type = 'BRANCH'
);

-- 2. 保留现有 ADMIN/USER 演示授权；重复执行不会产生重复范围或组织绑定。
INSERT INTO sys_role_data_scope (role_id, scope_type)
SELECT r.id, 'ALL'
FROM sys_role r
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_data_scope ds WHERE ds.role_id = r.id
  );

INSERT INTO sys_role_data_scope (role_id, scope_type)
SELECT r.id, 'CUSTOM'
FROM sys_role r
WHERE r.role_code = 'USER'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_data_scope ds WHERE ds.role_id = r.id
  );

INSERT INTO sys_role_scope_org (role_id, org_id)
SELECT r.id, branch.id
FROM sys_role r
INNER JOIN sys_role_data_scope ds
    ON ds.role_id = r.id
INNER JOIN sys_org branch
    ON branch.org_name = '第一党支部'
   AND branch.org_type = 'BRANCH'
INNER JOIN sys_org parent
    ON parent.id = branch.parent_id
   AND parent.org_name = '党委组织'
   AND parent.parent_id IS NULL
WHERE r.role_code = 'USER'
  AND ds.scope_type = 'CUSTOM'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_scope_org existing
      WHERE existing.role_id = r.id
        AND existing.org_id = branch.id
  );

-- 3. 仅按已知、唯一的“党委组织/支部”初始化树映射历史文本字段。
-- 其他组织命名或映射歧义必须人工处理，不能自动猜测。
UPDATE party_member pm
INNER JOIN sys_org branch
    ON branch.org_name = pm.party_branch
   AND branch.org_type = 'BRANCH'
   AND branch.status = 1
INNER JOIN sys_org parent
    ON parent.id = branch.parent_id
   AND parent.org_name = '党委组织'
   AND parent.parent_id IS NULL
   AND parent.org_type = 'ORG'
SET pm.org_id = branch.id
WHERE pm.org_id IS NULL;

-- 执行 sql/22 前，此查询必须返回零行。
SELECT id, name, party_branch
FROM party_member
WHERE org_id IS NULL;
