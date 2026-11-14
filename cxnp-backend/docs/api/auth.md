# 认证 API

## 登录

`POST /api/auth/login`

使用用户名和密码创建独立的 Bearer 会话。同一账号可以在多个客户端同时登录；每次成功登录返回不同的 Token。认证失败、账号停用和临时锁定均不区分账号状态。

### 请求

```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "zhangsan",
  "password": "<password>"
}
```

### 成功响应

HTTP 状态码：`200 OK`

```json
{
  "code": 200,
  "message": "success",
  "data": "<token>"
}
```

### 认证失败与临时锁定

连续 5 次失败登录后，系统按登录标识临时锁定 15 分钟；锁定期内不会校验密码。普通认证失败返回 `401 Unauthorized`，临时锁定返回 `429 Too Many Requests`；两者均使用相同的响应文案，且 `data` 为 `null`：

```json
{
  "code": 429,
  "message": "用户名或密码错误",
  "data": null
}
```

成功登录会清除该登录标识的失败计数和锁定状态。

## 注销当前会话

`POST /api/auth/logout`

注销请求中 Bearer Token 所代表的当前会话。该接口不接收用户 ID，也不会影响同一用户在其他客户端持有的有效会话。

### 认证要求

必须在请求头携带有效的当前会话 Token：

```http
Authorization: Bearer <token>
```

缺少、格式错误、已过期或已注销的 Token 均返回 `401 Unauthorized`。

### 请求

请求体为空。

```http
POST /api/auth/logout HTTP/1.1
Authorization: Bearer <token>
```

### 成功响应

HTTP 状态码：`200 OK`

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

成功后，当前 Token 会立即失效；同一用户的其他有效 Token 仍可继续访问受保护 API。

### 未认证响应

HTTP 状态码：`401 Unauthorized`

```json
{
  "code": 401,
  "message": "未登录或登录已过期",
  "data": null
}
```

### 无功能权限响应

HTTP 状态码：`403 Forbidden`

```json
{
  "code": 403,
  "message": "无功能权限",
  "data": null
}
```

## 管理员撤销用户全部会话

`POST /api/users/{userId}/sessions/revoke`

撤销指定用户的全部有效会话。该操作只影响路径中的目标用户，重复调用在目标用户没有有效会话时仍返回成功。

### 认证与权限要求

请求必须携带有效 Bearer Token，且当前用户必须拥有 `rbac:user:session:revoke` 功能权限。未认证返回 `401 Unauthorized`；没有该权限返回 `403 Forbidden`。

### 请求

`userId` 必须是大于 0 的整数；非法格式或非正数返回 `400 Bad Request`。格式正确但不存在的用户返回 `404 Not Found`。

```http
POST /api/users/123/sessions/revoke HTTP/1.1
Authorization: Bearer <administrator-token>
```

### 成功响应

HTTP 状态码：`200 OK`

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

成功后，目标用户的全部有效 Token 会立即失效。
