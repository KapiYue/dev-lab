package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.PartyMember;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

// @Mapper 告诉 MyBatis 注册这个接口
@Mapper
public interface PartyMemberMapper {

    List<PartyMember> selectAll();
}
