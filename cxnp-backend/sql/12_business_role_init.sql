
USE cxnp;

-- ==========================================
-- 初始化施工、监理业务角色
-- 不修改原有 ADMIN、USER
-- ==========================================

INSERT INTO sys_role (
    role_code,
    role_name,
    status
)

-- FROM后面SELECT 查询产生的临时结果集，称为派生表或者子表查询
-- UNION ALL：将多个查询结果按行合并，保留重复行。
-- 第一，执行后产生的结果，临时表名roles：

-- role_code	        role_name
-- CONSTRUCTION_WORKER	施工员
-- CONSTRUCTION_MANAGER	施工经理
-- SUPERVISION_WORKER	监理员
-- SUPERVISION_MANAGER	监理负责人

-- 第二，外层SELECT读取内层的结果：
-- SELECT roles.role_code, roles.role_name, 1


SELECT
    roles.role_code,
    roles.role_name,
    1
FROM (
    SELECT
        'CONSTRUCTION_WORKER' AS role_code,
        '施工员' AS role_name

    UNION ALL SELECT
        'CONSTRUCTION_MANAGER',
        '施工经理'

    UNION ALL SELECT
        'SUPERVISION_WORKER',
        '监理员'

    UNION ALL SELECT
        'SUPERVISION_MANAGER',
        '监理负责人'
) roles
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role existing
    WHERE existing.role_code = roles.role_code
);
