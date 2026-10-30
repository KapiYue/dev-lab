package com.qingmou.cxnp.mapper;

import com.qingmou.cxnp.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
//    自己调用方法，不是MyBatis提供，只要对应xml中的id即可
    List<User> selectAll();
}