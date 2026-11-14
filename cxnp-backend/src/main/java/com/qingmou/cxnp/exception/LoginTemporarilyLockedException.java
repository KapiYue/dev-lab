package com.qingmou.cxnp.exception;

/**
 * 登录标识因连续认证失败而被临时锁定。
 *
 * <p>统一异常处理将其映射为 HTTP 429，响应文案与普通认证失败保持一致，
 * 以避免泄露账号状态。</p>
 */
public class LoginTemporarilyLockedException extends AuthenticationException {

    public LoginTemporarilyLockedException() {
        super("用户名或密码错误");
    }
}
