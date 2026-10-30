package com.qingmou.cxnp;

import com.qingmou.cxnp.entity.User;
import com.qingmou.cxnp.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void testSelectAll() {
        List<User> users = userMapper.selectAll();

        System.out.println("查询到用户数量：" + users.size());

        for (User user : users) {
            System.out.println(
                    user.getId() + " - " +
                            user.getUsername()
            );
        }
    }
}