package com.qingmou.cxnp;

import com.qingmou.cxnp.entity.User;
import com.qingmou.cxnp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testGetAllUsers() {
        List<User> users = userService.getAllUsers();

        assertNotNull(users);

        System.out.println("Service 查询成功！");
        System.out.println("用户数量：" + users.size());

        for (User user : users) {
            System.out.println(
                    user.getId() + " - " + user.getUsername()
            );
        }
    }
}