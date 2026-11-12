package com.qingmou.cxnp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Entity 负责表示数据库中的一条业务记录，不负责执行 SQL。
// join_date属于mySql蛇形命名（下划线命名），对应java中joinDate
public class PartyMember {

    private Long id;
    private String name;
    private String gender;
    private String phone;
    private LocalDate joinDate;
    private String partyBranch;
    private Long orgId;
    private Long userId;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public String getPartyBranch() {
        return partyBranch;
    }

    public void setPartyBranch(String partyBranch) {
        this.partyBranch = partyBranch;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    /** 系统账号关联仅供服务层数据权限判断，不对外暴露。 */
    @JsonIgnore
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
