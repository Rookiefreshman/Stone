# v0.9.7 自然语言确认与实际只读业务链路验收

Stone 领域代码、API、MySQL V6 不变。CI 固定检出 Agent 5596372d5af0f96f1de6c91df0538cdd00398361。
[实际 CI 45766336138](https://github.com/Rookiefreshman/Stone/actions/runs/45766336138)：MySQL 5.7/8.0 各45项 Java 测试，零失败、错误、跳过；9组实际 HTTP 检查全部通过。新增组通过 managed session 的有限自然语言 Fixture 语法、用户明确类型化确认、原有 RULE Runtime，分别执行真实历史回执、出库诊断和 WES 调度查询。无执行模型调用，业务 POST=0。不是外部真实 LLM 语义评测。
[Agent CI 45766300805](https://github.com/Rookiefreshman/stone-agent/actions/runs/45766300805)：346 passed, 1 warning，新增21项。首次 CI 的生成正则语法错误已修复，保留失败记录。
真实 IAM、真实 LLM、实际业务发送及目标环境验收尚未完成，生产写入关闭。
[Agent 自然语言业务契约与运行说明](https://github.com/Rookiefreshman/stone-agent/blob/agent/business-language-v0.9.7/docs/BUSINESS_LANGUAGE_V097.md)
