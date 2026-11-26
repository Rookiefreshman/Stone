# v0.9.1 取消操作回执（内部实现，写入门禁仍关闭）

V5 新增 wes_cancel_operation_receipt。操作键作用域为 warehouse_id + operation_id，单次取消事务只记录 COMMITTED；缺失不是 NOT_COMMITTED。recordedAt 是事务内写入记录的 UTC 时间，不是数据库精确提交时间。旧取消接口不会产生新回执，不能把已有 CANCELLED 状态认作本次操作提交。

内部 CancelOperationService.submit 复用已有锁仓与领域取消服务，同一 READ_COMMITTED 事务写入回执，失败回滚。调用前和锁仓内必须通过受信 CancelAuthorization；默认无实现且拒绝。没有新增 POST Controller 或生产授权实现。后续必须验证 tenant、真实主体、仓权限、审批有效期和一次性审批消费；重放也需当前授权。旧取消接口的生产访问治理仍是上线前置条件。

指纹：固定协议 CANCEL_READY_V1，依次 UTF-8 4 字节大端长度 + 字节：协议、operationId、warehouseId、taskId、tenant、actor、taskKey、workType、skuId、lotNo、sourceId（null 为空串）、destinationId、quantity、reason、approvalHash；SHA-256 小写十六进制。Java/Python 黄金向量见测试。任务 ID/任务键及公开作业维度需匹配 READY，不能代替服务端审批或业务版本策略。

独立只读 GET /api/wms/warehouses/{warehouseId}/cancel-operation-receipts/{operationId}，默认关闭；仅隔离测试环境设置 stone.agent.cancel-receipt-query.enabled=true、token（至少24字符）、warehouses。开发 token 不代表生产企业身份。禁止从此 GET 派发。回执缺失 404 OPERATION_RECEIPT_NOT_FOUND；任何缺失、传输异常、身份或指纹不一致均保持 UNKNOWN，不重派。回执存在且匹配才确认 COMMITTED。Maven、V5 MySQL 原子提交/回滚、并发、真实权限均未验收。
