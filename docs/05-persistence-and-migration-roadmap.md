# 持久化与数据库迁移设计

## 1. 设计目标

下一阶段从内存 MVP 演进到可恢复、可审计、可灰度升级的数据库版本。目标不是一次性引入全部生产能力，而是先建立稳定的数据边界、迁移规范和仓储替换路径。

## 2. 技术选型建议

| 能力 | 建议 | 原因 |
| --- | --- | --- |
| 主数据库 | PostgreSQL 优先，MySQL 兼容预留 | PostgreSQL 对约束、事务、JSON、索引表达力更强，WMS 库存一致性依赖数据库约束 |
| 迁移工具 | Flyway 兼容 SQL 目录 | SQL 可审查、可回放、易接入 CI/CD；当前仓库先提供 Flyway 命名规范脚本 |
| Java 访问层 | V1 可用 JDBC/JdbcTemplate，V2 再评估 JPA/MyBatis | 库存扣减 SQL 需要强控制，先避免 ORM 掩盖锁与并发语义 |
| 并发控制 | 乐观锁 + 条件更新，热点 SKU 可升级行级锁 | 防止超卖，同时保留吞吐量 |
| 事件一致性 | Outbox 表 | 库存变更与事件发布同事务提交，避免双写不一致 |

## 3. 迁移目录规范

迁移脚本放在 `src/main/resources/db/migration`，采用 Flyway 兼容命名：

```text
V{version}__{description}.sql
```

示例：

- `V1__init_wms_core.sql`：创建仓库、库位、SKU、库存余额、库存流水、Outbox 表。
- `V2__inventory_query_indexes.sql`：创建库存查询和流水追踪索引。

约束：

1. 版本号只能递增，禁止重写已发布脚本。
2. 表结构变更必须和代码仓储变更在同一个 PR 中提交。
3. 危险 DDL（删列、改类型、重命名）必须先写 ADR 和回滚方案。
4. 上线流程应先执行迁移，再发布依赖新字段的应用版本。

## 4. 核心表设计

### `inventory_balance`

库存余额表是扣减库存的关键表。唯一键建议覆盖：租户、仓库、SKU、库位、批次、库存状态。当前 MVP 暂不实现租户和批次逻辑，但 schema 先预留字段，避免后续大迁移。

关键字段：

- `quantity`：当前余额，不允许小于 0。
- `allocated_quantity`：已占用数量，出库波次引入后使用。
- `version`：乐观锁版本。
- `updated_at`：库存更新时间。

### `stock_movement`

库存流水表只增不改，用于审计。每次入库、出库、调整、盘点差异都必须写入。业务查询不应依赖流水汇总实时余额，实时余额以 `inventory_balance` 为准。

### `outbox_event`

库存事件先写入 Outbox，再由后台发布到 MQ/Webhook。这样可以保证库存变更和事件记录同事务提交。

## 5. 仓储替换路径

当前代码已经通过 `InventoryRepository` 与 `StockMovementRepository` 隔离存储实现。下一阶段建议新增：

```text
repository
├── InventoryRepository.java          # 保持接口
├── StockMovementRepository.java      # 保持接口
└── jdbc
    ├── JdbcInventoryRepository.java  # 数据库实现
    └── JdbcStockMovementRepository.java
```

切换策略：

1. 保留内存仓储用于单元测试。
2. JDBC 仓储用于集成测试和运行时。
3. Service 层不直接接触 SQL。
4. 数据库实现必须在同一事务内完成库存余额更新和流水写入。

## 6. 出库扣减 SQL 策略

数据库阶段不能使用“先查再扣”的无锁流程。推荐条件更新：

```sql
UPDATE inventory_balance
SET quantity = quantity - :picked,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id
  AND quantity >= :picked
  AND version = :version;
```

当更新行数为 0 时，说明库存不足或并发冲突，需要重试或返回库存不足。

## 7. Skill 流接入

持久化相关任务应使用“持久化迁移 Skill”：

1. 读取 `init.md` 和本文件。
2. 明确新增/变更的业务字段与状态。
3. 先写迁移脚本，再写仓储测试。
4. 实现 JDBC/JPA/MyBatis 仓储适配。
5. 使用 Testcontainers 或真实测试库运行迁移和集成测试。
6. 更新 `init.md`、架构文档和 API 示例。

## 8. 验收标准

- 迁移脚本命名合法且版本递增。
- 空库执行迁移后可以启动应用。
- 入库、出库、查询用例在数据库仓储上通过。
- 并发出库不会导致负库存。
- 库存变更必然产生 `stock_movement` 和可选 `outbox_event`。
