# Stone 仓储作业系统

WMS 管业务单据和库存，WES 管波次、作业队列与派发，WCS 管工作站、指令与回执。浏览器工作台可操作入库、移库、补货、盘点、出库复核与发运、原单退货，并追踪库存流水和设备异常。

## 运行

需要 JDK 8 或以上和 Maven。在上一步配置过的 Stone 云环境可先执行 `source /workspace/.stone-tools/activate.sh`。

```bash
mvn -B -C -ntp verify
java -jar target/stone-warehouse-0.1.0-SNAPSHOT.jar
```

启动后从本机浏览器打开 `http://127.0.0.1:8080/`。默认使用 H2 文件库，数据保存在工作目录下的 `data/`。这是本地信任模式，设备动作由模拟站或人工报工完成。

## 走一遍闭环

1. 点「载入演示数据」。在入库单 `DEMO-IN-001` 依次提交审核、收货；打开任务派发到模拟站，提交「已开始」「成功」。`A-01` 上架 24 件。
2. 建补货单，商品 `SKU-100`、批次 `BATCH-001`，从 `A-01` 向 `P-01` 转 8 件。审核、释放，再派发并回执。
3. 向 `P-01` 建同批次盘点单，审核并释放。派发后回执实盘 7 件，在单据中确认差异。库存流水记录盘亏 1 件。
4. 建同批次出库单，指定 `P-01` 出 4 件。审核后单单释放，或在「出库波次」中创建波次并整波释放。任务完成后复核打包并确认发运。
5. 复制已完成的出库单 ID 建退货单，退 2 件至隔离位 `Q-01`，审核收货、上架。库存页应显示 `A-01` 16 件、`P-01` 3 件、`Q-01` 2 件，均无预留。
6. 可另建作业提交失败回执，查看队列和设备槽状态。通过现场核对流程确认未作业后重新派发，或确认实物已完成并落账。

## 设计与测试

- [概要设计与总体流程](docs/architecture.md)
- [各模块详细设计与流程图](docs/modules.md)
- [数据库 ER 图、字段与账务时点](docs/database.md) · [DDL](src/main/resources/db/schema.sql)
- [HTTP 接口、请求示例与错误](docs/api.md)
- [集成测试](src/test/java/com/stone/warehouse/WarehouseFlowTest.java)

可使用 `mysql` profile 连接已有数据库。先执行 DDL，再设置 `STONE_DB_URL`、`STONE_DB_USER`、`STONE_DB_PASSWORD`，启动时传 `--spring.profiles.active=mysql`。生产部署前需接入身份授权、设备边缘网关，并做现场联调和容量验收。

在独立的 MySQL 测试数据库中可设置 `STONE_TEST_DB_URL`、`STONE_TEST_DB_USER`、`STONE_TEST_DB_PASSWORD` 后运行同一条 Maven 验证命令。测试会初始化表并创建测试仓与业务数据。
