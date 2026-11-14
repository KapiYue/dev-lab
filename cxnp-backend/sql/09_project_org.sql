
USE cxnp;

-- 项目与组织关联表
CREATE TABLE IF NOT EXISTS sys_project_org (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    project_id BIGINT NOT NULL,

    org_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    -- 同一个项目不能重复绑定同一个组织
    UNIQUE KEY uk_project_org (
        project_id,
        org_id
    ),

    -- 支持反向查询组织参与的项目
    KEY idx_project_org_org (
        org_id
    ),

    CONSTRAINT fk_project_org_project
        FOREIGN KEY (project_id)
        REFERENCES sys_project(id),

    CONSTRAINT fk_project_org_org
        FOREIGN KEY (org_id)
        REFERENCES sys_org(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
