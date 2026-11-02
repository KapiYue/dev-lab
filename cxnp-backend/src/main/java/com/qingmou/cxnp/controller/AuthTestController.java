
package com.qingmou.cxnp.controller;

import com.qingmou.cxnp.common.Result;
import com.qingmou.cxnp.context.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth-test")
public class AuthTestController {

    @GetMapping("/me")
    public Result<Long> me() {

        Long userId = UserContext.getUserId();

        return Result.success(userId);
    }
}
