# Codex 执行必要约束：Java WMS 版

本文档是本仓库每次由 Codex / AI Coding Agent 执行任务时必须遵守的约束。目标是在 Java 技术栈下，以清晰、可验证、可维护的方式建设 WMS（Warehouse Management System，仓储管理系统）。

## 1. Agent 工作原则

### 1.1 执行顺序

每次任务必须按以下顺序推进：

1. **Understand：理解需求**
   - 先确认用户目标：新增功能、修复缺陷、重构、性能优化、文档整理或代码审查。
   - 对影响范围不清晰的任务，先阅读上下文，不要直接编码。
2. **Inspect：检查现状**
   - 阅读相关模块、配置、数据库脚本、测试和既有实现。
   - 优先复用项目现有模式，不引入无关架构或依赖。
3. **Design：简短设计**
   - 非平凡变更必须先形成设计思路：涉及哪些层、哪些类、数据如何流转、事务边界在哪里。
   - 设计必须遵守本文档的 DDD 分层和转换规则。
4. **Implement：小步实现**
   - 每次只改与任务直接相关的文件。
   - 新增类后必须检查包路径、Spring Bean、Mapper、XML、配置、路由、测试是否需要同步注册。
5. **Verify：验证**
   - 修改后运行项目对应的编译、测试和静态检查命令。
   - 不能运行时必须说明环境原因，不得编造结果。
6. **Report：报告**
   - 最终回复使用简体中文，说明修改内容、验证命令、结果和遗留风险。

### 1.2 沟通风格

- 始终使用简体中文。
- 保持简洁，优先展示关键代码、结构和命令。
- 不隐藏失败，不伪造测试结果。
- 不做未经要求的大规模重构。
- 如果用户要求会破坏分层、事务或 Java 语言约束，必须先说明原因，再给替代方案。

## 2. 语言与技术栈

### 2.1 基础环境

- **语言**：Java 8+
- **构建工具**：Maven 或 Gradle，以仓库现状为准。
- **Web 框架**：Spring Boot / Spring MVC，以仓库现状为准。
- **ORM / DAO**：MyBatis-Plus 或 MyBatis。
- **数据库**：MySQL 5.7+；若用户提到 MySQL 5.8，按“兼容 MySQL 5.7 的内部版本或目标环境”处理。
- **日志**：SLF4J + Logback / Log4j2，以仓库现状为准。
- **测试**：JUnit 4/5、Mockito、AssertJ，以仓库现状为准。

### 2.2 Java 8 兼容约束

生产代码默认保持 Java 8 兼容，除非仓库明确配置了更高版本：

- 禁止使用 `record`、`sealed`、`var`、文本块、switch 表达式等 Java 9+ / 10+ / 14+ 特性。
- 可以使用 Lambda、Stream、Optional、CompletableFuture，但必须保持可读性。
- `Optional` 只允许作为返回值，不允许作为字段或方法参数。
- 时间类型优先使用 `java.time` 包。

## 3. DDD 分层架构

WMS 系统必须遵守严格分层，各层只能承担自己的职责。

```text
src/main/java/{base_package}/
├── domain/                 # 领域层：纯业务规则
│   ├── model/              # 领域模型 BO / Aggregate / Value Object
│   ├── param/              # 领域查询参数 / 业务参数
│   ├── repository/         # 仓储接口
│   └── service/            # 领域服务
│
├── application/            # 应用层：用例编排
│   ├── dto/                # DTO / Command / Query / Result
│   ├── service/            # 应用服务
│   ├── assembler/          # BO ↔ DTO 转换
│   └── scheduler/          # 定时任务
│
├── infrastructure/         # 基础设施层：数据库、缓存、外部系统
│   ├── entity/             # 数据库实体 PO / DO
│   ├── converter/          # PO ↔ BO 转换
│   ├── repository/         # Repository 实现
│   ├── mapper/             # MyBatis / MyBatis-Plus Mapper
│   ├── xml/                # MyBatis XML，若项目使用 XML
│   └── client/             # 第三方系统客户端
│
├── interfaces/             # 接口层：外部入口
│   ├── controller/         # REST Controller
│   ├── request/            # API Request
│   ├── response/           # API Response
│   └── facade/             # 外部系统门面，按需使用
│
└── startup/ 或 config/      # 启动、依赖装配、框架配置
```

### 3.1 Domain 层规则

Domain 层只表达业务概念和业务规则。

允许：

- 聚合根、实体、值对象。
- 领域服务。
- 仓储接口。
- 领域异常。
- 与框架无关的业务校验。

