CREATE TABLE `party_member` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '党员ID',
                                `name` varchar(50) NOT NULL COMMENT '党员姓名',
                                `gender` varchar(10) DEFAULT NULL COMMENT '性别',
                                `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
                                `join_date` date DEFAULT NULL COMMENT '入党日期',
                                `party_branch` varchar(100) DEFAULT NULL COMMENT '所属党支部',
                                `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='党员信息表'


CREATE TABLE IF NOT EXISTS sys_user (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    );


-- 1. 角色表
CREATE TABLE IF NOT EXISTS sys_role (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        role_code VARCHAR(100) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 功能权限表
CREATE TABLE IF NOT EXISTS sys_permission (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_code (permission_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 用户角色关联表
/**
  UNIQUE KEY uk_user_role(user_id, role_id)联合检索，两字段组合不能重复
  MySQL 创建联合唯一索引的语法
 */
CREATE TABLE IF NOT EXISTS sys_user_role (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             user_id BIGINT NOT NULL,
                                             role_id BIGINT NOT NULL,
    -- 1 防止重复绑定
    UNIQUE KEY uk_user_role (user_id, role_id),
    -- 2 优化按角色查询用户的速度，SQL 内部扫描而不是数据库发送多次请求，验证办法
    -- EXPLAIN
    -- SELECT user_id
    -- FROM sys_user_role
    -- WHERE role_id = 1;
    -- 重点查看返回的，type查询访问方式，通常为 ref；key使用的索引，rows 预计需要检查的记录数，比全表少

    KEY idx_role_id (role_id),
    -- 3 保证用户真实存在
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    -- 4 保证角色真实存在
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
                                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                   role_id BIGINT NOT NULL,
                                                   permission_id BIGINT NOT NULL,
    --  防止重复绑定
    UNIQUE KEY uk_role_permission (role_id, permission_id),
--     优化权限查找速度
    KEY idx_permission_id (permission_id),
    CONSTRAINT fk_role_permission_role
    FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_role_permission_permission
    FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 创建组织表 sys_org
USE cxnp;

CREATE TABLE IF NOT EXISTS sys_org (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    org_name VARCHAR(100) NOT NULL,

    parent_id BIGINT DEFAULT NULL,

    org_type VARCHAR(30) NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_parent_id (parent_id),

    KEY idx_org_type (org_type),

    KEY idx_org_status (status),

    CONSTRAINT fk_org_parent
        FOREIGN KEY (parent_id)
        REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建角色数据范围表 sys_role_data_scope
CREATE TABLE IF NOT EXISTS sys_role_data_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    role_id BIGINT NOT NULL,

    scope_type VARCHAR(30) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_role_id (role_id),

    CONSTRAINT fk_data_scope_role
        FOREIGN KEY (role_id)
        REFERENCES sys_role(id),

    CONSTRAINT chk_scope_type
        CHECK (scope_type IN (
            'ALL',
            'ORG_AND_CHILD',
            'CUSTOM',
            'SELF'
        ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建指定组织关联表 sys_role_scope_org
CREATE TABLE IF NOT EXISTS sys_role_scope_org (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    role_id BIGINT NOT NULL,

    org_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_role_org (role_id, org_id),

    KEY idx_org_id (org_id),

    CONSTRAINT fk_scope_org_role
        FOREIGN KEY (role_id)
        REFERENCES sys_role(id),

    CONSTRAINT fk_scope_org_org
        FOREIGN KEY (org_id)
        REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



