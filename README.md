# Stone WMS

这是一个用 AI Coding 工具自动化生成的 WMS 业务系统示例。

当前代码按照 `Codex.md` 中的 Java WMS 约束实现：

- Java 8 + Spring Boot 2.7。
- DDD 分层：domain / application / infrastructure / interfaces / startup。
- MyBatis-Plus / MyBatis XML 访问 MySQL 5.7+。
- 库存聚合控制数量一致性，出库单聚合控制状态机。
- Controller 只调用 Application Service，Application 只调用 Domain Repository，Infrastructure 负责 Entity / Mapper / Converter / Repository 实现。

## 验证

```bash
mvn test
```

## WMS + WES + WCS 设计

- [系统概要设计](docs/architecture.md)
- [模块详细设计与流程图](docs/modules.md)
- [数据库设计与参考建表 SQL](docs/database.md)
- [HTTP 接口设计](docs/api.md)

[高并发、幂等、任务容量设计与 MySQL 复验](docs/concurrency-dispatch.md)

[入库、移库、库位冻结与库存流水](docs/stock-work-execution.md)

## Agent 业务验收（v0.9.3）

[Java/MySQL 双版本及实际 HTTP 跨服务验收](docs/agent-business-validation-v093.md)。使用独立 CI 测试库，禁止连接生产库；生产写入门禁仍关闭。

## Agent 可信审批与唯一消费（v0.9.4）

[审批绑定与事务内唯一消费](docs/agent-approval-consumption-v094.md)。V6 复用取消回执表，双 MySQL CI 各45项通过；真实授权适配器尚未接入，不开放 Agent 业务写入。
