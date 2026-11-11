
package com.qingmou.cxnp.entity;

import com.qingmou.cxnp.enums.DataScopeType;
import java.time.LocalDateTime;

public class SysRoleDataScope {

    private Long id;
    private Long roleId;
    private DataScopeType scopeType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public DataScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(DataScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
