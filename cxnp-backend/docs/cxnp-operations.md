# cxnp-backend 项目运维手册

技术环境：macOS + Docker + MySQL 8.0 + Spring Boot + DataGrip

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

### 4. 启动并验证

- DataGrip：连接 `127.0.0.1:3306`，数据库 `cxnp`
- IDEA：运行 `CxnpApplication`
- 终端：验证接口

```bash
curl http://localhost:8080/api/users
```

---

## 二、日常开发启动（每天执行）

**第一步：** 打开 Docker Desktop。

**第二步：** 启动数据库。

```bash
docker start cxnp-mysql
```

**第三步：** IDEA 运行 `CxnpApplication`。

**第四步：** 验证后端接口。

```bash
curl http://localhost:8080/api/users
```

DataGrip 只在需要查看或修改数据库时打开。

---

## 三、日常停止

```bash
# 1. IDEA 点击 Stop，停止 Spring Boot

# 2. 按需停止数据库
docker stop cxnp-mysql
```

---

## 四、常见问题

| 问题 | 解决方法 |
|---|---|
| 3306 端口被占用 | 检查占用进程，若为本机 Oracle MySQL，执行 `sudo launchctl bootout system/com.oracle.oss.mysql.mysqld` |
| Docker 无法连接 | 打开 Docker Desktop |
| MySQL 没启动 | `docker start cxnp-mysql` |
| 查看数据库日志 | `docker logs --tail 50 cxnp-mysql` |
| 后端数据库连接失败 | 检查 MySQL 状态及 `DB_PASSWORD` |
| 8080 端口被占用 | `lsof -nP -iTCP:8080 -sTCP:LISTEN` |

---

## 五、注意事项

1. 本地开发统一使用 Docker MySQL。
2. 不要每天重新创建容器或数据库。
3. `docker stop` 不会删除数据库数据。
4. 本地 Docker MySQL 与腾讯云 MySQL 是独立环境，操作前确认连接目标。
5. 不要随意删除 MySQL 容器和数据卷。

**核心流程：Docker Desktop → MySQL → Spring Boot → API 验证。**