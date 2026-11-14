
USE cxnp;

-- ============================================
-- 1. 初始化业务模块
-- ============================================

INSERT INTO sys_business_module (
    module_code,
    module_name,
    status
)
VALUES
    ('CONSTRUCTION', '施工管理', 1),
    ('SUPERVISION', '监理管理', 1)
ON DUPLICATE KEY UPDATE
    module_code = module_code;


-- ============================================
-- 2. 初始化施工、监理功能权限
-- ============================================

INSERT INTO sys_permission (
    permission_code,
    permission_name
)
SELECT
    codes.permission_code,
    codes.permission_name
FROM (
    SELECT
        'construction:progress:view'
            AS permission_code,
        '施工进度查看'
            AS permission_name

    UNION ALL SELECT
        'construction:progress:report',
        '施工进度上报'

    UNION ALL SELECT
        'construction:progress:approve',
        '施工进度审批'

    UNION ALL SELECT
        'construction:permission:configure',
        '施工权限配置'

    UNION ALL SELECT
        'supervision:record:view',
        '监理记录查看'

    UNION ALL SELECT
        'supervision:quality:inspect',
        '监理质量检查'

    UNION ALL SELECT
        'supervision:record:approve',
        '监理记录审批'

    UNION ALL SELECT
        'supervision:permission:configure',
        '监理权限配置'
) codes
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission existing
    WHERE existing.permission_code =
          codes.permission_code
);


-- ============================================
-- 3. 绑定业务模块与功能权限
-- ============================================

INSERT INTO sys_module_permission (
    module_id,
    permission_id
)
SELECT
    bm.id,
    p.id
FROM sys_business_module bm
JOIN sys_permission p
    ON (
        bm.module_code = 'CONSTRUCTION'
        AND p.permission_code IN (
            'construction:progress:view',
            'construction:progress:report',
            'construction:progress:approve',
            'construction:permission:configure'
        )
    )
    OR (
        bm.module_code = 'SUPERVISION'
        AND p.permission_code IN (
            'supervision:record:view',
            'supervision:quality:inspect',
            'supervision:record:approve',
            'supervision:permission:configure'
        )
    )
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_module_permission existing
    WHERE existing.permission_id = p.id
);