禁止：

- 依赖 Spring Web、Controller、Request、Response。
- 依赖 MyBatis、MyBatis-Plus、Mapper、Entity。
- 依赖 Redis、MQ、HTTP Client、第三方 SDK。
- 返回 DTO 或 API Response。

### 3.2 Application 层规则

Application 层负责业务用例编排和事务边界。

允许：

- 调用 Domain Model / Domain Service。
- 调用 Domain Repository 接口。
- DTO、Command、Query、Result。
- BO ↔ DTO 的 Assembler。
- 事务控制。

禁止：

- 写 SQL。
- 直接使用 Mapper。
- 直接操作 Infrastructure Entity。
- 编写复杂领域规则。

### 3.3 Infrastructure 层规则

Infrastructure 层负责技术实现。

允许：

- MyBatis / MyBatis-Plus Mapper。
- Entity / PO / DO。
- Repository 实现。
- PO ↔ BO Converter。
- Redis、MQ、外部 API Client。

禁止：

- 将 Entity 暴露给 Interfaces 或 Application。
- 返回 DTO 给 Controller。
- 写核心业务规则。

### 3.4 Interfaces 层规则

Interfaces 层负责处理外部输入输出。

允许：

- Controller。
- Request / Response。
- 参数校验。
- 调用 Application Service。

禁止：

- Controller 直接调用 Mapper。
- Controller 直接操作 Entity。
- Controller 写业务逻辑或事务。
- Controller 跨过 Application 调用 Infrastructure。

## 4. 数据转换规则

数据必须按以下方向转换，严禁跨层转换：

```text
数据库查询
  ↓
Entity / PO / DO
  ↓ Converter
Domain Model / BO
  ↓ Assembler
DTO / Response
  ↓
JSON
```

禁止示例：

```java
// 禁止：Controller 直接返回数据库实体
return warehouseEntity;

// 禁止：Application 直接使用 Mapper
WarehouseEntity entity = warehouseMapper.selectById(id);

// 禁止：Infrastructure 返回接口层 DTO
return new WarehouseResponse();
```

推荐示例：

```java
WarehouseEntity entity = warehouseMapper.selectById(id);
Warehouse warehouse = warehouseConverter.toDomain(entity);
WarehouseDto dto = warehouseAssembler.toDto(warehouse);
```

## 5. 命名与字段规范

### 5.1 Java 字段

Java 代码中统一使用 lowerCamelCase：

```java
private Long warehouseId;
private Long skuId;
private Integer availableQty;
```

### 5.2 数据库字段

MySQL 表字段统一使用 snake_case：

```sql
warehouse_id
sku_id
available_qty
```

### 5.3 JSON 字段

接口 JSON 默认使用 lowerCamelCase：

```json
{
  "warehouseId": 1,
  "skuId": 1001,
  "availableQty": 20
}
```

如果项目已有统一 Jackson 命名策略，必须遵守项目配置。

## 6. 错误处理规范

生产代码禁止：

```java
optional.get();
e.printStackTrace();
throw new RuntimeException(e);
System.out.println(error);
```

必须使用：

- 业务错误：业务异常，例如 `BusinessException`、`DomainException`。
- 接口错误：统一异常处理，例如 `@RestControllerAdvice`。
- 参数错误：Bean Validation，例如 `@NotNull`、`@NotBlank`、`@Min`。
- 基础设施错误：包装为可识别的基础设施异常或应用异常。

推荐：

```java
Inventory inventory = inventoryRepository.findBySkuId(skuId)
        .orElseThrow(() -> new BusinessException("库存不存在"));
```

## 7. 事务与并发

### 7.1 事务边界

- 事务应放在 Application Service 层。
- 单个业务用例一个事务边界。
- Domain 层不得感知事务。
- Controller 不得声明事务。

推荐：

```java
@Transactional(rollbackFor = Exception.class)
public InboundOrderDto receiveInboundOrder(ReceiveInboundCommand command) {
    // 编排入库验收用例
}
```

### 7.2 库存并发

WMS 中库存是强一致核心数据，必须明确并发策略：

- 扣减库存、释放库存、转移库存必须有数据库级保护。
- MySQL 5.7 下优先使用行锁、乐观锁版本号或唯一约束保证一致性。
- 涉及库存数量变更时，SQL 必须包含防负库存条件。
- 对幂等接口必须保存业务单号或幂等键。

推荐库存扣减条件：

