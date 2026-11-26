# READY 库存作业取消：v0.9 业务放行契约（未开放 Agent 写入）

## 已有实现
`DispatchApplicationService.cancelStockWork` 在 READ_COMMITTED 事务中先锁仓容量，按仓库和任务读取库存作业；READY 才允许取消，CANCELLED 重放直接返回当前任务。StockWorkService.finish 锁库位，TRANSFER 释放源预占；PUTAWAY 通过终态不再计入承诺收货量。状态更新条件为原状态。claim 同样先锁仓，因此取消与领取必须串行。现有 GET 详情可作状态预检，但不是锁定、授权或操作回执。

## 本版新增
仅新增专用 MySQL 集成测试源码（竞争领取与取消、重复取消及跨仓隔离）。使用独立迁移测试库，不连接实际生产仓储。Maven/MySQL 尚未运行。未新增或修改任何写接口、业务事务或表。

## 下一版写入放行条件（未实现）
1. Stone 端验证企业身份、实际操作者/服务身份、仓数据域及取消权限，禁止将设备凭据当用户身份。审批绑定参数、任务身份、原因、证据、有效期与操作 ID；不能用开发 token 代替生产授权。
2. 候选新 POST 契约接收稳定 operation_id、仓库/任务、期望状态和业务身份指纹、原因；在锁仓事务内复查权限、审批及任务。状态变更/库存释放/操作回执须同一事务；相同 operation_id 同 payload 返回原回执，不同 payload 拒绝；不能仅通过 CANCELLED 状态推断该请求提交。
3. 只读回执查询按授权域及 operation_id 返回 COMMITTED/NOT_COMMITTED/UNKNOWN，含操作指纹、实际业务状态、提交时间；不存在回执不等于未提交。对账维护身份不得持有业务写权限；排除旧在途请求前不重派。
4. Agent 端写入账本、审批消耗、派发前复查、幂等、UNKNOWN 对账与恢复经过真实故障注入、并发和数据库验收之后才放行。v0.9 仅支持预检和合成数据审批演练，永远不派发。

测试：按 Stone 原 README 初始化 V1–V4 的一次性 MySQL；设置 STONE_TEST_MYSQL_URL/USER/PASSWORD 后执行 mvn -Dtest=DispatchMySqlTest test。测试使用独立 ID 段 99001–99003，不要在生产运行。
