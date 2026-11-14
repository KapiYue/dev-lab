-- 认证会话安全：管理员强制撤销用户会话权限初始化。
-- 执行前请先选择目标数据库，并确保 sys_role、sys_permission、
-- sys_role_permission 已由基础 RBAC 建表脚本创建。

INSERT INTO sys_permission (
    permission_code,
    permission_name
)
VALUES (
    'rbac:user:session:revoke',
    '撤销用户全部会话'
)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name);

INSERT INTO sys_role_permission (
    role_id,
    permission_id
)
SELECT
    role.id,
    permission.id
FROM sys_role role
INNER JOIN sys_permission permission
    ON permission.permission_code = 'rbac:user:session:revoke'
WHERE role.role_code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );
