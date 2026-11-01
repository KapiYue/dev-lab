# 当前任务：党员详情查询

## 目标

在已有党员列表查询的基础上，实现党员详情查询接口，跑通以下请求链路：

请求 → Controller → Service → Mapper → MyBatis XML → MySQL → JSON

## 待办

- [ ] 在 `PartyMemberMapper` 中增加按 ID 查询方法。
- [ ] 在 `PartyMemberMapper.xml` 中增加按 ID 查询 SQL。
- [ ] 在 `PartyMemberService` 中增加详情查询业务方法。
- [ ] 在 `PartyMemberController` 中增加 `GET /api/party-members/{id}` 接口。
- [ ] 处理党员不存在场景，返回统一错误 JSON。
- [ ] 增加必要的参数校验，处理非法 ID 参数。
- [ ] 验证正常查询场景。
- [ ] 验证 ID 不存在场景。
- [ ] 验证非法 ID 参数场景。

## 完成标准

- [ ] 可以通过 ID 查询党员详情。
- [ ] 查询链路完整：Controller → Service → Mapper → MyBatis → MySQL。
- [ ] 正常查询返回统一 JSON 格式。
- [ ] 党员不存在时返回明确的统一错误 JSON。
- [ ] 非法 ID 参数不会进入数据库查询，并返回统一错误 JSON。

## 上一任务完成记录

党员列表基础查询已完成并人工确认通过：

- `mvn test` 已通过。
- 已验证有数据、空数据和统一 JSON 返回。
- `party_member` 表及至少 3 条党员初始数据已准备完成。

## 备注

- 当前仍采用单 Maven 工程，不拆分 Maven Module。
- 完成详情查询后，再规划党员新增、修改或删除等后续任务。
