# v0.9.3 Stone Java/MySQL 与跨服务验收

本版仅增加 CI 验收机制及文档，不改变领域业务、API 或数据库 Schema。V1–V5 依旧按原顺序手工迁移；生产写入不放行。

## 自动化验收
- .github/workflows/business-verification.yml：Java 8 + MySQL 5.7、8.0 两组独立临时服务容器；执行 V1–V5、mvn -B -ntp clean verify。
- 解析 Surefire XML，要求核心测试套件存在、测试数大于零、没有失败/错误/跳过，上传测试报告。
- 固定检出 Agent 客户端 commit 99c43574fbbeec31c53313a9e160ff09f317ac32。用 Java 测试生成的任务和回执 ID 启动本地 Spring Boot JAR（127.0.0.1:18081），启用只读诊断和独立回执查询开发凭据。Python 脚本走真实 HTTP，独立计算请求指纹，不从数据库复制指纹作为预期值。
- 查询前后检查选定业务表计数和库存余额一致；不是全库 diff。

## 实测记录
- [首次 Java/MySQL 验收](https://github.com/Rookiefreshman/Stone/actions/runs/45762032368)：MySQL 5.7 和 8.0 各 38 项（21 单测、17 MySQL 集成），零失败、零错误、零跳过。
- [真实 Java HTTP 与 Python Agent 验收](https://github.com/Rookiefreshman/Stone/actions/runs/45762289693)：双版本各 38 项 Maven 测试及 6 组跨服务检查全通过，业务 POST 为零，选定计数和余额不变。
- [Agent 回归](https://github.com/Rookiefreshman/stone-agent/actions/runs/45762213821)：305 passed、1 warning（含6项 PostgreSQL 集成）。
- 真实数据库验证了 READY 取消与领取竞争、并发幂等、操作回执与库存释放原子事务及回执插入故障回滚。Java/Python 指纹黄金向量和实际回执匹配均通过。

## 禁止误解
业务夹具是合成数据，内部取消事务在独立测试库中运行，授权使用测试替身；真实企业 IAM、独立审批、旧写接口访问治理仍未建立。新取消提交内部服务默认拒绝，无新增对外写 POST。生产真实发送、持久 SEND_INTENT、跨服务响应丢失恢复、真实回执 Worker、部署目标存量迁移、容量及安全验收仍待实施。测试不得连接生产库。

Agent [实施清单](https://github.com/Rookiefreshman/stone-agent/blob/agent/business-validation-v0.9.3/docs/PROJECT_CHECKLIST.md)。
