
package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.exception.AuthenticationException;
import com.qingmou.cxnp.exception.PermissionDeniedException;
import com.qingmou.cxnp.mapper.PostAuthorizationMapper;
import com.qingmou.cxnp.service.ProjectAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectAuthorizationServiceImpl
        implements ProjectAuthorizationService {

    private final PostAuthorizationMapper postAuthorizationMapper;

    public ProjectAuthorizationServiceImpl(
            PostAuthorizationMapper postAuthorizationMapper
    ) {
        this.postAuthorizationMapper = postAuthorizationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public void checkProjectAccess(Long projectId) {

        // 1. 校验项目 ID
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("项目 ID 不合法");
        }

        // 2. 从后端认证上下文获取当前用户
        Long currentUserId = UserContext.getUserId();

        if (currentUserId == null) {
            throw new AuthenticationException("未登录或登录已过期");
        }

        // 3. 查询当前用户有权访问的项目
        List<Long> authorizedProjectIds =
                postAuthorizationMapper.selectAuthorizedProjectIds(
                        currentUserId
                );

        // 4. 验证当前项目是否在授权范围内
        if (authorizedProjectIds == null
            || authorizedProjectIds.isEmpty()
                || !authorizedProjectIds.contains(projectId)) {

            throw new PermissionDeniedException(
                    "当前用户无权访问该项目"
            );
        }
    }
}