```sql
UPDATE wms_inventory
SET available_qty = available_qty - #{qty},
    reserved_qty = reserved_qty + #{qty},
    version = version + 1
WHERE warehouse_id = #{warehouseId}
  AND sku_id = #{skuId}
  AND available_qty >= #{qty}
  AND version = #{version}
```

## 8. MyBatis / MyBatis-Plus 规范

### 8.1 Mapper 规则

- Mapper 只能位于 Infrastructure 层。
- Mapper 不得被 Controller 或 Application 直接调用。
- 复杂 SQL 优先使用 XML，简单 CRUD 可使用 MyBatis-Plus。
- XML 中字段必须显式列出，避免生产查询使用 `SELECT *`。

### 8.2 MySQL 5.7 兼容规则

- 避免使用 MySQL 8.0 专属语法，例如窗口函数、CTE、JSON_TABLE，除非任务明确允许。
- 索引设计必须适配查询条件。
- 金额、重量、体积使用 `DECIMAL`，不要使用 `FLOAT` / `DOUBLE`。
- 时间字段建议使用 `DATETIME`，并统一时区策略。
- 字符集建议使用 `utf8mb4`。

### 8.3 分页规则

- 后台列表可使用 MyBatis-Plus 分页插件或项目既有分页方案。
- 大数据翻页避免深分页，优先使用游标或业务键翻页。

## 9. 测试规范

- 单元测试放在 `src/test/java`。
- 领域模型测试优先不启动 Spring。
- Application Service 测试可以使用 Mockito mock Repository。
- Repository / Mapper 测试按项目现有集成测试方式执行。
- 新增库存、入库、出库、移库、盘点等核心逻辑时，必须覆盖正常路径和异常路径。

推荐覆盖：

- 库存不足。
- 重复提交。
- 状态机非法流转。
- 批次 / 库位不存在。
- 并发扣减失败。

## 10. WMS 系统重新设计

### 10.1 设计目标

本 WMS 面向中小型仓库与制造 / 电商 / 维修备件场景，核心目标是：

- 库存准确：账实一致、可追溯、可审计。
- 作业清晰：入库、上架、拣货、复核、出库、移库、盘点流程明确。
- 扩展可控：支持批次、库位、供应商、客户、业务单据扩展。
- 技术稳健：兼容 Java 8、MyBatis / MyBatis-Plus、MySQL 5.7+。

### 10.2 业务边界上下文

```text
WMS
├── 基础资料上下文 Master Data
├── 仓库布局上下文 Warehouse Layout
├── 商品与批次上下文 SKU & Lot
├── 库存上下文 Inventory
├── 入库上下文 Inbound
├── 出库上下文 Outbound
├── 库内作业上下文 Internal Operation
├── 盘点上下文 Stocktake
├── 报表上下文 Report
└── 系统上下文 System
```

### 10.3 核心模块

#### 10.3.1 基础资料 Master Data

职责：维护 WMS 运行所需基础数据。

核心对象：

- 货主 Owner。
- 仓库 Warehouse。
- 商品 SKU。
- 单位 Unit。
- 供应商 Supplier。
- 客户 Customer。
- 承运商 Carrier。

关键规则：

- SKU 编码在货主维度唯一。
- 仓库编码全局唯一。
- 停用的 SKU、供应商、库位不得参与新单据。

#### 10.3.2 仓库布局 Warehouse Layout

职责：描述仓库物理结构。

核心对象：

- Warehouse：仓库。
- Zone：库区。
- Location：库位。
- LocationType：收货区、存储区、拣货区、暂存区、残次区。

关键规则：

- 库位归属于库区，库区归属于仓库。
- 库位状态包括启用、停用、冻结。
- 冻结库位不可入库、出库或移库。

#### 10.3.3 库存 Inventory

职责：维护库存数量和库存状态，是系统核心聚合。

核心对象：

- Inventory：库存余额。
- InventoryLot：批次库存。
- InventoryTransaction：库存流水。
- InventoryHold：库存冻结。

库存数量模型：

```text
on_hand_qty    账面库存
available_qty  可用库存
reserved_qty   已分配库存
frozen_qty     冻结库存
in_transit_qty 在途库存
```

基本约束：

```text
on_hand_qty = available_qty + reserved_qty + frozen_qty
```

关键规则：

- 所有库存变更必须写库存流水。
- 库存余额表只保存当前状态，库存流水表保存审计历史。
- 扣减可用库存时必须防止负库存。
- 同一仓库、SKU、批次、库位、库存状态维度下只能有一条库存余额记录。

#### 10.3.4 入库 Inbound

职责：处理从外部进入仓库的货物。

支持单据：

