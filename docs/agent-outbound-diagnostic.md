# 出库分配只读诊断 v0.7

GET `/api/wms/warehouses/{warehouseId}/outbound-orders/{orderId}/allocation-diagnostic`。基于 V1–V4 现有表，单条 SELECT 在 REPEATABLE_READ 只读事务中返回指定仓库的订单、精确 AVAILABLE 库存维度、库位状态与成功回执存在标记。不返回幂等键。最多 200 行，超限返回 422，不截断后宣称可分配。库存为空与数量为零不同；未配置库位不等于已冻结。按 inventoryId 合并相同库存记录的剩余需求（MySQL 比较规则决定匹配）。历史失败原因、并发冲突、实物库存不在此快照保证范围；不能替代写事务校验。

默认关闭。仅开发环境配置 `stone.agent.outbound-query.enabled=true`、`stone.agent.outbound-query.token`（至少24字符）、`stone.agent.outbound-query.warehouses`（逗号分隔）。禁止提交实际 token。生产需要 Stone 全接口企业级鉴权和数据域检查。Maven 编译、Java 单测、真实 MySQL 和跨服务验收待完成，不允许作为写操作授权。
