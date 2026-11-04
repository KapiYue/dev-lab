
USE cxnp;

-- 仅清理由本次 RBAC 初始化脚本创建的测试数据
-- 请只在测试环境执行

-- 1. 清理这些角色的用户绑定
DELETE ur
FROM sys_user_role ur
JOIN sys_role r ON ur.role_id = r.id
WHERE r.role_code IN ('ADMIN', 'USER');

-- 2. 清理这些角色的权限绑定
DELETE rp
FROM sys_role_permission rp
JOIN sys_role r ON rp.role_id = r.id
WHERE r.role_code IN ('ADMIN', 'USER');

-- 3. 清理初始化角色
DELETE FROM sys_role
WHERE role_code IN ('ADMIN', 'USER');

-- 4. 清理初始化权限
DELETE FROM sys_permission
WHERE permission_code IN (
                          'party:member:list',
                          'party:member:detail'
    );
