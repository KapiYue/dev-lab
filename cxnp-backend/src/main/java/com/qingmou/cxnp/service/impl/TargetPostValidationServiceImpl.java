package com.qingmou.cxnp.service.impl;

import com.qingmou.cxnp.exception.PermissionDeniedException;
import com.qingmou.cxnp.mapper.PostAuthorizationMapper;
import com.qingmou.cxnp.service.TargetPostValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TargetPostValidationServiceImpl
        implements TargetPostValidationService {

    private final PostAuthorizationMapper postAuthorizationMapper;

    public TargetPostValidationServiceImpl(
            PostAuthorizationMapper postAuthorizationMapper
    ) {
        this.postAuthorizationMapper = postAuthorizationMapper;
    }
    

    @Override
    @Transactional(readOnly = true)
    public void validateTargetPost(
            Long targetPostId,
            Long projectId
    ) {
        // 1. 校验目标任职 ID
        if (targetPostId == null || targetPostId <= 0) {
            throw new IllegalArgumentException("目标任职ID不合法");
        }

        // 2. 校验项目 ID
        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("项目ID不合法");
        }

        // 3. 查询有效目标任职
        Long validPostId = postAuthorizationMapper
                .selectValidTargetPostId(targetPostId, projectId);

        // 4. 不存在、已禁用或不属于指定项目，一律拒绝
        if (validPostId == null) {
            throw new PermissionDeniedException("目标任职不存在或无效");
        }
    }

    @Override
@Transactional(readOnly = true)
public void validateTargetPostModule(
        Long targetPostId,
        Long projectId,
        String moduleCode
) {
    // 1. 校验参数
    if (targetPostId == null || targetPostId <= 0) {
        throw new IllegalArgumentException("目标任职ID不合法");
    }

    if (projectId == null || projectId <= 0) {
        throw new IllegalArgumentException("项目ID不合法");
    }

    if (moduleCode == null || moduleCode.isBlank()) {
        throw new IllegalArgumentException("业务模块编码不能为空");
    }

    // 2. 查询目标任职是否拥有指定模块的有效授权
    Long validPostId = postAuthorizationMapper
            .selectValidTargetPostModule(
                    targetPostId,
                    projectId,
                    moduleCode
            );

    // 3. 未授权或任职无效，拒绝操作
    if (validPostId == null) {
        throw new PermissionDeniedException(
                "目标任职不存在、无效或未获得该业务模块授权"
        );
    }
   }

   @Override
@Transactional(readOnly = true)
public void validateModulePermission(
        String moduleCode,
        String permissionCode
) {
    // 1. 校验业务模块编码
    if (moduleCode == null || moduleCode.isBlank()) {
        throw new IllegalArgumentException("业务模块编码不能为空");
    }

    // 2. 校验权限编码
    if (permissionCode == null || permissionCode.isBlank()) {
        throw new IllegalArgumentException("权限编码不能为空");
    }

    // 3. 查询权限与模块的归属关系
    Long permissionId = postAuthorizationMapper
            .selectValidModulePermissionId(
                    moduleCode,
                    permissionCode
            );

    // 4. 权限不属于指定模块，拒绝操作
    if (permissionId == null) {
        throw new PermissionDeniedException(
                "权限不存在、业务模块无效或权限不属于指定业务模块"
        );
    }
   }
   

}