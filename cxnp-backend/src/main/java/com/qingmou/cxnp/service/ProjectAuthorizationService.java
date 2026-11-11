
package com.qingmou.cxnp.service;

/**
 * 项目访问授权服务。
 *
 * 负责验证当前登录用户是否有权访问指定项目。
 *
 * 当前用户身份必须来自后端认证上下文，
 * 不允许由前端传入 userId。
 */
public interface ProjectAuthorizationService {

    /**
     * 检查当前登录用户是否有权访问指定项目。
     *
     * @param projectId 项目 ID
     * @throws IllegalArgumentException 项目 ID 不合法
     * @throws com.qingmou.cxnp.exception.AuthenticationException
     *         当前用户未登录
     * @throws com.qingmou.cxnp.exception.PermissionDeniedException
     *         当前用户无权访问该项目
     */
    void checkProjectAccess(Long projectId);
}
