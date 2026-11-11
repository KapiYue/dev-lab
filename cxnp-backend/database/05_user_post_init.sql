
USE cxnp;

-- 项目基础信息
CREATE TABLE IF NOT EXISTS sys_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_code VARCHAR(50) NOT NULL,

    project_name VARCHAR(100) NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_project_code (project_code),

    KEY idx_project_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 用户在不同组织、项目中的任职信息
CREATE TABLE IF NOT EXISTS sys_user_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    user_id BIGINT NOT NULL,

    role_id BIGINT NOT NULL,

    org_id BIGINT NOT NULL,

    project_id BIGINT DEFAULT NULL,

    post_type VARCHAR(20) NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    -- 一个用户只能有一个启用的基础业务岗位
    active_basic_user_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN post_type = 'BASIC' AND status = 1
                THEN user_id
                ELSE NULL
            END
        ) STORED,

    UNIQUE KEY uk_active_basic_user (
        active_basic_user_id
    ),

    KEY idx_user_post_user (user_id),

    KEY idx_user_post_role (role_id),

    KEY idx_user_post_org (org_id),

    KEY idx_user_post_project (project_id),

    CONSTRAINT fk_post_user
        FOREIGN KEY (user_id)
        REFERENCES sys_user(id),

    CONSTRAINT fk_post_role
        FOREIGN KEY (role_id)
        REFERENCES sys_role(id),

    CONSTRAINT fk_post_org
        FOREIGN KEY (org_id)
        REFERENCES sys_org(id),

    CONSTRAINT fk_post_project
        FOREIGN KEY (project_id)
        REFERENCES sys_project(id),

    CONSTRAINT chk_post_type
        CHECK (post_type IN ('BASIC', 'MANAGEMENT')),

    CONSTRAINT chk_post_status
        CHECK (status IN (0, 1))

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
