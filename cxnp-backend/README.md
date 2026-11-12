# CXNP Backend

CXNP Backend 是一个基于 Spring Boot、MyBatis 和 MySQL 的党建业务学习项目，当前以党员管理和 RBAC 权限管理为核心。

## 项目结构

```text
src/main/java/              Java 业务代码
src/main/resources/         应用配置和 MyBatis XML
sql/                        新增初始化、测试初始化和测试清理脚本
database/                   保留的历史数据库脚本
openspec/                   中大型变更的需求、设计和任务工件
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
2. 创建 `cxnp` 数据库，并按需执行 `database/` 中的历史建表与基础数据脚本，以及当前任务对应的 `sql/` 脚本。
3. 通过环境变量配置数据库连接：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。

### 本地启动（macOS + Docker）

Cursor 或终端重启后，在项目根目录执行：

```bash
./scripts/dev.sh
```

脚本会启动已有的 `cxnp-mysql` 和 `cxnp-redis` 容器，自动读取 MySQL 容器自己的密码到当前进程环境，并使用 Java 17、`dev` Profile 启动 Spring Boot。密码不会写入脚本、日志或 Git。

运行数据库测试：

```bash
./scripts/test.sh UserRoleServiceTest
```
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

## 变更协作

中型及以上变更使用 OpenSpec 管理需求、设计、任务和验收。OpenSpec 工件位于 `openspec/`：未完成变更位于 `openspec/changes/`，已确认的稳定业务规格位于 `openspec/specs/`。

在 Codex 中，可从 Skills 选择 `openspec-propose`，或使用 `$openspec-propose` 创建变更规划。规划经人工确认后，每次只执行一个任务；该任务的实现与验证完成后停止，等待下一次明确确认。全部任务通过验收并获得明确许可后，才可归档。

`AGENTS.md` 记录长期工程规则，`current-task.md` 仅记录当前活跃任务和人工确认状态；两者不替代 OpenSpec 的变更工件。

## 配置安全

本地数据库密码、线上连接信息、证书和其他运行环境资料统一放在未被 Git 跟踪的 `internal/` 目录，或通过环境变量注入。仓库只保留非敏感的配置说明和示例。

