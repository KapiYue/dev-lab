# CXNP Backend

CXNP Backend 是一个基于 Spring Boot、MyBatis 和 MySQL 的党建业务学习项目，当前以党员管理和 RBAC 权限管理为核心。

## 项目结构

```text
src/main/java/              Java 业务代码
src/main/resources/         应用配置和 MyBatis XML
database/                   数据库脚本
docs/                       可对外共享的项目文档
internal/                   本地和部署环境专用资料，不提交 Git
AGENTS.md                   内部协作规范
current-task.md             内部当前任务
```

## 技术栈

- Java 17
- Spring Boot 3.3.5
- MyBatis XML
- MySQL 8
- Maven
- Lombok

## 本地启动

1. 准备 Java 17、Maven 和 MySQL 8。
2. 创建 `cxnp` 数据库，并按需执行 `database/` 下的脚本。
3. 通过环境变量配置数据库连接：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。
4. 执行：

   ```bash
   mvn spring-boot:run
   ```

默认使用 `dev` profile，服务端口为 `8080`。

## 文档

- [API 文档](docs/api/README.md)
- [部署说明](docs/deployment/README.md)
- [架构说明](docs/architecture/README.md)

内部协作规范和当前任务不属于对外文档，分别见 `AGENTS.md` 和 `current-task.md`。

## 配置安全

本地数据库密码、线上连接信息、证书和其他运行环境资料统一放在未被 Git 跟踪的 `internal/` 目录，或通过环境变量注入。仓库只保留非敏感的配置说明和示例。

