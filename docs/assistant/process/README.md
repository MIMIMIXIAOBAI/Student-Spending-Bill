# 过程日志目录约定

本目录存放可给评委点开的过程留痕（决策、Prompt 版本、工具参数、确认记录）。格式见上一级 `think-protocol.v0.1.md`。

建议文件名：

```
process/YYYY-MM-DD-<demo-or-session-id>.jsonl
```

- 只记事实，不编造。
- 禁止写入 API Key、Keystore 内容或完整密钥。
- 演示「这周食堂 + 记午餐 18 元」应能在同一文件里看到：system prompt 版本、`query_spend` 参数与摘要、`propose_add_bill` 预览、确认结果与回读核对。

具体模板与一键复现步骤见 [../REPRODUCE.md](../REPRODUCE.md)、[templates/](templates/) 与 [DECISION_LOG.md](DECISION_LOG.md)。上面的 JSONL 约定仍然有效，不另起一套记录格式。

## 目录（与 JSONL 并存）

```
docs/assistant/process/
├── README.md                 # 本约定（DSH 的 JSONL 规则 + 模板目录）
├── DECISION_LOG.md           # 决策与范围演进（只追加）
├── templates/                # 空白模板
│   ├── session-index.md
│   ├── prompt-record.md
│   ├── tool-call.md
│   └── confirm-record.md
├── sessions/                 # 脱敏后的演示归档（按日期）
└── runtime/                  # 本机原始 JSONL（gitignore，不提交）
```

应用过程页读取本机 `runtime/*.jsonl`（设备上在应用私有目录的同等路径）。事件 `type` 仍是 `think-protocol.v0.1.md` 里的枚举。界面文字状态在协议三档之外，经项目负责人追加了「出错」（仅密钥、余额/配额、网络失败）；协议文件更新前，工程照原样加载 `think-protocol.v0.1.md`，并允许 `status.value` 为「出错」。

一条归档会话至少能对上：会话索引、Prompt 版本、每次工具的名称和参数、写库前的确认记录、状态时间线。文件名用 `sessions/YYYY-MM-DD-<短名>/`。不要把 Key 或未脱敏明细提交进 git。
