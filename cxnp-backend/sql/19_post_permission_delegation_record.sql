USE cxnp;

-- 任务 5.9：权限委派审计与撤销记录。
-- 有效委派与目标岗位的实际授权一一对应；撤销时在同一事务中
-- 将本表状态置为 REVOKED，并删除 sys_post_permission_grant 中的授权。
CREATE TABLE IF NOT EXISTS sys_post_permission_delegation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_post_id BIGINT NOT NULL,
    target_post_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at DATETIME NULL,
    -- 仅 ACTIVE 行生成唯一键；REVOKED 历史记录不阻止以后重新委派。
    active_target_permission VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'ACTIVE'
                THEN CONCAT(target_post_id, ':', permission_id)
                ELSE NULL
            END
        ) STORED,
    UNIQUE KEY uk_active_target_permission (active_target_permission),
    KEY idx_delegation_source_status (source_post_id, status),
    KEY idx_delegation_target_status (target_post_id, status),
    KEY idx_delegation_permission_status (permission_id, status),
    CONSTRAINT fk_delegation_source_post
        FOREIGN KEY (source_post_id) REFERENCES sys_user_post(id),
    CONSTRAINT fk_delegation_target_post
        FOREIGN KEY (target_post_id) REFERENCES sys_user_post(id),
    CONSTRAINT fk_delegation_permission
        FOREIGN KEY (permission_id) REFERENCES sys_permission(id),
    CONSTRAINT chk_delegation_status
        CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT chk_delegation_revoke_time
        CHECK (
            (status = 'ACTIVE' AND revoked_at IS NULL)
            OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 验证：不应存在两个有效来源向同一目标任职授予同一权限。
SELECT target_post_id, permission_id, COUNT(*) AS active_count
FROM sys_post_permission_delegation
WHERE status = 'ACTIVE'
GROUP BY target_post_id, permission_id
HAVING COUNT(*) > 1;
