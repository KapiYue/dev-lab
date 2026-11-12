-- 角色数据范围与党员归属迁移。
-- 前置条件：已执行 database/schema.sql，且 sys_user、sys_role、sys_org 已存在。
-- 本文件是一次性迁移脚本；重复执行前请先确认目标库字段不存在。

USE cxnp;

CREATE TABLE IF NOT EXISTS sys_role_data_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_id (role_id),
    CONSTRAINT fk_data_scope_role
        FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT chk_scope_type
        CHECK (scope_type IN ('ALL', 'ORG_AND_CHILD', 'CUSTOM', 'SELF'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_scope_org (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_org (role_id, org_id),
    KEY idx_org_id (org_id),
    CONSTRAINT fk_scope_org_role
        FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_scope_org_org
        FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 先以可空字段迁移历史数据；完成 sql/21 的映射校验后，再执行 sql/22 收紧 org_id。
ALTER TABLE party_member
    ADD COLUMN org_id BIGINT NULL COMMENT '所属组织 ID' AFTER party_branch,
    ADD COLUMN user_id BIGINT NULL COMMENT '关联系统用户 ID' AFTER org_id,
    ADD KEY idx_party_member_org_id (org_id),
    ADD UNIQUE KEY uk_party_member_user_id (user_id),
    ADD CONSTRAINT fk_party_member_org
        FOREIGN KEY (org_id) REFERENCES sys_org(id),
    ADD CONSTRAINT fk_party_member_user
        FOREIGN KEY (user_id) REFERENCES sys_user(id);

-- 表级约束无法表达以下跨表规则，由角色范围配置 Service 在同一事务内校验：
-- ALL、SELF 不允许组织绑定；CUSTOM、ORG_AND_CHILD 必须至少绑定一个启用组织。
