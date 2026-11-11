
USE cxnp;

-- ============================================
-- 任务 5.9.3.7.4.16.4
-- 创建临时 Token 状态验证账号
-- ============================================

-- 1. 检查临时账号是否存在
-- 预期：0 rows

SELECT
    id,
    username,
    status
FROM sys_user
WHERE username = 'token_status_check';


-- 2. 创建临时账号
-- 将下方占位符替换为自己刚生成的 BCrypt 哈希
-- 必须保留单引号

INSERT INTO sys_user (
    username,
    password,
    status
)
SELECT
    'token_status_check',
    '在这里填写完整BCrypt哈希',
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user
    WHERE username = 'token_status_check'
);
