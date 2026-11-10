# 决策与演进日志

只记已拍板的事实，不预测、不编造。新条目追加在文末。

---

## 2026-11-10 · 比赛项目立项与模块落点

- **决定**：本项目为比赛「中央学生助手」，不复用编码帮手群；在现有学生消费账单仓库加助手层，不新开 App、不重写记账。
- **仓库**：[Student-Spending-Bill](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill)，包名 `com.campusbill.app`，数据继续用 `com.campusbill.app.data` 的 Room（BillDao / BillEntity）。助手只经工具适配器查询/确认后写入并回读。
- **工程落点**：扩展现有 `com.campusbill.app.chat` 与 `chat.ai`（AiChatActivity、AiRemoteClient、skills）：DeepSeek 向导 + Keystore、流式、文字状态、确认卡片、可选可关闭 Emotion Ball WebView。
- **文档落点**：系统提示词与工具 schema → `docs/assistant/`；过程日志 → `docs/assistant/process/`；衔接已有 `prompt_evolution.md`、`Promat历史`、`制作过程.docx`。密钥不进仓库。
- **第一版范围**：① 查本周花销、确认后记一笔并回读；② 日记仅经用户确认后作上下文；③ 娱乐只留一个运势/情绪工具。
- **明确不做**：独立横屏 UI、八个侧边栏、塔罗/日记/随手记独立 App、多厂商 Key 向导。
- **Emotion Ball**：上游 sam70361/aora-bot（经典球形在 emotion-ball 分支），须标注许可，该造型不可商用。
- **人员**：主管大人总协调并盯范围；Trae 工程；DSH Prompt/schema；WorkBuddy 复现与许可页；UI 设计主界面/确认卡；流程记录员从本日记录留痕。项目顾问暂时移出。
- **来源**：学生助手比赛群，主管大人 2026-11-10 模块落点说明。

---

## 2026-11-10 · 旧过程材料先不润色

- **决定**：仓库里已有的过程记录与提示词（`制作过程.docx`、`prompt_evolution.md`、`Promat历史`）现阶段视为原始材料，不整理、不润色、不改写成最终交赛稿。交赛前再统一收拾。
- **做法**：只在 `docs/assistant/process/` 追加新留痕（决策、Prompt、工具参数、确认记录）；可在 `prompt_evolution.md` 末尾加一行索引，但不改旧正文。
- **来源**：学生助手比赛群，主管大人转述用户补充，2026-11-10。

---

## 2026-11-10 · 本地路径约定

- **决定**：项目只放在 `E:\AI` 下，具体路径为现有仓库 `E:\AI\Student Spending Bill`，不另开新目录或新工程。改代码、落 Prompt 和留痕都对这个目录。
- **来源**：学生助手比赛群，主管大人转述用户补充，2026-11-10。

---

## 2026-11-10 · 界面与文字状态

- **决定**：界面只挂在现有竖屏对话上，不新开页面、不加侧边栏或横屏。对话区是主体；「思考 / 调用工具 / 等待确认」用一行文字状态（输入栏上方或消息流顶部）；确认卡用 Grouped 白卡（浅灰底、靠间距分组、几乎无描边），且只出现在「等待确认」。Emotion Ball 默认可关，放角落或顶栏，不占中屏。文字状态只这三档，不加第四种。
- **评审硬指标**：流式输出和思考可视化。体验相关暂按评估页 https://cursor.com/canvas/store-bc74cac2-9413-4d73-8afb-e2acc238f862/4b314a43-e8cb-4ecd-87a6-864e978014a7 收束。
- **待确认**：比赛要求原件当时群内未见附件，主管大人与 UI设计大师均请用户再发。
- **来源**：学生助手比赛群，UI设计大师、主管大人，2026-11-10。

---

## 2026-11-10 · 工具清单与 Prompt v0.1 落点（已冻住）

