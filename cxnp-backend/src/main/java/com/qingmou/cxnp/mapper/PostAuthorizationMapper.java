
package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.SysPostDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostAuthorizationMapper {

        // 查询当前用户在指定项目、业务模块、
        // 操作权限下符合条件的任职 ID
        List<Long> selectAuthorizedPostIds(
                        @Param("userId") Long userId,
                        @Param("projectId") Long projectId,
                        @Param("moduleCode") String moduleCode,
                        @Param("permissionCode") String permissionCode);

        // 查询任职对应的数据范围配置
        SysPostDataScope selectScopeByPostId(
                        @Param("postId") Long postId);

        // 查询 CUSTOM 范围下的指定组织 ID
        List<Long> selectCustomOrgIdsByPostId(
                        @Param("postId") Long postId);

        // 查询任职所属组织 ID
        Long selectOrgIdByPostId(
                        @Param("postId") Long postId);

        // 查询当前项目关联的全部有效组织 ID
        List<Long> selectActiveOrgIdsByProjectId(
                        @Param("projectId") Long projectId);

        /**
         * 查询当前用户在指定项目和模块中，
         * 可以委派某项功能权限的有效管理任职 ID。
         *
         * userId 必须来自后端认证上下文。
         *
         * 返回空集合表示没有符合条件的委派资格。
         *
         * 此方法不负责检查目标任职以及实际委派范围。
         */
        List<Long> selectDelegablePostIds(
                        @Param("userId") Long userId,
                        @Param("projectId") Long projectId,
                        @Param("moduleCode") String moduleCode,
                        @Param("permissionCode") String permissionCode);

        /**
         * 查询指定项目内有效的目标任职 ID。
         *
         * 有效条件：
         * 1. 任职启用
         * 2. 所属用户启用
         * 3. 项目启用
         * 4. 组织启用
         * 5. 任职组织属于该项目
         */
        Long selectValidTargetPostId(
                        @Param("targetPostId") Long targetPostId,
                        @Param("projectId") Long projectId);

        /**
         * 判断目标任职在指定项目中是否拥有有效的业务模块授权。
         *
         * 返回目标任职ID：授权有效
         * 返回null：未授权或任职、项目、模块无效
         */
        Long selectValidTargetPostModule(
                        @Param("targetPostId") Long targetPostId,
                        @Param("projectId") Long projectId,
                        @Param("moduleCode") String moduleCode);

        /**
         * 查询指定权限是否属于已启用的业务模块。
         *
         * 返回权限ID：归属关系有效
         * 返回null：模块不存在、被禁用或权限不属于该模块
         */
        Long selectValidModulePermissionId(
                        @Param("moduleCode") String moduleCode,
                        @Param("permissionCode") String permissionCode);

        /**
         * 查询当前用户有权访问的有效项目 ID。
         *
         * 用户必须来自后端认证上下文。
         * 只返回存在有效任职关系的项目。
         *
         * @param userId 当前登录用户 ID
         * @return 当前用户有权访问的项目 ID
         */
        List<Long> selectAuthorizedProjectIds(
                        @Param("userId") Long userId);

        /** Returns the permission ID only when the target post does not already own it. */
        int countPostPermissionGrant(
                        @Param("postId") Long postId,
                        @Param("permissionId") Long permissionId);

        /** Creates the effective permission grant for a target post. */
        int insertPostPermissionGrant(
                        @Param("postId") Long postId,
                        @Param("permissionId") Long permissionId);

        /** Records the source of an effective delegated grant. */
        int insertPermissionDelegation(
                        @Param("sourcePostId") Long sourcePostId,
                        @Param("targetPostId") Long targetPostId,
                        @Param("permissionId") Long permissionId);

        /** Marks the active delegation revoked; returns 1 only for an active record. */
        int revokeActivePermissionDelegation(
                        @Param("sourcePostId") Long sourcePostId,
                        @Param("targetPostId") Long targetPostId,
                        @Param("permissionId") Long permissionId);

        /** Removes the effective grant after its delegation record was revoked. */
        int deletePostPermissionGrant(
                        @Param("postId") Long postId,
                        @Param("permissionId") Long permissionId);
}
