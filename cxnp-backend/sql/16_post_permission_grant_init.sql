
USE cxnp;

-- ============================================
-- 任务 5.9.3.6
-- 初始化任职功能权限
-- 不包含事务控制语句
-- ============================================

INSERT INTO sys_post_permission_grant (
    post_id,
    permission_id
)
SELECT
    up.id,
    perm.id
FROM sys_user_post up

INNER JOIN sys_user u
    ON up.user_id = u.id

INNER JOIN sys_role r
    ON up.role_id = r.id

INNER JOIN sys_project proj
    ON up.project_id = proj.id

INNER JOIN sys_post_module_access pma
    ON pma.post_id = up.id

INNER JOIN sys_business_module bm
    ON pma.module_id = bm.id

INNER JOIN sys_module_permission mp
    ON mp.module_id = bm.id

INNER JOIN sys_permission perm
    ON mp.permission_id = perm.id

WHERE up.status = 1
  AND proj.status = 1
  AND r.status = 1
  AND bm.status = 1

  AND (
      -- testuser：项目 A 施工员
      (
          u.username = 'testuser'
          AND proj.project_code = 'PROJECT_A'
          AND r.role_code = 'CONSTRUCTION_WORKER'
          AND up.post_type = 'BASIC'
          AND bm.module_code = 'CONSTRUCTION'
          AND perm.permission_code IN (
              'construction:progress:view',
              'construction:progress:report'
          )
      )

      OR

      -- construction_manager：项目 A 施工经理
      (
          u.username = 'construction_manager'
          AND proj.project_code = 'PROJECT_A'
          AND r.role_code = 'CONSTRUCTION_MANAGER'
          AND up.post_type = 'MANAGEMENT'
          AND bm.module_code = 'CONSTRUCTION'
          AND perm.permission_code IN (
              'construction:progress:view',
              'construction:progress:report',
              'construction:progress:approve',
              'construction:permission:configure'
          )
      )

      OR

      -- construction_manager：项目 B 施工员
      (
          u.username = 'construction_manager'
          AND proj.project_code = 'PROJECT_B'
          AND r.role_code = 'CONSTRUCTION_WORKER'
          AND up.post_type = 'BASIC'
          AND bm.module_code = 'CONSTRUCTION'
          AND perm.permission_code IN (
              'construction:progress:view',
              'construction:progress:report'
          )
      )

      OR

      -- cross_department_head：项目 A 施工经理
      (
          u.username = 'cross_department_head'
          AND proj.project_code = 'PROJECT_A'
          AND r.role_code = 'CONSTRUCTION_MANAGER'
          AND up.post_type = 'MANAGEMENT'
          AND bm.module_code = 'CONSTRUCTION'
          AND perm.permission_code IN (
              'construction:progress:view',
              'construction:progress:report',
              'construction:progress:approve',
              'construction:permission:configure'
          )
      )

      OR

      -- cross_department_head：项目 A 监理负责人
      (
          u.username = 'cross_department_head'
          AND proj.project_code = 'PROJECT_A'
          AND r.role_code = 'SUPERVISION_MANAGER'
          AND up.post_type = 'MANAGEMENT'
          AND bm.module_code = 'SUPERVISION'
          AND perm.permission_code IN (
              'supervision:record:view',
              'supervision:quality:inspect',
              'supervision:record:approve',
              'supervision:permission:configure'
          )
      )
  )

  -- 防止重复授权
  AND NOT EXISTS (
      SELECT 1
      FROM sys_post_permission_grant existing
      WHERE existing.post_id = up.id
        AND existing.permission_id = perm.id
  );
