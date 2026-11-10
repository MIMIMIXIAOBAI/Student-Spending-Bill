# 中央学生助手 · Prompt 与协议（v0.1 草稿）

- **版本**：`v0.1`（2026-11-10，Asia/Shanghai）
- **范围**：比赛项目第一版。只接通三件事：查账/记账主线、日记作经确认的上下文、一个娱乐工具（今日运势/情绪晴雨表；塔罗只作为运势下的玩法）。
- **仓库落点**：本目录。过程日志见 `process/`。演进记录同时接到仓库根目录 `prompt_evolution.md`、`Promat历史/`。
- **实现约束**：不改账本数据模型；助手只经工具适配器访问 `BillDao` / `BillEntity`。密钥不进仓库。本文档只定义 Prompt / schema / 协议，不改 Java（由工程侧接入）。

## 文件

| 文件 | 用途 |
|---|---|
| `SYSTEM_PROMPT.v0.1.md` | 系统提示词（发给模型的正文） |
| `tools.schema.v0.1.json` | 工具 schema（DeepSeek 兼容的 function calling） |
| `intent-control.v0.1.md` | 意图优先级、反例话术、演示脚本对齐 |
| `think-protocol.v0.1.md` | 流式思考可视化与过程留痕事件 |
| `emotion-map.v0.1.md` | 情绪/状态映射（文字状态为主体；Emotion Ball 可选可关） |
| `process/README.md` | 过程日志目录约定 |
| [REPRODUCE.md](REPRODUCE.md) | 评委复现步骤（WorkBuddy） |
| [THIRD_PARTY.md](THIRD_PARTY.md) | 第三方许可，含 Emotion Ball（WorkBuddy） |
| [process/DECISION_LOG.md](process/DECISION_LOG.md) | 已拍板决策 |
| `process/templates/` | 会话索引、Prompt、工具、确认记录模板 |

上表中的 v0.1 Prompt / schema / 协议文件以 DSH 版本为准，工程侧只读取、不另写一套 schema。修订时递增版本号（v0.2…），旧文件保留以便评委复现。
