
USE cxnp;

-- 任职数据范围表
CREATE TABLE IF NOT EXISTS sys_post_data_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    post_id BIGINT NOT NULL,

    scope_type VARCHAR(30) NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    -- 一条任职记录只能配置一种数据范围
    UNIQUE KEY uk_post_scope (post_id),

    CONSTRAINT fk_post_scope_post
        FOREIGN KEY (post_id)
        REFERENCES sys_user_post(id),

    CONSTRAINT chk_post_scope_type
        CHECK (
            scope_type IN (
                'ALL',
                'ORG_AND_CHILD',
                'CUSTOM',
                'SELF'
            )
        )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;


-- 任职指定组织关联表
CREATE TABLE IF NOT EXISTS sys_post_scope_org (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    post_scope_id BIGINT NOT NULL,

    org_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    -- 防止同一授权范围重复绑定组织
    UNIQUE KEY uk_post_scope_org (
        post_scope_id,
        org_id
    ),

    -- 支持按组织反查授权记录
    KEY idx_post_scope_org_id (org_id),

    CONSTRAINT fk_post_scope_org_scope
        FOREIGN KEY (post_scope_id)
        REFERENCES sys_post_data_scope(id),

    CONSTRAINT fk_post_scope_org_org
        FOREIGN KEY (org_id)
        REFERENCES sys_org(id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
