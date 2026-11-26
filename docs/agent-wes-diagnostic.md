# WES 调度诊断 v0.8

GET `/api/wes/warehouses/{warehouseId}/dispatch-diagnostic?deviceCode=station-01`。单条非锁定聚合 SELECT，在 REPEATABLE_READ 只读事务中读取已登记仓库和设备配置、READY 数量、仓/设备 IN_FLIGHT 与 UNKNOWN 数量。终态不占槽；未知设备返回 `404 DISPATCH_SCOPE_NOT_FOUND`，不是空队列。复用 V2 表及现有索引，不增加表；需实测执行计划及大仓负载。

Stone claim 新领取上限为 `max(0,min(requestedLimit,maxBatch,maxActive-inFlight-unknown,deviceMaxActive-deviceInFlight-deviceUnknown,ready))`。maxReady 只限制入队，不限制领取。诊断不是实际领取，不包含旧 claimKey 重放，不持有仓容量行锁，不得据此控制设备或自动释放 UNKNOWN。当前容量不代表硬件健康或历史故障根因。

默认关闭。测试环境设置 `stone.agent.dispatch-query.enabled=true`、`stone.agent.dispatch-query.token`（至少24字符）及 `stone.agent.dispatch-query.warehouses`（逗号分隔）。不提交实际凭据。当前只支持开发服务身份和仓库范围，生产需正式鉴权与数据域。Maven 编译、Java 单测、真实 MySQL 与跨服务联调待验证。