- **决定**：范围未膨胀，四个工具和三档文字状态按此冻住。
- **工具**：`query_spend`（只读查询）；`propose_add_bill`（只出确认卡，确认后由应用写入 Room 并回读）；`request_diary_context`（日记须先确认）；`daily_fortune`（唯一娱乐入口，`tarot_lite` 只是玩法）。
- **约定**：本周按本地周一至周日，日期 `YYYY-MM-DD`，对齐现有 `BillEntity.date`。
- **文件（DSH v0.1 草稿，先不改 Java）**：`SYSTEM_PROMPT.v0.1.md`、`tools.schema.v0.1.json`、`intent-control.v0.1.md`、`think-protocol.v0.1.md`、`emotion-map.v0.1.md`、`process/README.md`。事件格式见 `think-protocol.v0.1.md`。
- **进度**：DSH 云端任务 [文档草稿提交](https://cursor.com/agents/bc-f80fb7dc-c105-5ee8-abeb-c6f70a55d367)；Trae 工程任务 [Central assistant layer on Campus Bill](https://cursor.com/agents/bc-3f78ddb6-e3ab-5fa5-950c-a05460caa926)。Prompt 正文与 PR 链接当时尚未发到群内，入库状态待确认。
- **来源**：学生助手比赛群，DSH助手、Trae助手、主管大人，2026-11-10。

---

## 2026-11-10 · 文档合入顺序

- **决定**：Trae 与 DSH 都在写同一仓库。工程分支先收 WorkBuddy 的复现/许可/过程模板文档，DSH 的 Prompt PR 开出来后再合进同一条分支，避免 `docs/assistant/` 各改各的。
- **WorkBuddy 草稿（尚未入库）**：本机 `/workspace/student-assistant-docs/`。应对齐到仓库的包括 `docs/assistant/REPRODUCE.md`、`docs/assistant/THIRD_PARTY.md`、`docs/assistant/process/`（含本日志与模板）、根 README 追加段 `README-SECTION.md`；建议把 Keystore 与 `docs/assistant/process/runtime/` 写入 `.gitignore`。Trae 已回复会并进现有仓，不新开目录。
- **来源**：学生助手比赛群，主管大人、WorkBuddy助手、Trae助手，2026-11-10。

---

## 2026-11-10 · 文字状态四档冻结

- **决定**：在原三档（思考 / 调用工具 / 等待确认）上增加「出错」，用于密钥失败、余额不足、网络错误；同一行文字状态，不加新页面、不加工具。四个工具（`query_spend`、`propose_add_bill`、`request_diary_context`、`daily_fortune`）与这四档状态一并冻结。
- **原因**：评估页已有「出错」；主管大人确认不算膨胀。
- **跟进**：@DSH助手 补 `think-protocol` 与 `emotion-map`；@Trae助手 界面按四档落。
- **来源**：学生助手比赛群，UI设计大师建议，主管大人拍板，2026-11-10。

---

## 2026-11-10 · 官方赛道介绍与提交要求入库

- **决定**：用户将赛道介绍与作品提交要求原文发到本群。现阶段只据此补清单与复现指南，不润色旧材料、不提前写最终 PDF。
- **硬性提交项**：仓库 Public 或可评审访问；细粒度 Git 历史（禁止一次提交堆齐功能）；至少 3 条核心功能完整 Prompt 链（含提问、AI 建议、引导修 Bug）；README「10分钟极速复现指南」含环境、依赖、环境变量模板；5–8 分钟 1080p 演示视频（其中至少 2 分钟为 AI 协同过程复现）；技术文档 PDF 不超过 30 页（意图控制、架构约束、可复现性论证）。
- **文档落点**：`docs/assistant/REPRODUCE.md`、`docs/assistant/env.example`、`docs/assistant/SUBMISSION-CHECKLIST.md`。
- **来源**：学生助手比赛群，用户粘贴「赛道介绍 / 作品提交要求」，2026-11-10。

---

## 2026-11-10 · 文字状态改四档

- **决定**：在已冻住的「思考 / 调用工具 / 等待确认」之外，增加「出错」这一档。评估页本来就有，不算范围膨胀。只用于密钥失败、余额不足、网络错误这类失败态，同一行文字状态提示，不加新页面、也不加工具。确认卡仍只出现在「等待确认」。
- **界面**：流式字符输出是硬指标；文字状态须独立可见；Emotion Ball 只是加分，不能替代状态，不占中屏。
- **落点**：DSH 将「出错」补进 `think-protocol` 与 `emotion-map`，改动追加到现有 Prompt 草稿 PR，不另开目录。Trae 界面按四档落，等协议更新后对齐。
- **来源**：学生助手比赛群，UI设计大师提议，主管大人同意，DSH助手、Trae助手确认，2026-11-10。此前「三档冻住」条目以本条为准更正。

---

## 2026-11-10 · Prompt 草稿 PR 已开

- **决定**：DSH v0.1 草稿 PR 已开：[docs: add assistant prompt/schema drafts v0.1](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/2)，分支 `cursor/assistant-docs-v0.1-d367`。只加了 `docs/assistant/`，并在 `prompt_evolution.md` 末尾追加【序号】14，没有改 Java，也没有润色旧文案。工程分支先收 WorkBuddy 文档，再将此 PR 合进同一条分支；Trae 不另改 `docs/assistant/` 里的 Prompt 正文。「出错」补丁也将推到同一条 PR。
- **来源**：学生助手比赛群，DSH助手、Trae助手，2026-11-10。此前「PR 链接尚未发到群内、入库状态待确认」以本条更新。

---

## 2026-11-10 · 官方赛道要求（原件入群）

- **决定**：用户将全国高校 Vibe Coding 开发挑战赛赛道介绍发到本群，作为官方要求原文。过程必须可复现；旧材料（`制作过程.docx`、`prompt_evolution.md`、`Promat历史`）先不动，只按本口径追加新留痕。
- **仓库与演进**：托管 GitHub/Gitee（须 Public 或提供评审访问权限）。必须保留完整、细粒度的 Git 提交历史，禁止一次 commit 堆完全部功能；每次提交应对应一次与 AI 的核心对话或一个功能模块的落地。须包含至少 3 个核心功能迭代时与 AI 交互的完整 Prompt 链（提问、AI 建议、如何引导修 Bug）；工具不支持导出时，可以截图或录屏。
- **README**：按「10 分钟极速复现指南」写清环境配置、依赖清单、环境变量模板。密钥仍不进仓库，只给模板。
- **交赛项（主线跑通后再做）**：演示视频 5–8 分钟、MP4 1080p，除功能演示外必须包含至少 2 分钟「AI 协同过程复现展示」；技术文档 PDF 不超过 30 页，重点阐述意图控制策略、架构设计、可复现性论证。
- **工程与 Prompt**：云端改仓和 PR 按细粒度提交拆（向导与 Keystore、流式与状态、Room 工具与确认卡、留痕）。意图控制和 Prompt 链继续按 `docs/assistant/` 的版本文件留痕。
- **体验对照**：评审会看交互细节和产品闭环；演示视频里要清楚拍到流式输出、四档文字状态和确认卡。
- **来源**：学生助手比赛群，用户粘贴赛道介绍原文；主管大人转述口径，WorkBuddy、Trae、DSH、UI设计大师确认，2026-11-10。此前「比赛要求原件群内未见附件」以本条更新。
