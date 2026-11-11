
package com.qingmou.cxnp.entity;

import com.qingmou.cxnp.enums.DataScopeType;
import java.time.LocalDateTime;

public class SysPostDataScope {

    private Long id;
    private Long postId;
    private DataScopeType scopeType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
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
