# HTTP 接口设计

> 本文记录 WMS + WES + WCS 的目标设计与参考实现。后续开发以本分支的 Java WMS 工程为基础，接口和功能的当前实现进度以代码与测试为准。

所有路径以 `/api` 开头，写请求使用 JSON。ID 为服务端生成的 UUID，枚举使用大写英文，日期为 ISO 8601 日期。成功返回 HTTP 200 和资源／查询对象。失败返回 `{"code":"INSUFFICIENT_STOCK","message":"第 1 行的合格可用库存不足"}`。作业台与 API 同源，默认仅本机访问。上线服务需要企业身份、仓库数据域、设备身份和接口授权。

分页列表支持 `limit`（默认 50，1–200）及 `offset`（默认 0）。列表返回数组，少于 limit 表示到末页；写入同时发生时允许翻页漂移，审计导出需要固定快照或游标。基础目录直接返回数组。

## 接口清单

| 方法 | 路径（省略 /api） | 请求 | 响应或行为 |
| --- | --- | --- | --- |
| GET / POST | /warehouses | 新增填 code、name | 仓库列表 / Master |
| GET | /warehouses/{warehouseId}/catalog | — | {skus, locations, devices} 三个 Master 列表 |
| POST | /warehouses/{warehouseId}/skus | code、name | Master |
| POST | /warehouses/{warehouseId}/locations | code、name、locationType | Master |
| POST | /warehouses/{warehouseId}/devices | code、name、deviceMode | Master |
| PUT | /devices/{deviceId}/state | {state} | Master；运行中的设备不能停用 |
| GET / POST | /warehouses/{warehouseId}/documents | 分页 / DocumentInput | Document 列表 / Document |
| GET | /documents/{documentId} | — | {document, lines, tasks, audit} |
| POST | /documents/{documentId}/actions/{action} | {reason} | Document |
| GET / POST | /warehouses/{warehouseId}/waves | 分页 / {code, priority, documentIds} | Wave 列表 / Wave |
| GET | /waves/{waveId} | — | {wave, documents}，按波次成员顺序 |
| POST | /waves/{waveId}/actions/{action} | {reason} | Wave；action 为 RELEASE 或 CANCEL |
| GET | /warehouses/{warehouseId}/tasks | 分页 | READY、RUNNING、EXCEPTION 的 Task 列表 |
| GET | /tasks/{taskId} | — | {task, document, commands, events}，events 属于最新轮次 |
| POST | /tasks/{taskId}/dispatch | {deviceId} | Command，新建执行轮次 |
| POST | /tasks/{taskId}/reconcile | {completed, observedQuantity?, reason} | Task |
| GET | /tasks/{taskId}/commands | — | 全部轮次的 Command 列表 |
| GET | /commands/{commandId} | — | {command, task, events} |
| POST | /commands/{commandId}/events | EventInput | Command |
| GET | /warehouses/{warehouseId}/inventory | 分页 | 库存余额数组 |
| GET | /warehouses/{warehouseId}/ledger | 分页 | 库存流水数组 |

## 写请求、约束和示例

- Master 新增使用 1–40 位字母、数字、点、下划线或连字符编码及 1–100 位名称。locationType 为 STORAGE、PICK_FACE、QUARANTINE。deviceMode 为 SIMULATOR、MANUAL。设备默认为 ENABLED，启停使用 ENABLED、DISABLED。
- 单据外部编号 1–64 位，可额外含冒号与斜杠。相同仓库、类型和规范化外部编号，且原始请求字段的反序列化值一致，返回当前单据；改变内容报 409。明细 1–100 行，顺序参与去重。数量每行 1–1,000,000,000，盘点请求省略数量。
- 波次 1–50 张不同、同仓且已审核的出库单，priority 为 0–100。按 documentIds 顺序分配，释放任一单失败即全部回滚。
- 审核、收货、取消、释放、确认差异、复核和异常核对要求 reason，1–240 字符，用于记录现场证据。退拣取消须写明已返原位及核对依据。
- 回执编号 1–64 位，字符集与外部编号相同。STARTED 后才能 SUCCEEDED；FAILED 允许从 SENT/STARTED 提交且 detail 必填。盘点成功或核对确认已盘必须填 observedQuantity，整数 0–1,000,000,000,000。其他回执省略此字段。

