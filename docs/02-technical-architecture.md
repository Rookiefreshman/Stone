# 技术架构设计

## 1. 架构原则

- 业务优先：所有技术选型服务于库存准确、作业效率、异常可追踪。
- 分层清晰：HTTP 入口做协议转换，Service 做业务编排，Domain 表达规则，Repository 隔离存储。
- 可替换：当前内存仓储可替换为 JPA/MyBatis，不影响 API 与核心服务。
- 可测试：领域规则必须能脱离 Web 容器单测。
- 可演进：从单体模块化起步，按业务边界拆分微服务。

## 2. 当前分层

```text
api -> service -> repository
        |
      domain
```

- `WmsApplication`：当前使用 Java 标准库 `HttpServer` 暴露 REST 风格接口；后续可迁移到 Spring MVC/WebFlux。
- `dto`：外部 API 契约。
- `service`：入库、出库、库存查询用例。
- `domain`：库存对象与流水。
- `repository`：仓储接口与内存实现；下一阶段新增 JDBC/JPA/MyBatis 实现。
- `migration`：迁移脚本目录加载和命名校验。
- `src/main/resources/db/migration`：Flyway 兼容 SQL 迁移脚本。
- `exception`：业务异常与全局异常处理。

## 3. Java 核心技术栈规划

| 类别 | 推荐技术 | 落地阶段 |
| --- | --- | --- |
| Web | Java HttpServer（当前 MVP），Spring Boot MVC / WebFlux（后续） | V0/V1 |
| 校验 | Jakarta Bean Validation | V0 |
| 持久化 | Spring Data JPA 或 MyBatis Plus | V1 |
| 数据库 | PostgreSQL/MySQL | V1 |
| 迁移 | Flyway/Liquibase | V1 |
| 缓存 | Redis | V2 |
| 消息 | Kafka/RabbitMQ/RocketMQ | V3 |
| 搜索 | Elasticsearch/OpenSearch | V3 |
| 安全 | Spring Security + OAuth2/JWT | V1 |
| 文档 | springdoc-openapi | V1 |
| 测试 | JUnit 5, AssertJ, Testcontainers | V0/V1 |
| 构建 | Maven | V0 |
| 可观测性 | Micrometer, Prometheus, OpenTelemetry | V2 |
| 部署 | Docker, Kubernetes, Helm | V2 |

## 4. 目标模块划分

未来可演进为 Maven 多模块：

```text
wms-parent
├── wms-api          # API 契约与 HTTP 入口
├── wms-application  # 用例编排、事务、权限上下文
├── wms-domain       # 聚合、领域服务、领域事件
├── wms-infra        # DB/MQ/缓存/外部系统适配
└── wms-test         # 测试夹具、契约测试、容器测试
```

## 5. 数据模型建议

当前已提供 `V1__init_wms_core.sql` 与 `V2__inventory_query_indexes.sql` 作为下一阶段数据库落地基线。核心表：

- `sku`：商品主数据。
- `warehouse` / `zone` / `location`：仓库空间模型。
- `inventory_balance`：库存余额，唯一键建议为 `tenant_id + warehouse_id + sku_id + location_id + lot_no + status`。
- `stock_movement`：库存流水，只增不改。
- `receipt_order` / `receipt_line`：入库单。
- `shipment_order` / `shipment_line`：出库单。
- `warehouse_task`：作业任务。

## 6. 集成架构

- 同步 REST：适合主数据查询、单据创建、库存查询。
- 异步事件：适合库存变更、单据状态回传、设备事件。
- Outbox：业务写库与事件发布必须具备最终一致性保障，当前 schema 已预留 `outbox_event`。
- 幂等键：外部单据号 + 业务类型 + 行号，避免重复收货/出库。

## 7. 非功能需求

- 性能：核心库存查询 p95 < 100ms，出入库写入 p95 < 300ms。
- 一致性：库存扣减必须在事务中完成，数据库阶段使用乐观锁或行锁。
- 审计：库存流水不可物理删除。
- 安全：按仓库、货主、角色隔离权限。
- 可观测：接口、任务、库存扣减、外部回调均需要 traceId。