- 采购入库单。
- 退货入库单。
- 调拨入库单。
- 其他入库单。

流程：

```text
创建入库单 → 到货登记 → 收货验收 → 质检可选 → 上架 → 完成
```

关键规则：

- 未审核入库单不可收货。
- 收货数量不可超过允许超收阈值。
- 质检不合格库存必须进入残次或冻结状态。
- 上架完成后增加库位库存并写库存流水。

#### 10.3.5 出库 Outbound

职责：处理仓库向外发出的货物。

支持单据：

- 销售出库单。
- 生产领料单。
- 调拨出库单。
- 其他出库单。

流程：

```text
创建出库单 → 审核 → 库存分配 → 生成拣货任务 → 拣货 → 复核 → 发运 → 完成
```

关键规则：

- 审核后才能分配库存。
- 分配库存时从 available_qty 转入 reserved_qty。
- 发运时扣减 on_hand_qty 和 reserved_qty。
- 已取消单据必须释放 reserved_qty。
- 出库单号必须支持幂等。

#### 10.3.6 库内作业 Internal Operation

职责：处理不改变仓库总库存但改变库位、状态或归属的作业。

支持作业：

- 移库。
- 补货。
- 库存冻结 / 解冻。
- 库存调整。
- 批次状态转换。

关键规则：

- 移库需要同时减少源库位库存并增加目标库位库存。
- 冻结库存从 available_qty 转入 frozen_qty。
- 解冻库存从 frozen_qty 转回 available_qty。
- 调整库存必须记录调整原因和审批人。

#### 10.3.7 盘点 Stocktake

职责：周期性校验账实一致。

流程：

```text
创建盘点计划 → 冻结盘点范围 → 生成盘点任务 → 录入实盘 → 差异确认 → 调整库存 → 完成
```

关键规则：

- 盘点范围冻结后，不允许普通出入库影响该范围。
- 盘盈盘亏必须生成库存调整流水。
- 差异确认需要审批。

#### 10.3.8 报表 Report

职责：提供运营分析视图。

报表：

- 实时库存报表。
- 库存流水报表。
- 库龄报表。
- 入库明细报表。
- 出库明细报表。
- 盘点差异报表。

规则：

- 报表不得反向承载业务逻辑。
- 大数据报表应使用异步导出。

### 10.4 推荐聚合设计

```text
Warehouse 聚合
├── Warehouse
├── Zone
└── Location

Sku 聚合
├── Sku
└── SkuPackage

Inventory 聚合
├── Inventory
├── InventoryLot
├── InventoryHold
└── InventoryTransaction

InboundOrder 聚合
├── InboundOrder
└── InboundOrderLine

OutboundOrder 聚合
├── OutboundOrder
└── OutboundOrderLine

StocktakeOrder 聚合
├── StocktakeOrder
└── StocktakeOrderLine
```

原则：

- 库存聚合负责数量一致性。
- 单据聚合负责状态机和明细完整性。
- 跨聚合协作由 Application Service 编排。

### 10.5 单据状态机

#### 入库单状态

```text
DRAFT → APPROVED → RECEIVING → RECEIVED → PUTAWAYING → COMPLETED
  ↓         ↓            ↓             ↓
CANCELLED  CANCELLED    CANCELLED     EXCEPTION
```

#### 出库单状态

```text
DRAFT → APPROVED → ALLOCATED → PICKING → PICKED → CHECKED → SHIPPED → COMPLETED
  ↓         ↓            ↓          ↓        ↓
CANCELLED  CANCELLED    CANCELLED  EXCEPTION EXCEPTION
```

#### 盘点单状态

```text
DRAFT → FROZEN → COUNTING → DIFFERENCE_CONFIRMING → ADJUSTING → COMPLETED
  ↓        ↓          ↓
CANCELLED CANCELLED  EXCEPTION
```

状态机规则：

- 状态流转必须由领域方法控制。
- 禁止在 Application 或 Controller 中直接修改状态字段。
- 非法状态流转必须抛出领域异常。

### 10.6 推荐数据库表

#### 基础资料

```text
wms_owner
wms_warehouse
wms_zone
wms_location
wms_sku
wms_sku_package
wms_supplier
wms_customer
wms_carrier
```

#### 库存

```text
wms_inventory
wms_inventory_lot
wms_inventory_transaction
wms_inventory_hold
```

#### 入库

```text
wms_inbound_order
wms_inbound_order_line
wms_receive_task
wms_putaway_task
```

#### 出库

```text
wms_outbound_order
wms_outbound_order_line
wms_allocation_detail
wms_pick_task
wms_shipping_order
```

