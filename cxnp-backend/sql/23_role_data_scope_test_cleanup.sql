-- 仅清理阶段 8 使用固定前缀创建的验收数据；不清理现有业务数据。

USE cxnp;

START TRANSACTION;

DELETE FROM party_member
WHERE name LIKE '验收-数据范围-%'
   OR name LIKE '验收-党员越权-%';

DELETE rso
FROM sys_role_scope_org rso
INNER JOIN sys_role r ON r.id = rso.role_id
WHERE r.role_code LIKE 'ROLE_SCOPE_TEST_%';

DELETE ds
FROM sys_role_data_scope ds
INNER JOIN sys_role r ON r.id = ds.role_id
WHERE r.role_code LIKE 'ROLE_SCOPE_TEST_%';

DELETE rp
FROM sys_role_permission rp
INNER JOIN sys_role r ON r.id = rp.role_id
WHERE r.role_code LIKE 'ROLE_SCOPE_TEST_%';

DELETE ur
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id
WHERE r.role_code LIKE 'ROLE_SCOPE_TEST_%';

DELETE FROM sys_role
WHERE role_code LIKE 'ROLE_SCOPE_TEST_%';

DELETE FROM sys_user
WHERE username LIKE 'scope_test_%'
   OR username LIKE 'http_party_%';

COMMIT;
