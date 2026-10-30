# 架构说明

CXNP Backend 当前是单 Maven 工程、按业务模块分包的单体应用，不拆分为多个 Maven Module，也不作为微服务部署。

## 分层

- Controller：接口入口、参数校验和响应封装
- Service：业务规则、事务控制和数据权限校验
- Mapper：数据库访问接口
- Mapper XML：动态 SQL 和复杂查询
- Entity：数据库实体
- DTO：请求对象
- VO：响应对象

## 业务模块

- `party`：党员业务
- `rbac`：用户、角色、功能权限和数据权限
- `organization`：党组织和支部业务
- `common`：统一响应、异常、常量和通用工具
- `config`：框架配置

