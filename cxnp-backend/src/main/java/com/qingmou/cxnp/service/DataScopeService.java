
package com.qingmou.cxnp.service;

import com.qingmou.cxnp.vo.DataScopeResultVO;

public interface DataScopeService {

    /**
 * 计算当前业务操作允许访问的数据范围。
 *
 * 当前用户 ID 必须从后端 UserContext 获取，
 * 不允许由前端或调用方传入。
 *
 * projectId 必须经过业务资源归属校验。
 */
DataScopeResultVO getAccessibleScope(
    Long projectId,
    String moduleCode,
    String permissionCode
);
}
