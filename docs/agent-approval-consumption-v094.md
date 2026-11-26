# v0.9.4 可信审批绑定与事务内唯一消费

## 范围及业务理由
READY 库存作业取消已由 Stone 领域服务负责库存释放与回执原子性。本版只补审批消费约束，不重写取消事务，不引入 IAM、审批中心或生产写 HTTP 接口。现有旧写接口未修改，必须由实际网关/权限系统治理后才能生产放行 Agent 写入。

## 可信接入边界
CancelAuthorization 是受信内部端口，不是用户传入 DTO。requireAccess 必须基于服务端认证上下文校验实际主体、租户、仓权限、当前权限；verifiedApproval 必须校验签发者、真实性、撤销状态、审批权限、精确参数绑定，提供规范化稳定主体 ID。不能相信用户提供的 actor、approvalHash 或模型输出；不能把演练审批当作真实授权。仓锁内不进行远程网络 I/O，实际集成需要明确本地可信证明及撤权策略。没有生产授权 Bean 时默认拒绝 submit。VerifiedCancelApproval 仅是受信适配器输出，不具备签名验证能力。

## 执行路径
1. requireAccess 在读仓/回执前执行。锁仓后检查已有回执；重放重新校验当前访问权限，精确指纹匹配则返回原回执，不消耗审批。
2. 新操作在锁仓内再次检查访问权限，获取 VerifiedCancelApproval。必须绑定 tenant/requester 和完整 CancelCommand SHA-256 指纹（包括 operation ID、仓、任务、作业维度、原因及 approvalHash），申请人与审批人不同。数据库 UTC 毫秒时间必须处于 [validFromMs, expiresAtMs)。
3. 复查真实任务 READY 及任务标识、作业维度，复用原取消服务和库存释放事务。
4. V6 扩展原 wes_cancel_operation_receipt，保存 approval_issuer、approval_grant_id、approval_tenant、approval_requester、approval_reviewer、approval_valid_from_ms、approval_expires_at_ms。唯一索引 uk_cancel_approval_grant(approval_issuer, approval_grant_id) 作用于全部仓。签发方必须有全局稳定命名空间；若授权平台 ID 只在租户内唯一，需明确映射到稳定签发方命名空间。
5. INSERT ... SELECT ... WHERE 以数据库 UTC 时间再次校验有效期，零行插入抛出异常；唯一键冲突不得捕获后继续，整个领域事务回滚。只有提交成功才算消费。同一合法操作的回执重放不再消费。审批有效期二次检查发生在 INSERT SQL 语句时间，不是提交时间，不能宣称提交瞬间仍有效或消除撤权竞态。
6. 公开 GET 回执 DTO 不含审批人员和凭证信息，原 CANCEL_READY_V1 指纹协议不变。Agent 只读对账适配器无需改造。

## 迁移
V6__bind_cancel_receipt_to_approval.sql 在 V5 基础上增加7个可空审计字段和一个唯一索引，不新增表。历史 V5 回执缺少审批元数据，保留 NULL，不追认为可信审批；新内部写入必须使用受信审批。兼容既有只读接口。当前 CI 从空库依次应用 V1–V6，通过两个 MySQL 版本；已有生产 V5 数据上的增量迁移、DDL 锁影响、备份恢复未验收。禁止混合运行旧写入代码和新版本。先停受影响服务、备份、独立恢复库演练并确认审计与权限策略，再安排目标环境迁移。

## 实际验收
核心提交 707c2498a1929b74beba291457b14c63b33f8529，GitHub Actions 45763151336：
- MySQL 5.7、8.0 各45项测试，0失败、0错误、0跳过（25单元、20集成，较v0.9.3新增4单元、3集成）。
- 无审批、同主体审批、错误绑定、有效期边界、撤权拒绝；跨仓并发模拟重复签发同一 grant，只有一笔提交，另一笔库存、状态与流水回滚；审批失效及最终 SQL 时间条件拒绝；失败事务后经有效授权可重试，原回执重放不消费。
- 6组既有实际 Java/Python HTTP 契约检查双版本继续通过，选定业务计数及余额不变，Agent 业务 POST 为0。

测试授权来自 MockBean，不代表真实企业认证或审批。最终 SQL 过期测试通过注入过期有效期验证数据库条件及回滚，并非真实时钟竞态/跨服务故障注入。没有实际 IAM、生产审批、真实发送入口、SEND_INTENT、生产对账 Worker 或 NOT_COMMITTED 证明。保留 UNKNOWN 不重派。所有 Agent 真实业务写入关闭。
