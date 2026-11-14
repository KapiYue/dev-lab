-- 党员模块初始化数据
-- 执行前请先选择目标数据库，并通过 schema.sql 创建表

INSERT INTO party_member
(id, name, gender, phone, join_date, party_branch, created_at)
VALUES
    (1, '张三', '男', '13800000001', '2020-06-15', '第一党支部', '2026-10-30 09:50:14'),
    (2, '李四', '女', '13800000002', '2019-07-01', '第二党支部', '2026-10-30 09:50:14'),
    (3, '王五', '男', '13800000003', '2021-05-20', '第一党支部', '2026-10-30 09:50:14');


INSERT INTO cxnp.sys_user (
    username,
    password,
    status
)
VALUES (
           'admin',
           '这里替换为PasswordHashTest生成的完整BCrypt哈希',
           1
       );