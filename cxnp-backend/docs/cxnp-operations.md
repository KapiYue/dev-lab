# cxnp-backend 项目运维手册

技术环境：macOS + Docker + MySQL 8.0 + Redis 7.4 + Spring Boot + DataGrip

## 一、首次启动（只需要配置一次）

### 1. 检查 3306 端口

```bash
sudo lsof -nP -iTCP:3306 -sTCP:LISTEN
```

如果是 Mac 本机 `mysqld` 占用，停止本机 MySQL：

```bash
sudo launchctl bootout system/com.oracle.oss.mysql.mysqld
```

禁止本机 MySQL 自动启动，避免下次冲突：

```bash
sudo launchctl disable system/com.oracle.oss.mysql.mysqld
```

### 2. 启动 Docker MySQL

先打开 Docker Desktop。

```bash
# 已经创建过容器，直接启动
docker start cxnp-mysql

# 检查运行状态
docker ps
```

如果是新电脑且没有容器，需要先按照项目 Docker 配置创建容器。

### 3. 配置后端数据库

`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/cxnp
    username: root
    password: ${DB_PASSWORD}
```

IDEA → Run → Edit Configurations → Environment variables：

```text
DB_PASSWORD=数据库密码
```

### 4. 配置 Redis

**第一步：拉取 Redis 镜像**

```bash
docker pull redis:7.4
```

**第二步：创建 Redis 容器（仅首次执行）**

```bash
docker run -d \
  --name cxnp-redis \
  -p 127.0.0.1:6379:6379 \
  -v cxnp-redis-data:/data \
  --restart unless-stopped \
  redis:7.4 \
  redis-server --appendonly yes
```

说明：

- 容器名称：`cxnp-redis`
- Redis 默认端口：`6379`
- 使用 Docker Volume 保存 Redis 数据。
- 开启 AOF 持久化。
- 仅允许本机访问，适用于本地学习环境。

**第三步：验证 Redis**

```bash
docker exec -it cxnp-redis redis-cli
```

进入 Redis 后：

```redis
PING
SET username "zhangsan"
GET username
DEL username
```

`PING` 返回 `PONG`，表示服务正常。

输入 `exit` 退出 Redis 命令行。

**第四步：配置 Spring Boot**

在 `pom.xml` 的 `<dependencies>` 中增加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

IDEA 重新加载 Maven 项目。

在 `application.yml` 原有 `spring` 节点下增加 Redis 配置，不要覆盖已有的 MySQL 配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/cxnp
    username: root
    password: ${DB_PASSWORD}

  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      timeout: 3000ms
```

注意：Redis 是认证会话的必要运行时依赖，不只是可选缓存。服务使用它保存 Bearer 会话、用户会话索引、登录失败计数和临时锁定状态；Redis 不可用时，登录和受保护请求会失败关闭。

认证会话策略如下：会话初始有效期为两小时，临近过期（剩余不超过 30 分钟）的受保护请求会续期；同一账号允许多端并存。连续 5 次登录失败会锁定对应登录标识 15 分钟，成功登录会清除失败及锁定状态。

### 5. 启动并验证

**检查 Docker 容器：**

```bash
docker ps
```

确认以下两个容器正在运行：

```text
cxnp-mysql
cxnp-redis
```

**验证 MySQL：**

- DataGrip：连接 `127.0.0.1:3306`
- 数据库：`cxnp`

**验证 Redis：**

```bash
docker exec cxnp-redis redis-cli PING
```

预期返回：

```text
PONG
```

**验证 Spring Boot：**

IDEA 运行 `CxnpApplication`。

终端执行：

```bash
curl http://localhost:8080/api/users
```

接口正常返回用户数据，说明已有后端查询链路正常。

注意：`/api/users` 仅验证当前 MySQL 查询接口，不代表 Redis 业务读写已经验证。

---

## 二、日常开发启动（每天执行）

**第一步：** 打开 Docker Desktop。

**第二步：** 启动 MySQL 和 Redis。

```bash
docker start cxnp-mysql
docker start cxnp-redis
```

如果容器已经运行，无须重复启动。

**第三步：** 检查服务状态。

```bash
docker ps
docker exec cxnp-redis redis-cli PING
```

确认 MySQL 和 Redis 正常运行。

**第四步：** IDEA 运行 `CxnpApplication`。

**第五步：** 验证后端接口。

```bash
curl http://localhost:8080/api/users
```

DataGrip 只在需要查看或修改 MySQL 数据库时打开。

---

## 三、日常停止

```bash
# 1. IDEA 点击 Stop，停止 Spring Boot

# 2. 按需停止 MySQL
docker stop cxnp-mysql

# 3. 按需停止 Redis
docker stop cxnp-redis
```

---

## 四、常见问题

| 问题 | 解决方法 |
|---|---|
| 3306 端口被占用 | 检查本机 Oracle MySQL，必要时执行 `sudo launchctl bootout system/com.oracle.oss.mysql.mysqld` |
| 6379 端口被占用 | `lsof -nP -iTCP:6379 -sTCP:LISTEN` |
| Docker 无法连接 | 打开 Docker Desktop |
| MySQL 没启动 | `docker start cxnp-mysql` |
| Redis 没启动 | `docker start cxnp-redis` |
| 查看 MySQL 日志 | `docker logs --tail 50 cxnp-mysql` |
| 查看 Redis 日志 | `docker logs --tail 50 cxnp-redis` |
| 后端数据库连接失败 | 检查 MySQL 状态及 `DB_PASSWORD` |
| Redis 连接失败 | 检查 Redis 容器、6379 端口及 `application.yml`；认证登录和受保护接口会因此失败关闭 |
| 8080 端口被占用 | `lsof -nP -iTCP:8080 -sTCP:LISTEN` |

---

## 五、注意事项

1. 本地开发统一使用 Docker MySQL 和 Docker Redis。
2. 不要每天重新创建容器或数据库。
3. `docker stop` 不会删除 MySQL 或 Redis 的持久化数据。
4. MySQL 使用 `3306` 端口，Redis 使用 `6379` 端口。
5. 本地 Docker MySQL 与腾讯云 MySQL 是独立环境，操作前确认连接目标。
6. 不要随意删除 MySQL、Redis 容器和数据卷。
7. Redis 当前仅供本地学习，不要直接暴露到公网；其中保存认证会话和登录失败保护状态。
8. Spring Boot 的 `application.yml` 只保留一个顶层 `spring` 节点。

**核心流程：Docker Desktop → MySQL + Redis → Spring Boot → API 验证。**
