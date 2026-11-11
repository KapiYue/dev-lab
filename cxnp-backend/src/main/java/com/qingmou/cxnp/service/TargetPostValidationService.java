package com.qingmou.cxnp.service;

public interface TargetPostValidationService {

    /**
     * 校验目标任职是否有效。
     *
     * 有效条件：
     * 1. 任职存在且已启用
     * 2. 所属用户已启用
     * 3. 所属项目已启用
     * 4. 所属业务角色已启用
     * 5. 所属组织已启用
     * 6. 任职组织属于指定项目
     *
     * @param targetPostId 目标任职 ID
     * @param projectId 项目 ID
     * @throws IllegalArgumentException 参数非法时抛出
     * @throws com.qingmou.cxnp.exception.PermissionDeniedException
     *         目标任职不存在、不属于指定项目或已失效时抛出
     */
    void validateTargetPost(
            Long targetPostId,
            Long projectId
    );

    /**
         * 校验目标任职是否具备指定业务模块的有效授权。
         *
         * @param targetPostId 目标任职ID
         * @param projectId 项目ID
         * @param moduleCode 业务模块编码
         */
        void validateTargetPostModule(
                Long targetPostId,
                Long projectId,
                String moduleCode
        );

        /**
         * 校验指定权限是否属于指定业务模块。
         *
         * @param moduleCode 业务模块编码
         * @param permissionCode 权限编码
         */
        void validateModulePermission(
                String moduleCode,
                String permissionCode
        );
}