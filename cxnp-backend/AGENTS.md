# CXNP Backend 协作规范

## 项目定位

本项目是基于 Spring Boot 和 MyBatis 的党建业务学习项目，当前以党员管理和 RBAC 权限管理为核心。

## 当前工程形态

当前采用单 Maven 工程、按业务模块分包的单体结构：

- `party`：党员业务。
- `rbac`：用户、角色、功能权限与数据权限。
- `organization`：党组织和支部业务。
- `common`：统一响应、异常、常量与通用工具。
- `config`：框架配置。

## 技术栈

- Java 17
- Spring Boot 3.3.5
- MyBatis XML
- MySQL 8
- Maven
- Lombok

## 分层规范

- Controller：接口入口、参数校验、响应封装；不得直接调用 Mapper。
- Service：业务规则、事务控制、数据权限二次校验。
- Mapper：数据库访问接口。
- Mapper XML：动态 SQL、多表关联 SQL。
- Entity：数据库实体。
- DTO：接口请求对象。
- VO：接口响应对象。

## 权限规范

- 功能权限统一使用 `permissionKey`，例如 `party:member:list`。
- API 功能权限与业务数据权限分层处理。
- 数据权限不能依赖前端传入的组织或支部范围。
- 用户—角色、角色—权限、角色—数据范围均通过关联表建模。
- 重要绑定关系需由 Service 前置校验与数据库唯一约束双重保证。

## 数据库规范

- 表名、字段名使用 `snake_case`；Java 属性使用 `camelCase`。
- 中间表增加必要的唯一约束和索引。
- 初始化数据与测试清理脚本统一放在 `sql` 目录。
- 复杂查询优先通过 MyBatis XML 管理。

## 验证规范

- 每个接口至少验证：正常结果、空结果、非法参数。
- 权限接口至少验证：有权限、无功能权限、无数据范围。
- 涉及多表写入时，验证事务回滚。
- 涉及重复绑定时，验证 Service 校验和数据库约束。

## 文档职责

- `AGENTS.md` 只记录项目定位、长期技术约束、协作规范和文档职责，不记录具体开发任务、任务进度或历史任务。
- `current-task.md` 只记录当前开发任务，包括任务目标、当前状态、待办事项、完成标准和必要的当前任务上下文。
- 不在 `current-task.md` 中维护已完成任务历史、长期工程规划或与当前任务无关的备注。
- 未经用户明确确认验收，不得将当前任务标记为“已完成（验收通过）”。

## 后续工程演进

当前不拆分 Maven Module。

仅在模块需要独立复用、多人独立维护或构建隔离时，演进为：

- `cxnp-common`
- `cxnp-rbac`
- `cxnp-party`
- `cxnp-organization`
- `cxnp-app`

多 Maven Module 不等同于微服务；在保留单一启动模块时，系统仍是单体应用。
