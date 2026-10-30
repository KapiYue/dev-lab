# 当前任务：党员列表基础查询

## 目标

完成党员列表的首个可运行接口，跑通以下请求链路：

请求 → Controller → Service → Mapper → MyBatis XML → MySQL → JSON

## 待办

- [ ] 建立 `party_member` 表。
- [ ] 准备至少 3 条党员初始数据。
- [ ] 创建 `PartyMember` Entity，完成数据库字段映射。
- [ ] 创建 `PartyMemberMapper` 接口。
- [ ] 创建 `PartyMemberMapper.xml`，实现无参列表查询 SQL。
- [ ] 创建 `PartyMemberService` 及实现类。
- [ ] 创建统一返回对象 `Result<T>`。
- [ ] 创建 `PartyMemberController`。
- [ ] 实现无参党员列表查询接口。
- [ ] 验证接口返回统一 JSON。
- [ ] 验证空数据场景。

## 完成标准

- [ ] 可以通过接口查询党员列表。
- [ ] 查询链路完整：Controller → Service → Mapper → MyBatis → MySQL。
- [ ] 返回统一 JSON 格式。
- [ ] 能够说明各层职责。

## 备注

- 项目基础已完成，本文件不重复记录已完成事项。
- 本任务完成后，再由下一次规划写入新的当前任务。
