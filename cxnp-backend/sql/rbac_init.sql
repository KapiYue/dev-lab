
USE cxnp;

-- 1. 初始化角色
INSERT INTO sys_role (role_code, role_name, status)
VALUES
    ('ADMIN', '管理员', 1),
    ('USER', '普通用户', 1)
    ON DUPLICATE KEY UPDATE
                         role_name = VALUES(role_name),
                         status = VALUES(status);

-- 2. 初始化功能权限
INSERT INTO sys_permission (permission_code, permission_name)
VALUES
    ('party:member:list', '党员列表查询'),
    ('party:member:detail', '党员详情查询')
    ON DUPLICATE KEY UPDATE
                         permission_name = VALUES(permission_name);

-- 3. 管理员绑定两个权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code IN (
                            'party:member:list',
                            'party:member:detail'
    )
    ON DUPLICATE KEY UPDATE
                         role_id = VALUES(role_id);

-- 4. 普通用户只绑定列表权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.role_code = 'USER'
  AND p.permission_code = 'party:member:list'
    ON DUPLICATE KEY UPDATE
                         role_id = VALUES(role_id);

-- SELECT
--     r.id AS role_id,
--     r.role_code,
--     p.id AS permission_id,
--     p.permission_code
-- rp 是这张sys_role_permission表的别名
-- FROM sys_role_permission rp
-- r 是 sys_role别名，ON指定两张表如何匹配,【关联角色表】
-- JOIN sys_role r ON rp.role_id = r.id
-- 第二次【关联权限表】
-- JOIN sys_permission p ON rp.permission_id = p.id;
--