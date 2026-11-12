package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.PartyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Set;

// @Mapper 告诉 MyBatis 注册这个接口
@Mapper
public interface PartyMemberMapper {

    /** 仅查询后端已计算数据范围内的党员。 */
    List<PartyMember> selectByDataScope(
            @Param("all") boolean all,
            @Param("includeSelf") boolean includeSelf,
            @Param("userId") Long userId,
            @Param("orgIds") Set<Long> orgIds
    );

    /** 仅在后端已计算数据范围内按 ID 查询党员，避免详情越权。 */
    PartyMember selectByIdWithinDataScope(
            @Param("id") Long id,
            @Param("all") boolean all,
            @Param("includeSelf") boolean includeSelf,
            @Param("userId") Long userId,
            @Param("orgIds") Set<Long> orgIds
    );
}
