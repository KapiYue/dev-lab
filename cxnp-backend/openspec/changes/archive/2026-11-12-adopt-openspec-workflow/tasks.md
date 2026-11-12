# Tasks

## 1. 固化协作边界

- [x] 1.1 更新 `AGENTS.md`，定义 OpenSpec、`current-task.md` 与长期项目规则的职责、适用范围和人工审批边界；通过人工检查确认它不记录具体任务或历史。
- [x] 1.2 更新 `openspec/config.yaml` 的项目上下文、artifact 规则和 apply/archive 操作指引，明确“计划先审阅、每次只实施一项、明确确认后继续、明确验收后归档”；通过 `openspec context --json` 验证配置可被 CLI 读取。

## 2. 对齐项目入口与当前任务

- [x] 2.1 更新根目录 README 与架构文档，说明 `openspec/`、`sql/`、`database/` 的目录职责和 OpenSpec 使用入口；通过文档链接与目录引用人工检查确认无误。
- [x] 2.2 在已验收 RBAC 数据权限任务不保留历史的前提下，将 `current-task.md` 切换为本 OpenSpec 接入任务，记录当前 change、人工确认规则和本次完成标准；通过人工检查确认文件只描述当前活跃任务。

## 3. 验证工具集成

- [x] 3.1 验证 OpenSpec 根目录、Codex 的六个核心工作流文件及 `adopt-openspec-workflow` 变更状态；运行 `openspec list --json`、`openspec status --change adopt-openspec-workflow --json` 和文件清单检查。

## Workflow follow-up

- 全部任务完成并由用户明确验收后，才可同步或归档本变更。
