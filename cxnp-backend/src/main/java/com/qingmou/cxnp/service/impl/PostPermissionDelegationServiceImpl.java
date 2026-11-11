package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.context.UserContext;
import com.qingmou.cxnp.entity.SysOrg;
import com.qingmou.cxnp.entity.SysPostDataScope;
import com.qingmou.cxnp.enums.DataScopeType;
import com.qingmou.cxnp.mapper.DataScopeMapper;
import com.qingmou.cxnp.mapper.PostAuthorizationMapper;
import com.qingmou.cxnp.service.PostPermissionDelegationService;
import com.qingmou.cxnp.service.ProjectAuthorizationService;
import com.qingmou.cxnp.service.TargetPostValidationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.qingmou.cxnp.exception.AuthenticationException;
import com.qingmou.cxnp.exception.PermissionDeniedException;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
public class PostPermissionDelegationServiceImpl
        implements PostPermissionDelegationService {

    private final PostAuthorizationMapper postAuthorizationMapper;
    private final TargetPostValidationService targetPostValidationService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final DataScopeMapper dataScopeMapper;

    public PostPermissionDelegationServiceImpl(
            PostAuthorizationMapper postAuthorizationMapper,
            TargetPostValidationService targetPostValidationService,
            ProjectAuthorizationService projectAuthorizationService,
            DataScopeMapper dataScopeMapper) {
        this.postAuthorizationMapper = postAuthorizationMapper;
        this.targetPostValidationService = targetPostValidationService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.dataScopeMapper = dataScopeMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getCurrentUserDelegablePostIds(
            Long projectId,
            String moduleCode,
            String permissionCode) {

        // 1. 校验项目 ID
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException(
                    "项目 ID 不能为空且必须大于 0");
        }

        // 2. 校验模块编码
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException(
                    "业务模块编码不能为空");
        }

        // 3. 校验权限编码
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException(
                    "功能权限编码不能为空");
        }

        // 4. 从后端认证上下文获取当前用户
        Long currentUserId = UserContext.getUserId();

        if (currentUserId == null) {
            // 不允许在没有认证上下文时查询委派资格
            throw new AuthenticationException("未登录或登录已过期");
        }

        projectAuthorizationService.checkProjectAccess(projectId);

        // 5. 查询当前用户符合条件的管理任职
        List<Long> postIds = postAuthorizationMapper.selectDelegablePostIds(
                currentUserId,
                projectId,
                moduleCode,
                permissionCode);

        // 6. 不具备资格时返回空列表
        return postIds == null ? List.of() : List.copyOf(postIds);
    }

    @Override
    @Transactional
    public void delegatePermission(
            Long sourcePostId,
            Long targetPostId,
            Long projectId,
            String moduleCode,
            String permissionCode) {
        // 1. 校验来源任职 ID
        if (sourcePostId == null || sourcePostId <= 0) {
            throw new IllegalArgumentException("来源任职ID不合法");
        }

        // 2. 校验目标任职 ID
        if (targetPostId == null || targetPostId <= 0) {
            throw new IllegalArgumentException("目标任职ID不合法");
        }

        // 3. 校验项目 ID
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("项目ID不合法");
        }

        // 4. 校验业务模块编码
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("业务模块编码不能为空");
        }

        // 5. 校验权限编码
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("权限编码不能为空");
        }

        // 6. 查询当前登录用户具备委派资格的来源岗位
        List<Long> delegablePostIds = getCurrentUserDelegablePostIds(
                projectId,
                moduleCode,
                permissionCode);

        // 7. 必须明确使用拥有委派资格的来源岗位
        if (!delegablePostIds.contains(sourcePostId)) {
            throw new PermissionDeniedException(
                    "当前用户不具备该来源任职的权限委派资格");
        }
        // 8. 校验目标任职是否有效，并属于指定项目
        targetPostValidationService.validateTargetPost(
                targetPostId,
                projectId);

        // 9. 校验目标任职是否拥有指定业务模块的授权
        targetPostValidationService.validateTargetPostModule(
                targetPostId,
                projectId,
                moduleCode);

        // 10. 校验具体权限是否属于指定业务模块
        targetPostValidationService.validateModulePermission(
                moduleCode,
                permissionCode);
        Long permissionId = postAuthorizationMapper.selectValidModulePermissionId(
                moduleCode, permissionCode);
        validateTargetWithinSourceScope(sourcePostId, targetPostId, projectId);

        if (postAuthorizationMapper.countPostPermissionGrant(
                targetPostId, permissionId) > 0) {
            throw new PermissionDeniedException("目标任职已拥有该功能权限，不能重复委派");
        }

        if (postAuthorizationMapper.insertPostPermissionGrant(
                targetPostId, permissionId) != 1
                || postAuthorizationMapper.insertPermissionDelegation(
                sourcePostId, targetPostId, permissionId) != 1) {
            throw new IllegalStateException("权限委派写入失败");
        }
    }

    @Override
    @Transactional
    public void revokePermission(
            Long sourcePostId,
            Long targetPostId,
            Long projectId,
            String moduleCode,
            String permissionCode) {
        validateDelegationRequest(sourcePostId, targetPostId, projectId,
                moduleCode, permissionCode);

        List<Long> delegablePostIds = getCurrentUserDelegablePostIds(
                projectId, moduleCode, permissionCode);
        if (!delegablePostIds.contains(sourcePostId)) {
            throw new PermissionDeniedException("当前用户不具备该来源任职的权限撤销资格");
        }

        targetPostValidationService.validateTargetPost(targetPostId, projectId);
        targetPostValidationService.validateTargetPostModule(targetPostId, projectId, moduleCode);
        Long permissionId = postAuthorizationMapper.selectValidModulePermissionId(
                moduleCode, permissionCode);

        if (postAuthorizationMapper.revokeActivePermissionDelegation(
                sourcePostId, targetPostId, permissionId) != 1) {
            throw new PermissionDeniedException("不存在可撤销的有效权限委派记录");
        }
        if (postAuthorizationMapper.deletePostPermissionGrant(targetPostId, permissionId) != 1) {
            throw new IllegalStateException("权限撤销失败：未找到有效权限授予记录");
        }
    }

    private void validateTargetWithinSourceScope(
            Long sourcePostId, Long targetPostId, Long projectId) {
        Long targetOrgId = postAuthorizationMapper.selectOrgIdByPostId(targetPostId);
        if (targetOrgId == null || !resolveScopeOrgIds(sourcePostId, projectId)
                .contains(targetOrgId)) {
            throw new PermissionDeniedException("目标任职所属组织不在来源任职的数据范围内");
        }
    }

    private Set<Long> resolveScopeOrgIds(Long postId, Long projectId) {
        Set<Long> projectOrgIds = new HashSet<>();
        List<Long> activeProjectOrgIds = postAuthorizationMapper
                .selectActiveOrgIdsByProjectId(projectId);
        if (activeProjectOrgIds != null) {
            activeProjectOrgIds.stream().filter(id -> id != null)
                    .forEach(projectOrgIds::add);
        }

        SysPostDataScope scope = postAuthorizationMapper.selectScopeByPostId(postId);
        if (scope == null || scope.getScopeType() == null || projectOrgIds.isEmpty()) {
            return Set.of();
        }
        if (scope.getScopeType() == DataScopeType.ALL) {
            return projectOrgIds;
        }

        Set<Long> result = new HashSet<>();
        if (scope.getScopeType() == DataScopeType.CUSTOM) {
            List<Long> customOrgIds = postAuthorizationMapper.selectCustomOrgIdsByPostId(postId);
            if (customOrgIds != null) {
                customOrgIds.stream().filter(projectOrgIds::contains).forEach(result::add);
            }
        } else {
            Long postOrgId = postAuthorizationMapper.selectOrgIdByPostId(postId);
            if (postOrgId == null) {
                return Set.of();
            }
            if (scope.getScopeType() == DataScopeType.SELF) {
                if (projectOrgIds.contains(postOrgId)) {
                    result.add(postOrgId);
                }
            } else if (scope.getScopeType() == DataScopeType.ORG_AND_CHILD) {
                List<SysOrg> orgs = dataScopeMapper.selectOrgAndChildren(postOrgId);
                if (orgs != null) {
                    orgs.stream().map(SysOrg::getId).filter(projectOrgIds::contains)
                            .forEach(result::add);
                }
            }
        }
        return result;
    }

    private void validateDelegationRequest(
            Long sourcePostId, Long targetPostId, Long projectId,
            String moduleCode, String permissionCode) {
        if (sourcePostId == null || sourcePostId <= 0) {
            throw new IllegalArgumentException("来源任职ID不合法");
        }
        if (targetPostId == null || targetPostId <= 0) {
            throw new IllegalArgumentException("目标任职ID不合法");
        }
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("项目ID不合法");
        }
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("业务模块编码不能为空");
        }
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("权限编码不能为空");
        }
    }
}
