# v0.9.5 Agent 实际回执核对恢复验收

本版 Stone 领域代码、API、V6 schema 均不变。仅将双 MySQL CI 固定检出的 Agent 客户端升级到 9a575f192e41d6ee91072339c4eee709a373ce08。

[实际验收 CI 45763988260](https://github.com/Rookiefreshman/Stone/actions/runs/45763988260)：MySQL 5.7 和 8.0 各45项 Java 测试、7组实际 HTTP 契约均通过。新增检查：根据独立原始请求参数计算指纹，读取 Java 集成测试产生的实际取消回执，GET 返回后注入异常，模拟租约到期，新 Python 进程重新 GET 并持久化 RECEIPT_MATCHED。查询前后选定库存余额及记录计数相同，Agent 业务 POST 为0。Agent 的独立 PostgreSQL CI 317项通过（7项 PG 集成）。

这是隔离测试库上的只读结果丢失恢复，不是实际取消提交后响应丢失。Agent 参考由维护人员独立提供，不构成可信发送记录或审批；真实企业 IAM、生产审批、旧写接口治理、实际发送与对账工作流、生产数据库迁移均待验收。生产 Agent 写入继续关闭。

[Agent 核对与恢复 SOP](https://github.com/Rookiefreshman/stone-agent/blob/agent/receipt-recovery-v0.9.5/docs/RECEIPT_RECOVERY_V095.md)
