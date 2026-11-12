-- 党员组织迁移收口。
-- 只在 sql/21 最后一条校验查询返回零行后执行。
-- 若仍有 NULL，下面 ALTER 会失败并保留原结构，不会将未归属党员错误授权。

USE cxnp;

ALTER TABLE party_member
    MODIFY COLUMN org_id BIGINT NOT NULL COMMENT '所属组织 ID';