#### 库内作业与盘点

```text
wms_transfer_order
wms_transfer_order_line
wms_stocktake_order
wms_stocktake_order_line
wms_inventory_adjustment
```

### 10.7 通用表字段

所有业务表建议包含：

```sql
id BIGINT NOT NULL PRIMARY KEY,
tenant_id BIGINT NULL,
created_at DATETIME NOT NULL,
created_by BIGINT NULL,
updated_at DATETIME NOT NULL,
updated_by BIGINT NULL,
deleted TINYINT NOT NULL DEFAULT 0,
version INT NOT NULL DEFAULT 0
```

说明：

- `deleted` 用于逻辑删除。
- `version` 用于乐观锁。
- 如无多租户要求，`tenant_id` 可暂不启用，但保留扩展点需由项目决定。

### 10.8 核心索引建议

```text
wms_inventory:
- uk_inventory_dimension(warehouse_id, location_id, sku_id, lot_no, inventory_status, deleted)
- idx_inventory_sku(warehouse_id, sku_id, deleted)
- idx_inventory_location(warehouse_id, location_id, deleted)

wms_inventory_transaction:
- idx_txn_biz(biz_type, biz_no)
- idx_txn_sku(warehouse_id, sku_id, created_at)
- idx_txn_location(warehouse_id, location_id, created_at)

wms_inbound_order:
- uk_inbound_order_no(order_no)
- idx_inbound_status(warehouse_id, status, created_at)

wms_outbound_order:
- uk_outbound_order_no(order_no)
- idx_outbound_status(warehouse_id, status, created_at)
```

### 10.9 核心用例分层示例

#### 出库库存分配

```text
interfaces.controller.OutboundOrderController
  → application.service.OutboundAllocationApplicationService
    → domain.repository.OutboundOrderRepository
    → domain.repository.InventoryRepository
    → domain.model.OutboundOrder.allocate()
    → domain.model.Inventory.reserve()
    → infrastructure.repository.*RepositoryImpl
      → infrastructure.mapper.*Mapper
```

职责：

- Controller：接收请求、参数校验、返回结果。
- Application Service：开启事务、加载出库单和库存、编排分配流程。
- Domain：校验单据状态、执行库存预占规则。
- Infrastructure：持久化单据、库存和流水。

#### 入库上架

```text
interfaces.controller.InboundOrderController
  → application.service.InboundPutawayApplicationService
    → domain.repository.InboundOrderRepository
    → domain.repository.InventoryRepository
    → domain.model.InboundOrder.putaway()
    → domain.model.Inventory.increaseOnHand()
    → infrastructure.repository.*RepositoryImpl
```

职责：

- 入库单聚合控制上架状态和明细完成数量。
- 库存聚合控制库存增加和流水生成。
- Application Service 负责事务和跨聚合协作。

### 10.10 API 设计风格

REST 路径建议：

```text
POST   /api/wms/inbound-orders
POST   /api/wms/inbound-orders/{orderId}/approve
POST   /api/wms/inbound-orders/{orderId}/receive
POST   /api/wms/inbound-orders/{orderId}/putaway

POST   /api/wms/outbound-orders
POST   /api/wms/outbound-orders/{orderId}/approve
POST   /api/wms/outbound-orders/{orderId}/allocate
POST   /api/wms/outbound-orders/{orderId}/pick
POST   /api/wms/outbound-orders/{orderId}/ship

GET    /api/wms/inventories
GET    /api/wms/inventory-transactions
POST   /api/wms/transfers
POST   /api/wms/stocktakes
```

接口规则：

- 写操作使用 `POST`。
- 查询使用 `GET`。
- Request / Response 不直接复用 Domain Model。
- 列表查询必须分页。
- 业务操作接口必须支持幂等或明确不支持的原因。

## 11. 代码质量与验证命令

### 11.1 禁止项

- 禁止生产代码使用 `System.out.println`。
- 禁止生产代码使用 `printStackTrace`。
- 禁止随意捕获异常后吞掉。
- 禁止 Controller 调 Mapper。
- 禁止 Application 调 Mapper。
- 禁止 Infrastructure 返回 DTO。
- 禁止无理由新增依赖。
- 禁止提交无法解释的格式化大改。

### 11.2 验证命令

如果是 Maven 项目，优先运行：

```bash
mvn test
mvn verify
```

如果是 Gradle 项目，优先运行：

```bash
./gradlew test
./gradlew check
```

如果存在项目专用命令，以项目文档、CI 配置或现有脚本为准。
