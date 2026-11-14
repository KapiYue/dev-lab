
USE cxnp;

START TRANSACTION;

-- 1. 初始化项目
INSERT INTO sys_project (
    project_code,
    project_name,
    status
)
VALUES
    ('PROJECT_A', '项目 A', 1),
    ('PROJECT_B', '项目 B', 1)
ON DUPLICATE KEY UPDATE
    project_code = project_code;

-- 2. 初始化施工部
INSERT INTO sys_org (
    org_name, parent_id, org_type, status
)
SELECT '施工部', NULL, 'DEPARTMENT', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_org
    WHERE org_name = '施工部'
      AND parent_id IS NULL
      AND org_type = 'DEPARTMENT'
);

-- 3. 初始化监理部
INSERT INTO sys_org (
    org_name, parent_id, org_type, status
)
SELECT '监理部', NULL, 'DEPARTMENT', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_org
    WHERE org_name = '监理部'
      AND parent_id IS NULL
      AND org_type = 'DEPARTMENT'
);

-- 4. 初始化第一施工组
INSERT INTO sys_org (
    org_name, parent_id, org_type, status
)
SELECT '第一施工组', parent.id, 'TEAM', 1
FROM sys_org parent
WHERE parent.org_name = '施工部'
  AND parent.parent_id IS NULL
  AND parent.org_type = 'DEPARTMENT'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_org child
      WHERE child.parent_id = parent.id
        AND child.org_name = '第一施工组'
  );

-- 5. 初始化第一监理组
INSERT INTO sys_org (
    org_name, parent_id, org_type, status
)
SELECT '第一监理组', parent.id, 'TEAM', 1
FROM sys_org parent
WHERE parent.org_name = '监理部'
  AND parent.parent_id IS NULL
  AND parent.org_type = 'DEPARTMENT'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_org child
      WHERE child.parent_id = parent.id
        AND child.org_name = '第一监理组'
  );

-- 6. 关联项目 A 的组织
INSERT INTO sys_project_org (
    project_id, org_id
)
SELECT p.id, o.id
FROM sys_project p
JOIN sys_org o
    ON o.org_name IN (
        '施工部',
        '第一施工组',
        '监理部',
        '第一监理组'
    )
WHERE p.project_code = 'PROJECT_A'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_project_org po
      WHERE po.project_id = p.id
        AND po.org_id = o.id
  );

-- 7. 关联项目 B 的组织
INSERT INTO sys_project_org (
    project_id, org_id
)
SELECT p.id, o.id
FROM sys_project p
JOIN sys_org o
    ON o.org_name IN (
        '施工部',
        '第一施工组'
    )
WHERE p.project_code = 'PROJECT_B'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_project_org po
      WHERE po.project_id = p.id
        AND po.org_id = o.id
  );

COMMIT;
