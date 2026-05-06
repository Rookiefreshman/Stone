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
