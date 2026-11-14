
USE cxnp;

-- 当前任职实际拥有的功能权限
CREATE TABLE IF NOT EXISTS sys_post_permission_grant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    post_id BIGINT NOT NULL,

    permission_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    -- 同一个任职不能重复授予同一权限
    UNIQUE KEY uk_post_permission (
        post_id,
        permission_id
    ),

    KEY idx_post_grant_permission (
        permission_id
    ),

    CONSTRAINT fk_post_grant_post
        FOREIGN KEY (post_id)
        REFERENCES sys_user_post(id),

    CONSTRAINT fk_post_grant_permission
        FOREIGN KEY (permission_id)
        REFERENCES sys_permission(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;



-- 当前任职允许向下级委派的功能权限
CREATE TABLE IF NOT EXISTS sys_post_delegable_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    post_id BIGINT NOT NULL,

    permission_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    -- 同一任职不能重复配置委派权限
    UNIQUE KEY uk_post_delegable_permission (
        post_id,
        permission_id
    ),

    KEY idx_delegable_permission (
        permission_id
    ),

    -- 只能委派当前任职已经拥有的功能权限
    CONSTRAINT fk_delegable_owned_permission
        FOREIGN KEY (post_id, permission_id)
        REFERENCES sys_post_permission_grant(
            post_id,
            permission_id
        )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;

