# 过程日志目录约定

本目录存放可给评委点开的过程留痕（决策、Prompt 版本、工具参数、确认记录）。格式见上一级 `think-protocol.v0.1.md`。

建议文件名：

```
process/YYYY-MM-DD-<demo-or-session-id>.jsonl
```

- 只记事实，不编造。
- 禁止写入 API Key、Keystore 内容或完整密钥。
- 演示「这周食堂 + 记午餐 18 元」应能在同一文件里看到：system prompt 版本、`query_spend` 参数与摘要、`propose_add_bill` 预览、确认结果与回读核对。

具体模板与 README 一键复现步骤由 WorkBuddy 助手补全；本约定供工程与流程记录员对齐路径。
