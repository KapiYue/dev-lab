
package com.qingmou.cxnp.context;

public final class UserContext {

    // 每个请求处理线程拥有独立的用户 ID
    private static final ThreadLocal<Long> CURRENT_USER =
            new ThreadLocal<>();

    private UserContext() {
        // 工具类不允许实例化
    }

    // 保存当前登录用户 ID
    public static void setUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    // 获取当前登录用户 ID
    public static Long getUserId() {
        return CURRENT_USER.get();
    }

    // 请求结束后清理
    public static void clear() {
        CURRENT_USER.remove();
    }
}
