
package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.entity.SysOrg;
import com.qingmou.cxnp.entity.SysPostDataScope;
import com.qingmou.cxnp.enums.DataScopeType;
import com.qingmou.cxnp.mapper.DataScopeMapper;
import com.qingmou.cxnp.mapper.PostAuthorizationMapper;
import com.qingmou.cxnp.service.DataScopeService;
import com.qingmou.cxnp.service.ProjectAuthorizationService;
import com.qingmou.cxnp.vo.DataScopeResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.exception.AuthenticationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DataScopeServiceImpl implements DataScopeService {

    private final PostAuthorizationMapper postAuthorizationMapper;
    private final DataScopeMapper dataScopeMapper;
    private final ProjectAuthorizationService projectAuthorizationService;

    public DataScopeServiceImpl(
            PostAuthorizationMapper postAuthorizationMapper,
            DataScopeMapper dataScopeMapper,
            ProjectAuthorizationService projectAuthorizationService
        ) {
        this.postAuthorizationMapper = postAuthorizationMapper;
        this.dataScopeMapper = dataScopeMapper;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Override
    @Transactional(readOnly = true)
    public DataScopeResultVO getAccessibleScope(
            Long projectId,
            String moduleCode,
            String permissionCode) {
        // 1. 从后端认证上下文获取当前用户
        Long authenticatedUserId = UserContext.getUserId();

        if (authenticatedUserId == null) {
            throw new AuthenticationException("未登录或登录已过期");
        }
        // 2. 验证当前用户是否有权访问指定项目
        projectAuthorizationService.checkProjectAccess(projectId);

        // 1. 验证授权上下文
        if (projectId == null
                || moduleCode == null
                || moduleCode.isBlank()
                || permissionCode == null
                || permissionCode.isBlank()) {

            throw new IllegalArgumentException(
                    "数据权限计算缺少必要上下文");
        }

        // 2. 查询当前项目允许参与的有效组织
        List<Long> projectOrgIds = postAuthorizationMapper
                .selectActiveOrgIdsByProjectId(projectId);

        Set<Long> allowedProjectOrgIds = new HashSet<>();

        if (projectOrgIds != null) {
            for (Long orgId : projectOrgIds) {
                if (orgId != null) {
                    allowedProjectOrgIds.add(orgId);
                }
            }
        }

        // 项目没有有效组织时，默认拒绝
        if (allowedProjectOrgIds.isEmpty()) {
            return emptyScope(
                    projectId,
                    moduleCode,
                    permissionCode);
        }

        // 3. 查询有当前功能权限的有效任职
        List<Long> postIds = postAuthorizationMapper.selectAuthorizedPostIds(
                authenticatedUserId,
                projectId,
                moduleCode,
                permissionCode);

        if (postIds == null || postIds.isEmpty()) {
            return emptyScope(
                    projectId,
                    moduleCode,
                    permissionCode);
        }

        Set<Long> resultOrgIds = new HashSet<>();
        // 4. 分别计算每条有效任职的数据范围
        for (Long postId : postIds) {

            if (postId == null) {
                continue;
            }

            SysPostDataScope scope = postAuthorizationMapper.selectScopeByPostId(
                    postId);

            if (scope == null || scope.getScopeType() == null) {
                continue;
            }

            DataScopeType type = scope.getScopeType();

            switch (type) {

                case ALL -> {
                    // 只表示当前项目、当前业务操作的 ALL
                    // 业务 SQL 仍需限制 project_id
                    // 以及当前项目关联的有效组织
                    return new DataScopeResultVO(
                            projectId,
                            moduleCode,
                            permissionCode,
                            true,
                            false,
                            allowedProjectOrgIds);
                }

                case CUSTOM -> {
                    List<Long> customOrgIds = postAuthorizationMapper
                            .selectCustomOrgIdsByPostId(
                                    postId);

                    // CUSTOM 授权范围与项目组织取交集
                    if (customOrgIds != null) {
                        for (Long orgId : customOrgIds) {
                            if (orgId != null
                                    && allowedProjectOrgIds
                                            .contains(orgId)) {
                                resultOrgIds.add(orgId);
                            }
                        }
                    }
                }

                case ORG_AND_CHILD -> {
                    Long postOrgId = postAuthorizationMapper
                            .selectOrgIdByPostId(postId);

                    if (postOrgId == null) {
                        continue;
                    }

                    List<SysOrg> orgTree = dataScopeMapper.selectOrgAndChildren(
                            postOrgId);

                    if (orgTree != null) {
                        for (SysOrg org : orgTree) {
                            if (org != null
                                    && org.getId() != null
                                    && allowedProjectOrgIds
                                            .contains(org.getId())) {

                                resultOrgIds.add(org.getId());
                            }
                        }
                    }
                }

                case SELF -> {
                    // SELF 是组织级授权：当前任职所属组织，不包含下级组织；
                    // 不是“仅本人记录”授权。
                    Long postOrgId = postAuthorizationMapper.selectOrgIdByPostId(postId);

                    if (postOrgId != null
                            && allowedProjectOrgIds.contains(postOrgId)) {
                        resultOrgIds.add(postOrgId);
                    }
                }
            }
        }

        // 5. 返回当前项目和操作下的最终范围
        return new DataScopeResultVO(
                projectId,
                moduleCode,
                permissionCode,
                false,
                false,
                resultOrgIds);
    }

    private DataScopeResultVO emptyScope(
            Long projectId,
            String moduleCode,
            String permissionCode) {
        return new DataScopeResultVO(
                projectId,
                moduleCode,
                permissionCode,
                false,
                false,
                Set.of());
    }
}
