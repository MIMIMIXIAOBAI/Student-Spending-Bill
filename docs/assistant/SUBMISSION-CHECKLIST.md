# 交赛材料清单

依据用户 2026-11-10 贴到「学生助手比赛」群的赛道介绍与作品提交要求整理。本页是清单和落点，不是最终交赛稿。旧材料（`制作过程.docx`、`prompt_evolution.md`、`Promat历史`）先当原始资料，交赛前再统一收拾。

## 2.1 代码仓库与演进记录

| 要求 | 现状 / 做法 | 负责 |
|---|---|---|
| GitHub / Gitee，Public 或向评委开放访问 | 现仓库 [Student-Spending-Bill](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill) 为 Public | 工程 / 主管 |
| 完整、细粒度 Git 提交历史；禁止「一次 initial commit 堆齐全部功能」；每次提交对应一次与 AI 的核心对话或一个功能模块落地 | 助手层须保持细粒度提交（文档、Prompt、工程分批合入）。历史是否已满足，交赛前由工程侧核对日志 | @Trae助手 |
| 导出与 AI 的对话历史；工具不支持导出时用截图或录屏 | 新对话按 `docs/assistant/process/` 模板归档；旧对话先保留原文件，不润色 | @流程记录员 收集，@WorkBuddy助手 协助整理目录 |
| 至少 3 个核心功能迭代的完整 Prompt 链（提问、AI 修改建议、如何引导修复 Bug） | 建议三条主线：① 查本周花销（`query_spend`）② 确认后记一笔并回读（`propose_add_bill`）③ 流式 + 四档文字状态 / 过程页，或运势调度（`daily_fortune`）。链尚未齐备，标为待收集 | 全员；Prompt 正文以 @DSH助手 入库版本为准 |

## 2.2 复现性声明与重现指南（README.md）

| 要求 | 落点 |
|---|---|
| 10 分钟极速复现指南 | [REPRODUCE.md](REPRODUCE.md)，根 README 追加段指向本文 |
| 完整环境配置说明、依赖清单、环境变量模板 | [REPRODUCE.md](REPRODUCE.md) 第 1–3 节；[env.example](env.example)（只放占位，真实 Key 不入库） |

## 2.3 演示视频与技术文档

| 要求 | 做法 | 状态 |
|---|---|---|
| 演示视频 5–8 分钟，MP4 1080p | 除功能演示外，必须包含至少 2 分钟「AI 协同过程复现展示」（输入某段 Prompt 生成核心模块，或引导修复顽固 Bug） | **已录制（2026-11-12）**：1080×1920 竖屏 1080p，H.264 High / yuv420p / 25fps，AAC 44.1kHz；时长 **5:49.89**，16,422,369 B，SHA-256 `B7C39F4F…854A1A`；**AI 连续区间 03:03–05:38 = 155 s**（≥120 s）。母版 `E:\AI\_ssb_verify\demo.mp4`；交赛副本 `提交材料\演示视频.mp4`（同哈希）。**交付方式已定：不入库**——16.4 MB 二进制不占 git 历史，随交赛以附件/外链提交 |
| 技术文档 PDF，不超过 30 页 | 重点：意图控制策略（上下文与 Prompt 设计）、架构设计（AI 生成代码的架构及如何约束）、可复现性论证 | **已成稿（2026-11-12）**：`docs/assistant/TECHNICAL-DOC.pdf`，25 页，由 `docs/assistant/tech-doc/render.ps1` 从 `TECHNICAL-DOC.md` 渲染；含系统提示词、工具 schema、`think-protocol`、Prompt 链与可复现性章节 |

## 2.4 交赛前待办（2026-11-12 实测状态）

1. ~~视频入库方式~~（已定，2026-11-12）：**不入库**。`提交材料\演示视频.mp4`（16,422,369 B，与母版同 SHA-256）保持未跟踪，随交赛以附件/外链交付；仓库无 LFS，不把 16.4 MB 二进制写进历史。
2. **对话导出 / Prompt 链归档**：`docs/assistant/process/sessions/` 只有 `.gitkeep`，3 条主线的对话导出尚未按 `templates/session-index.md` 归档（Prompt 正文本身已随代码、`docs/assistant/` 与 git log 入库）。
3. **PDF 复核**：`TECHNICAL-DOC.pdf` 由 `tech-doc/render.ps1` 生成，改动 `TECHNICAL-DOC.md` 后必须重渲；交赛前复核页数 ≤30、目录与正文页码一致。
4. **工作树清理**：当前有 15 个 CRLF/换行基线脏文件与 2 个删除项（`~$制作过程.docx`、`基础提示词.txt`），提交前确认哪些是噪音、哪些该还原。

## 建议时间顺序

1. 保持细粒度提交，合入文档与 Prompt PR，不冲掉旧历史。
2. 边做边按模板归档 Prompt 链，凑满至少 3 条。
3. 功能可演示后再录视频，并单独留出 2 分钟协同过程。
4. 交赛前再统一润色旧材料，并输出不超过 30 页的 PDF。
