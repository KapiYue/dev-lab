package com.qingmou.cxnp.dto;

import java.util.List;

import com.qingmou.cxnp.enums.DataScopeType;

// DTO是承载数据范围配置参数
public record DataScopeConfigDTO(
    Long roleId,
    DataScopeType scopeType,
    List<Long> orgIds
) {
}

