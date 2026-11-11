package com.qingmou.cxnp.service;

import java.util.List;

public interface PostPermissionDelegationService {

    /**
     * 查询当前登录用户在指定项目、模块下，
     * 可以委派某项功能权限的管理任职 ID。
     *
     * 操作人 userId 必须从后端认证上下文获取，
     * 不允许由前端传入。
     *
     * @param projectId 当前业务项目 ID
     * @param moduleCode 业务模块编码
     * @param permissionCode 准备委派的功能权限编码
     * @return 符合条件的管理任职 ID；
     *         不具备资格时返回空列表
     */
    List<Long> getCurrentUserDelegablePostIds(
            Long projectId,
            String moduleCode,
            String permissionCode
    );

    /**
     * 将指定业务权限委派给目标任职。
     *
     * 安全要求：
     * 1. 委派者必须来自当前登录用户上下文。
     * 2. 委派者必须拥有该权限以及对应的委派资格。
     * 3. 来源任职与目标任职必须属于指定项目。
     * 4. 目标任职必须有效，并获得对应业务模块授权。
     * 5. 权限必须属于指定业务模块。
     * 6. 必须满足组织和数据范围约束。
     * 7. 全部校验通过后，才允许写入权限记录。
     *
     * @param sourcePostId 发起委派的管理岗位ID
     * @param targetPostId 接收权限的目标岗位ID
     * @param projectId 项目ID
     * @param moduleCode 业务模块编码
     * @param permissionCode 权限编码
     */
    void delegatePermission(
        Long sourcePostId,
        Long targetPostId,
        Long projectId,
        String moduleCode,
        String permissionCode
    );

    /**
     * Revokes an active delegation created by the specified source post.
     * The corresponding effective target-post permission is removed in the
     * same transaction.
     */
    void revokePermission(
            Long sourcePostId,
            Long targetPostId,
            Long projectId,
            String moduleCode,
            String permissionCode
    );
}
