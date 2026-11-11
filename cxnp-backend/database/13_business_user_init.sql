
USE cxnp;

-- ============================================
-- 任务 5.9.3.3
-- 初始化业务验证用户
--
-- 1. 不修改 admin 和 testuser
-- 2. 不存储明文密码
-- 3. 新用户默认禁用
-- 4. 重复执行时跳过已有用户
-- ============================================

-- 施工经理验证账号
INSERT INTO sys_user (
    username,
    password,
    status
)
SELECT
    'construction_manager',
    '$2y$12$R0CJZz9LsNhfVgRksqqByeXC6k9kD6zHSUK.vWxJt3ZZ3bXebjFFa',
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'construction_manager'
);

-- 跨部门负责人验证账号
INSERT INTO sys_user (
    username,
    password,
    status
)
SELECT
    'cross_department_head',
    '$2y$12$xaTLaGJTSTs4f.bCI865zexOzQSjUFmWfygVxvd/LSiW.GntLCGdW',
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'cross_department_head'
);
