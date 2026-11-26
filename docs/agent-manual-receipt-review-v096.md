# v0.9.6 实际只读人工复核验收

Stone 领域逻辑、生产 API 和 MySQL V6 schema 不变；CI 固定检出 Agent 69695dc564ad3fb2ca1b012ee74f70fb3e18052a。
[实际 CI 45765488016](https://github.com/Rookiefreshman/Stone/actions/runs/45765488016)：MySQL 5.7/8.0 各45项 Java 测试、8组实际 HTTP 契约全部通过，0失败/错误/跳过。新增验证真实 Stone 返回401，维护方修复只读凭据后显式发起一次 GET，匹配原始请求指纹及操作回执，重复复核请求不再发起查询，保留审计。选定业务表计数与库存余额不变，Agent 业务 POST=0。
[Agent CI 45765456480](https://github.com/Rookiefreshman/stone-agent/actions/runs/45765456480)：325 passed, 1 warning，包含8项 PostgreSQL 集成。当前仅受控本地维护身份，非生产 IAM；不提供人工确认提交、真实写入、审批或真实发送意图。真实业务提交响应丢失、生产授权及目标环境存量迁移仍待验收。
[Agent 复核 SOP](https://github.com/Rookiefreshman/stone-agent/blob/agent/manual-receipt-review-v0.9.6/docs/MANUAL_RECEIPT_REVIEW_V096.md)
