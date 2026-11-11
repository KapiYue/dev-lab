
USE cxnp;

CREATE TABLE IF NOT EXISTS sys_business_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    module_code VARCHAR(50) NOT NULL,

    module_name VARCHAR(100) NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_module_code (module_code),

    CONSTRAINT chk_business_module_status
        CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS sys_module_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    module_id BIGINT NOT NULL,

    permission_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_module_permission (
        module_id,
        permission_id
    ),

    UNIQUE KEY uk_module_permission_owner (
        permission_id
    ),

    CONSTRAINT fk_module_permission_module
        FOREIGN KEY (module_id)
        REFERENCES sys_business_module(id),

    CONSTRAINT fk_module_permission_permission
        FOREIGN KEY (permission_id)
        REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户管理岗资格
CREATE TABLE IF NOT EXISTS sys_user_management_authorization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    user_id BIGINT NOT NULL,

    authorized_by BIGINT NOT NULL,

    status TINYINT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_management_user (user_id),

    KEY idx_management_authorizer (
        authorized_by
    ),

    CONSTRAINT fk_management_user
        FOREIGN KEY (user_id)
        REFERENCES sys_user(id),

    CONSTRAINT fk_management_authorizer
        FOREIGN KEY (authorized_by)
        REFERENCES sys_user(id),

    CONSTRAINT chk_management_auth_status
        CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任职业务模块授权表

CREATE TABLE IF NOT EXISTS sys_post_module_access (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    post_id BIGINT NOT NULL,

    module_id BIGINT NOT NULL,

    authorized_by BIGINT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_post_module (
        post_id,
        module_id
    ),

    KEY idx_post_module_module (module_id),

    KEY idx_post_module_authorizer (
        authorized_by
    ),

    CONSTRAINT fk_post_module_post
        FOREIGN KEY (post_id)
        REFERENCES sys_user_post(id),

    CONSTRAINT fk_post_module_module
        FOREIGN KEY (module_id)
        REFERENCES sys_business_module(id),

    CONSTRAINT fk_post_module_authorizer
        FOREIGN KEY (authorized_by)
        REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

