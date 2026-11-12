# Proposal

## Why

项目已完成首个中型 RBAC 数据权限变更并通过验收。后续需要一套可审查的需求、设计与任务流程，避免复杂变更在多轮协作中遗漏边界、验收场景或人工确认节点。

OpenSpec 已在项目中完成工具初始化；现在需要将其与现有 `AGENTS.md`、`current-task.md` 和 SQL 文件约定明确分工，形成可持续执行的协作架构。

## What Changes

- 为中型及以上变更采用仓库内 OpenSpec 变更包，使用 Proposal、Specs、Design 和 Tasks 管理可审查的变更计划。
- 在 `AGENTS.md` 中补充 OpenSpec 的长期使用边界、人工确认规则和文档职责，保留原有架构、分层、权限与数据库约束。
- 将 `current-task.md` 定位为当前活跃任务与人工节奏控制页；已验收任务不作为历史继续保留。
- 将 `openspec/config.yaml` 补充为面向本项目的协作上下文，并对 apply、archive 阶段设置“单任务执行、人工确认、显式验收”的操作指引。
- 明确后续新建初始化与测试清理脚本放入 `sql/`；保留 `database/` 中的既有文件，不在本变更中迁移或删除。
- 在项目文档中说明 OpenSpec、`AGENTS.md`、`current-task.md` 和 `sql/` 的职责与使用入口。

## Capabilities

### New Capabilities

无。本变更只调整开发协作工具和文档治理方式，不改变运行时业务行为。

### Modified Capabilities

无。

## Impact

- 新增仓库内 `openspec/` 目录及 Codex 的 OpenSpec 工作流能力。
- 修改协作与项目说明文档，不修改业务 API、数据库既有数据、业务代码或当前已验收的数据权限实现。
- 后续中型及以上变更将在开始实施前先创建并人工审核 OpenSpec 变更包。