| document.kind | batchCode | sourceId | destinationId | 其他 |
| --- | --- | --- | --- | --- |
| INBOUND | 必填 | 省略 | 必填 | 可填 expiresOn；首次入库定义批次 |
| OUTBOUND | 可选 | 可选 | 省略 | 留空自动 FEFO 分配 |
| RETURN | 必填 | 省略 | 必填，必须隔离位 | sourceDocumentId 为同仓已发运出库单 |
| TRANSFER | 必填 | 必填 | 必填 | 同仓，不同库位 |
| REPLENISH | 必填 | 存储位 | 拣选位 | 同仓，不同库位 |
| COUNT | 必填 | 必填 | 省略 | quantity 省略；释放时系统记快照 |

入库建单的 JSON，其中 UUID 应使用目录接口返回值：

```json
{
  "kind": "INBOUND",
  "externalRef": "ERP:PO-24001",
  "lines": [
    {
      "skuId": "b9ffc357-275a-42f1-a174-f55f30d9ad9e",
      "batchCode": "L202611",
      "expiresOn": "2027-11-01",
      "destinationId": "daefdd03-2b23-4ec6-aae4-225059467510",
      "quantity": 24
    }
  ]
}
```

先提交 `{"eventKey":"station-01:1","eventType":"STARTED"}`，然后盘点成功回执示例：

```json
{"eventKey":"station-01:2","eventType":"SUCCEEDED","observedQuantity":23}
```

异常核对接口示例，确认未作业后任务重新进入 READY：

```json
{"completed":false,"reason":"班组长核对源位与工作站，料箱未移动"}
```

## 读响应和错误

| 对象 | 响应字段（举要） |
| --- | --- |
| Master | id、warehouseId、code、name、locationType、deviceMode、state |
| Document | id、warehouseId、kind、externalRef、sourceDocumentId、state、createdAt |
| Line | id、lineNo、skuId、lotId、batchCode、sourceId、destinationId、quantity |
| Wave | id、warehouseId、code、priority、state、reason、createdAt |
| Task | id、documentId、lineId、lotId、batchCode、sourceId、destinationId、quantity、observedQuantity、state、attempt |
| Command | id、taskId、deviceId、attempt、state、createdAt |
| Event | eventKey、eventType、observedQuantity、detail |
| Stock | locationId、locationCode、locationType、skuId、skuCode、lotId、batchCode、expiresOn、onHand、reserved、available |
| Ledger | id、taskId、locationId、lotId、batchCode、reason、deltaOnHand、deltaReserved、createdAt |
| Audit | id、taskId、action、detail、createdAt |

- 400：字段格式、库位类型不符、跨仓引用、实盘数量形态或未知枚举。
- 404：目标业务实体或所引用的主数据不存在。
- 409：状态冲突、数量不足、设备忙、盘点冻结、波次占用、退货超过原发运、重复键或旧轮次。
- 断线后查询业务对象。设备回执以相同 eventKey、eventType、数量和原因安全重放。业务动作重复通常返回 409，客户端以读取到的目标状态判断第一次处理结果。

## 外部集成契约设计

上线由 ERP/OMS 适配层将原单、货主、仓、行号和批次规范化，调用建单接口；对外回传完成事件带 eventId、bizType、externalRef、仓库、行/批次数量、发生时间和业务关联键。WES 与现场边缘网关使用命令 ID 和轮次关联，通过 Inbox 去重，通过 Outbox 保证提交后发布。网关回执带设备身份、指令 ID、回执编号、状态、现场时间和故障码。现场心跳与告警独立于业务成功回执；设备的能力、模式、实时位置及互锁按现场协议接入。
