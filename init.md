# Stone WMS Init Context

> 用途：后续 AI 或研发进入本项目时，优先读取本文件，而不是一次性读取全部文档。只有当任务涉及具体领域时，再按链接加载对应文档。

## 项目一句话

Stone WMS 是一个 Java 仓储管理系统样板，目标是从业务蓝图、架构设计、测试体系、AI Skill 流到可运行 MVP 逐步落地 WMS 核心能力。

## 当前边界

- 已实现：内存版入库、出库、库存查询、库存流水、核心单元测试、Flyway 兼容迁移脚本与迁移目录校验。
- 未实现：JDBC/JPA/MyBatis 数据库仓储运行时实现、认证授权、异步消息发布器、前端、设备集成、报表、生产级部署。
- 业务核心术语：SKU、库位 Location、库存 Inventory、入库 Receipt、出库 Shipment、库存流水 Movement。

## 代码结构

- `src/main/java/com/stone/wms/WmsApplication.java`：基于 Java 标准库 `HttpServer` 暴露 REST 风格接口。
- `src/main/java/com/stone/wms/dto`：请求/响应 DTO。
- `src/main/java/com/stone/wms/domain`：领域对象、枚举。
- `src/main/java/com/stone/wms/repository`：当前为内存仓储，后续新增 JDBC/JPA/MyBatis 实现。
- `src/main/java/com/stone/wms/migration`：迁移目录加载与命名校验。
- `src/main/resources/db/migration`：Flyway 兼容数据库迁移 SQL。
- `src/main/java/com/stone/wms/service`：应用服务与业务编排。
- `src/test/java/com/stone/wms/service`：核心业务单元测试。

## 研发约束

1. 先补测试，再改业务规则。
2. HTTP 入口只做协议转换，业务规则放 Service/Domain。
3. 金额、数量、库存等关键字段不能使用浮点数；库存数量当前使用 `int`，未来可演进为批次维度的 `BigDecimal`。
4. 入库、出库必须产生 `StockMovement`，便于审计与追踪。
5. 后续接入数据库时，应使用 `src/main/resources/db/migration` 中的 Flyway 兼容脚本管理 schema，禁止运行时自动改表。
6. 数据库仓储必须保证库存余额更新、库存流水、Outbox 事件处于同一事务。

## 任务路由

- 业务梳理：读取 `docs/01-business-blueprint.md`。
- 技术架构：读取 `docs/02-technical-architecture.md`。
- 测试设计：读取 `docs/03-test-strategy.md`。
- AI/Skill/token 优化：读取 `docs/04-skill-flow-and-token-optimization.md`。
- 持久化/迁移：读取 `docs/05-persistence-and-migration-roadmap.md` 和 `docs/adr/0001-persistence-migration-strategy.md`。

## 推荐提示词

```text
请先阅读 init.md，只在必要时读取相关 docs。保持现有分层架构，修改业务规则前补充单元测试，并在回答中说明影响的业务流程、接口与测试。
```
