# Stone WMS

Stone WMS 是一个面向仓储管理系统（Warehouse Management System, WMS）的 Java 技术栈落地样板项目。当前仓库从空项目初始化为“业务蓝图 + 技术架构 + 可运行代码 + 测试策略 + Skill/Init token 优化流”的完整起点。

## 目标

- 覆盖 WMS 核心域：商品、库位、库存、入库、出库、盘点、补货、波次、作业、追踪。
- 覆盖 Java 主流核心技术：Java 标准库 HTTP API、REST 风格接口、分层架构、DDD 聚合、单元测试、集成测试预留、CI/CD 预留、可观测性预留。
- 为后续引入 MySQL/PostgreSQL、Redis、MQ、Elasticsearch、工作流、规则引擎、权限体系、报表分析保留清晰演进路径。
- 用 `init.md` 与 Skill 流降低长上下文项目中的 token 消耗，让后续需求能够按固定模板快速对齐。

## 快速开始

```bash
mkdir -p target/classes target/test-classes
javac --release 21 -encoding UTF-8 -d target/classes $(find src/main/java -name '*.java')
javac --release 21 -encoding UTF-8 -cp target/classes -d target/test-classes $(find src/test/java -name '*.java')
java -cp target/classes:target/test-classes com.stone.wms.service.WarehouseServiceTest
java -cp target/classes com.stone.wms.WmsApplication
```

示例接口：

```bash
curl -X POST http://localhost:8080/api/v1/warehouse/receive \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-001","location":"A-01-01","quantity":10,"referenceNo":"ASN-10001"}'

curl -X POST http://localhost:8080/api/v1/warehouse/ship \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-001","quantity":3,"referenceNo":"SO-20001"}'

curl http://localhost:8080/api/v1/inventory/SKU-001
```

## 文档地图

- [`init.md`](init.md)：给 AI/研发进入项目时读取的最小上下文。
- [`docs/01-business-blueprint.md`](docs/01-business-blueprint.md)：WMS 业务域、流程、边界与 MVP 范围。
- [`docs/02-technical-architecture.md`](docs/02-technical-architecture.md)：Java 技术架构、模块划分、数据/集成/部署演进。
- [`docs/03-test-strategy.md`](docs/03-test-strategy.md)：测试金字塔、关键用例、验收标准。
- [`docs/04-skill-flow-and-token-optimization.md`](docs/04-skill-flow-and-token-optimization.md)：Skill 流、需求模板、上下文压缩与 init 策略。

## 当前代码能力

当前实现的是基于 Java 标准库的内存版 WMS 核心库存服务，适合作为业务规则与 API 契约的最小闭环：

- 入库：按 SKU + 库位增加库存，并记录库存流水。
- 出库：按 FIFO 风格从多个库位扣减库存，并校验库存不足。
- 查询：按 SKU 汇总总量、库位明细与库存流水。

下一阶段建议先引入持久化与数据库迁移，再扩展为多模块服务。
