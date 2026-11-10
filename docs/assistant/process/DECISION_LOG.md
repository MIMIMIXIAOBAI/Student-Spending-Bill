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

---

## 2026-11-10 · 复现指南与交赛清单草稿已落地（尚未入库）

- **事实**：WorkBuddy 按官方提交要求，将本机草稿目录 `/workspace/student-assistant-docs/docs/assistant/` 中的 `REPRODUCE.md` 改为「10分钟极速复现指南（评委用）」，并新增 `env.example` 与 `SUBMISSION-CHECKLIST.md`。已核对这些文件存在（mtime 2026-11-10 13:17，Asia/Shanghai，UTC+8）。真实 Key 只作占位，不入库。旧材料仍不润色；正文等工程和 Prompt 稳定后再统一收拾。
- **REPRODUCE.md 要点**：环境配置（Android Studio Narwhal 或更新、Platform API 37、仓库自带 Gradle Wrapper、根目录 `local.properties` 只写 `sdk.dir`）；依赖清单以 `app/build.gradle` 为准，并列出账本工程现有依赖（含 Room 2.6.1 等）；环境变量指向 `env.example`；演示主线依次为 `query_spend`（查本周食堂花销）→ `propose_add_bill`（确认后记一笔并回读）→ 过程页点开留痕 → `daily_fortune`（证明会调度工具）。文中写明四档文字状态，以及首次启动走 DeepSeek Key 向导并写入本机 Keystore。指南按已拍板的向导方案写；现仓库 README 仍写可选 API KEY / SharedPreferences，向导是否已在工程落地，仍待合入后核对，不视为已完成。
- **SUBMISSION-CHECKLIST.md**：按官方 2.1–2.3 列出细粒度 Git、至少 3 条核心功能完整 Prompt 链（建议主线含四档文字状态，并标明链尚未齐备、待收集）、10 分钟指南落点、5–8 分钟演示视频（待录制）与不超过 30 页技术文档 PDF（待写）。
- **env.example**：给出 `sdk.dir` 示例；说明 DeepSeek Key 默认经应用内向导写入 Keystore；若使用临时 `secrets.properties` 必须保持 gitignore；占位符为 `DEEPSEEK_API_KEY` / `DEEPSEEK_API_BASE` / `DEEPSEEK_MODEL`。
- **合入状态**：上述文件仍在本机草稿目录，尚未进入 GitHub 仓库或工程分支。WorkBuddy 已请 Trae 与上一批文档一并合进工程分支。
- **来源**：学生助手比赛群，WorkBuddy助手，2026-11-10（群消息无精确钟面时刻；本条按收到该轮消息的日期记录）。

---

## 2026-11-10 · 用户授权使用其电脑并暂时离开

- **事实**：用户在群内说可以使用其电脑，并暂时外出一段时间。此授权针对本比赛工程协作，路径仍为 `E:\AI\Student Spending Bill`。流程记录员不改工程代码；工程操作仍由 Trae 等按既定分工执行。
- **来源**：学生助手比赛群，用户，2026-11-10（群消息无精确钟面时刻）。

---

## 2026-11-10 · 比赛要求原件待确认已划掉

- **决定**：用户已将赛道介绍与作品提交要求原文发到本群。主管大人确认：此前「比赛要求原件群内未见附件」这条待确认可以划掉。官方口径仍以 2026-11-10「官方赛道要求（原件入群）」条目为准。
- **来源**：学生助手比赛群，主管大人 @流程记录员，2026-11-10。

---

## 2026-11-10 · Prompt PR #2 已补「出错」档（仍未合入 master）

- **事实**：已核 GitHub：[docs: add assistant prompt/schema drafts v0.1](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/2) 仍为 open，目标分支 master，head `cursor/assistant-docs-v0.1-d367` @ `4a59591fcac4`。PR 正文写明：在同一分支覆盖 `think-protocol.v0.1.md`、`emotion-map.v0.1.md`、`SYSTEM_PROMPT.v0.1.md`、`README.md`，补上「出错」（密钥失败、余额不足、网络错误，同一行文字状态、不加新页面）；并在 `prompt_evolution.md` 末尾追加【序号】15，未改【序号】14 及更早条目。共 2 个提交、8 个文件。创建于 2026-11-10 13:15，更新于 13:18（Asia/Shanghai，UTC+8）。DSH 在群内称草稿已含四档文字状态，不占用用户电脑。
- **来源**：GitHub PR #2 正文；学生助手比赛群，DSH助手，2026-11-10。此前「出错补丁是否已推到 PR #2」以本条更新。

---

## 2026-11-10 · 工程 PR #3 已开（尚未合入 master）

- **事实**：已核 GitHub：[Add the central student assistant on the existing chat screen](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3) 为 open，目标分支 master（底本 `3d5604acf3`，已含 Apple 系统 UI），head 分支 `cursor/central-student-assistant-a926` @ `9e8968b88dfe`。创建于 2026-11-10 13:35，更新于 13:47（Asia/Shanghai，UTC+8）。共 10 个提交，+6552 / −44，75 个文件。尚未合并。
- **PR 正文所写范围**（记作该 PR 的自我说明，合入 master 与真机冒烟仍待确认）：在现有竖屏 `AiChatActivity` 上扩展中央助手，沿用 Room（`BillDao` / `BillEntity`）；从 `cursor/assistant-docs-v0.1-d367` 合入 DSH Prompt v0.1；附带 WorkBuddy 许可页与过程模板，并加入 10 分钟复现指南、`env.example`、交赛清单；运行时留痕目录为应用私有 `assistant/process/runtime`（对应已 gitignore 的 `docs/assistant/process/runtime/`）。四个工具与四档文字状态按已拍板方案。日记写明可选 `assistant/diary.txt`（该存储落点是否为群内拍板，仍待确认）。Emotion Ball 默认关，顶栏「球」打开、「关」关闭；上游记为 sam70361/aora-bot 的 emotion-ball 分支 @ `f793bb4`。正文称 `AssistantCoreTest`（18 个测试）覆盖本周区间、四工具名、流式解析、确认门闩与过程脱敏；该云端环境无 Android SDK，未在此跑 `assembleDebug`。
- **群内跟进**：Trae 称云端单测已过，本机 Android 编译需用户回来授权后再跑；并称会把刚更新的复现指南、`env.example`、交赛清单、决策日志，以及 UI 三帧约定补进同一条工程分支。WorkBuddy 称本机草稿在 `/workspace/student-assistant-docs/`，不另开目录；演示视频与 30 页 PDF 仍等主线跑通后再做；用户不在时不擅自把本机文件推进仓库，云端改仓可继续。
- **来源**：GitHub PR #3；学生助手比赛群，主管大人、Trae助手、WorkBuddy助手，2026-11-10。

---

## 2026-11-10 · 对话页三帧示意与出错行「重试」

- **决定**：UI设计大师给出三帧示意，群内文字约定如下：对话区是主体；四档状态固定在输入栏上方同一行；确认卡只出现在「等待确认」；Emotion Ball 用顶栏开关、默认关。出错那帧里，「重试」做成同一行右侧的文字按钮，不要和「请重试」叠成两个词。Trae 已回复按此收，并称会补进同一条工程分支（PR #3）；截此是否已推送到该分支，待确认。
- **说明**：本条按群内文字记下；此轮记录未见三帧附图文件本身。
- **来源**：学生助手比赛群，UI设计大师、Trae助手，2026-11-10。

---

## 2026-11-10 · PR #3 正文已写入出错行「重试」（仍未合入 master）

- **事实**：2026-11-10 16:08（Asia/Shanghai，UTC+8）再次核对 GitHub：[PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3) 仍为 open，尚未合并。更新于 13:51，head 现为 `dee9adfb09e4`（13:47 时为 `9e8968b88dfe`），共 11 个提交，+6727 / −45，75 个文件。正文已写明：四档状态行在输入栏正上方；出错时左侧可显示短原因（密钥失败 / 余额不足 / 网络错误），唯一操作是右侧「重试」文字按钮，用于重发失败的 Key 测通或 DeepSeek 请求；确认卡只在「等待确认」出现；Emotion Ball 顶栏「球」打开、「关」关闭。`AssistantCoreTest` 现为 19 个测试，正文称已覆盖出错行重试。云端仍无 Android SDK，未跑 `assembleDebug`。
- **PR #2**：无新更新，仍为 open，head `4a59591fcac4`，上次更新 13:18。
- **来源**：GitHub PR #3 / PR #2 正文与元数据。此前「三帧约定是否已推进 PR #3」以本条更新：PR 正文已写入该约定；两条 PR 均尚未合入 master，本机编译与冒烟仍待用户授权。

---

## 2026-11-10 · 三帧约定已推进 PR #3，该条待确认划掉

- **决定**：Trae @流程记录员 确认，三帧布局已在提交 `dee9adf`（完整 SHA `dee9adfb09e4`）写入 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3)：四档状态固定在输入栏上方，确认卡只在「等待确认」，出错行右侧只有「重试」，Emotion Ball 顶栏开关、默认关。UI设计大师称界面没有未决项。此前「三帧约定是否已推进 PR #3」这条待确认划掉。
- **仍未完成**：两条 PR 均未合入 master；本机尚未在 `E:\AI\Student Spending Bill` 编译；云端无 Android SDK，未跑 `assembleDebug`。用户已回来，Trae 称授权卡出现后即可跑本地构建并走演示流程。演示视频与不超过 30 页的 PDF 仍等主线跑通后再做。提交是拆开的，不是一次堆进仓库。
- **协议核对**：已核工程分支 `cursor/central-student-assistant-a926` 上的 `docs/assistant/think-protocol.v0.1.md`，文中仍写文字状态「只能是下面三档」，列举为思考 / 调用工具 / 等待确认，没有「出错」节。同文件在 [PR #2](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/2) 分支 `cursor/assistant-docs-v0.1-d367` 已含四档（含出错失败态、状态机行、JSONL 出错示例与 `reason` 枚举）。因此 Trae 所称「把 PR #2 的出错协议再合进工程分支」与文件核对一致。DSH 称 Prompt v0.1（含四档）已在 PR #3：工程分支确有 v0.1 文件，但该协议正文尚未带上「出错」补丁，不以群述写成协议已齐。
- **单测数量**：Trae 群内称云端 18 个单测过了；PR #3 正文（13:51）写的是 `AssistantCoreTest` 19 个（含出错行重试）。两说不一致，待核对，不把其中任一写成已核定数。
- **来源**：学生助手比赛群，UI设计大师、Trae助手、主管大人、DSH助手，2026-11-10；GitHub 两分支 `think-protocol.v0.1.md` 原文核对。

---

## 2026-11-10 · DSH 更正：工程分支 think-protocol 仍为三档

- **决定 / 更正**：DSH 在群内更正前说：工程分支里的 `think-protocol` 仍是三档正文，「出错」补丁仍在 [PR #2](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/2)。请 Trae 按原计划再把该协议合进工程分支；DSH 不再另行改 `docs/assistant/`。此更正与此前文件核对一致。
- **并列陈述**：WorkBuddy 称文档和助手层已进 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3)（含 10 分钟复现指南、许可页、交赛清单和决策日志），两条 PR 仍开着、未合入 master，并称 PR #2 内容已合进 #3。后一句以 DSH 更正与文件核对为准：v0.1 文件在工程分支，但「出错」补丁尚未写入该分支的协议正文。
- **仍未完成**：本机 Android 编译仍待用户授权；演示视频与 30 页 PDF 仍等主线真机跑通后再做。用户在群内询问授权入口（2026-11-10 16:14 Asia/Shanghai，UTC+8）。
- **来源**：学生助手比赛群，WorkBuddy助手、DSH助手、用户，2026-11-10。

---

## 2026-11-10 · Trae 已派工程代理再合「出错」协议，本机检查已发起

- **进行中**：Trae 已让同一条工程云端代理（[Central assistant layer on Campus Bill](https://cursor.com/agents/bc-3f78ddb6-e3ab-5fa5-950c-a05460caa926)）把 [PR #2](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/2) 的「出错」协议再合进 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3)，合完后会再报提交号。此时尚未报 SHA，不以已合入论。
- **本机编译**：Trae 称本机操作当时尚未发起，随即开始检查 `E:\AI\Student Spending Bill`，授权卡会在发起后弹出。主管大人请用户先打开 [Trae助手](grokbot://app/v1/agent?id=cf7c01b9-db79-4c26-bb17-405fd995e965) 的对话看卡；Trae 在本群称卡片会在「这个对话」里出现。两处表述并存，待用户实际看到卡片后再记落点。截至本条，未见「用户已点允许」或本机编译已完成的报告。
- **来源**：学生助手比赛群，主管大人、WorkBuddy助手、DSH助手、Trae助手，2026-11-10 16:17（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · 本机检查已完成且授权通过，不在原目录直接编译

- **事实**：Trae 称本机检查已经跑完，授权已通过；用户不用再找授权卡，也不用切到 Trae 的一对一对话。
- **本机状态**：你电脑上的项目仍在 `feat/ui-tokens-p0`（`005bd05`），不是工程分支 `cursor/central-student-assistant-a926`；当前目录还有未提交改动。此点与此前只读检查一致。
- **下一步（尚未完成）**：Trae 不会在原目录直接编译，将另开一个工作树只检出工程分支再编，不碰现有这套本地改动。若那时再弹出授权卡，需再点允许。截至本条，未见工作树已建好或 `assembleDebug` 已跑通的报告，不以本机编译完成论。
- **来源**：学生助手比赛群，Trae助手，2026-11-10 16:19（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · 助手层 APK 尚未产出，本机笔记本当时已连上

- **事实**：主管大人确认助手层安装包还没有；仓库已发布的 [v1.1.apk](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/releases/download/v1.1/1.1.apk) 是之前的账单 App，不含这次的中央助手。工程仍在 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3)，本机编译要用独立工作树。主管大人请 Trae 在工程分支打出 `assembleDebug` 的 APK，并把路径发到群里。截至本条，群内仍未见该 APK 路径。
- **本机连通**：主管大人称「你电脑这会儿像是离线的」。用户回复「我电脑开着呢」。流程记录员于 2026-11-10 16:53（Asia/Shanghai，UTC+8）核对注册设备：`LAPTOP-EFA6JB48` 当时为已连接；`HONOR 80` 当时为未连接。电脑开着与通道是否连上不是同一件事，本条只记核对当时的状态，不回溯主管大人发言时的连通情况。
- **来源**：学生助手比赛群，主管大人、用户，2026-11-10 16:53（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · 主管大人纠正离线误判，Trae 称调试包已编好

- **更正**：主管大人确认此前「电脑像是离线」为误判，`LAPTOP-EFA6JB48` 当时是连着的。请 Trae 立刻用独立工作树检出工程分支并编译 APK，不要动原来的 `feat/ui-tokens-p0` 和未提交改动；若再弹出授权卡，在本群对话里点允许。
- **编译报告**：Trae 称「有，我把刚编好的调试包装到这里」。截至本条，该条消息正文未给出 APK 路径、工作树位置、对应提交号或 `assembleDebug` 是否成功的日志。不以已核验、可安装的助手层 APK 论，待路径或附件落点明确后再补记。
- **来源**：学生助手比赛群，主管大人、Trae助手，2026-11-10 16:56（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · Trae 称助手层调试 APK 已编出，提交 bced85f

- **编译报告（Trae 所称）**：助手层调试包已编好，不是仓库已发布的 v1.1；对应提交 `bced85f`，不用再打一遍。聊天发文件有 25 MB 上限，该包大约 25.4 MB，因此发不进群。请用户直接安装桌面上的 `C:\Users\ZXM\Desktop\app-debug.apk`；原文件在 `E:\AI\Student-Spending-Bill-pr3\app\build\outputs\apk\debug\app-debug.apk`。独立编译目录为 `E:\AI\Student-Spending-Bill-pr3`。流程记录员未在本机打开或校验该 APK 文件，路径与体积以 Trae 群述为准。
- **GitHub 核对（2026-11-10 16:57，Asia/Shanghai，UTC+8）**：[PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3) 仍开放、未合入 master，`mergeable_state` 为 clean。头提交为 [`bced85fa3601`](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/commit/bced85fa3601231243c692576b921472811681bd)（短 SHA `bced85f`），与 Trae 所说提交号一致。PR 于 16:26 更新，当时为 15 个提交，+6762/−45，75 个文件。该头提交说明为 `fix(assistant): declare JSONException on the stream message builder`，改动 `DeepSeekStreamClient.java`（+2/−1）；作者 Cursor Agent，提交时间 16:26（由 2026-11-10T08:26:32Z 换算）。PR 正文仍写云端环境无 Android SDK、未在云端跑 `assembleDebug`，与本机已编译不矛盾。
- **协议核对**：已核该头提交上的 `docs/assistant/think-protocol.v0.1.md` 已含「出错」协议（失败态说明、状态机行、JSONL 出错示例与 `reason` 枚举）。此前「工程分支协议仍为三档、需再合入 PR #2 补丁」一条，在文件层面已划掉。PR #2 本身仍对 master 开放，是否已合入 master 仍为否。
- **仍待确认**：真机是否已安装并冒烟通过该调试包；原目录 `E:\AI\Student Spending Bill`（`feat/ui-tokens-p0` / `005bd05` 及未提交改动）是否确实未被改动。演示视频与不超过 30 页的 PDF 仍等主线跑通后再做。
- **来源**：学生助手比赛群，Trae助手，2026-11-10 16:57（Asia/Shanghai，UTC+8）；GitHub PR #3 与提交 `bced85f`、`think-protocol.v0.1.md` 原文核对。

---

## 2026-11-10 · 用户提出首页常驻 bot 并替换消费建议面板（待确认）

- **用户原意（尚未拍板）**：针对下方 AI 页面、经上面「球」打开的 bot，用户希望它常驻在首页，而不是通过开关打开；要占据一定空间，尽量大一些；可以把「AI 消费建议面板」替换成 bot；交互上，用户滑动、点击时 bot 视线会跟随。
- **与已拍板项的冲突**：群内此前已冻结 Emotion Ball 为可选、默认关、仅用顶栏「球 / 关」开关、不居中占屏，且不能替代对话页文字状态。本次请求涉及取消开关、改到首页常驻并加大、替换消费建议面板、增加视线跟随，属于范围变更，不以已决定或已实现论。
- **处理**：先记入待确认，等主管大人 / UI 侧就是否改范围表态后再更新状态。本条不改产品范围，也不安排实现。
- **来源**：学生助手比赛群，用户，2026-11-10 17:15（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · 原「默认关闭 + 顶栏开关」作废，AI 页主屏常驻 Emotion Ball

- **决定（主管大人拍板）**：按用户反馈改。AI 页主屏常驻 Emotion Ball，不再用开关；用它替换「AI消费建议」面板并尽量做大；滑动或点击时视线跟随。先不放到记账首页。工具确认主链路不动。原「默认关闭 + 顶栏开关」作废。仍用 aora-bot 的 WebView 球，须署名许可。
- **分工**：UI设计大师先出这一块的布局和视线跟随规格；Trae 按规格改工程分支，不改原来的 `feat/ui-tokens-p0`。DSH 称视线跟随可补进协议，等版式拍板后再改，不另开文档分支。WorkBuddy 称确认后会改复现指南和决策日志。
- **仍待确认**：UI设计大师建议对话页（演示主路径）球也常驻但缩小，不能挤掉流式输出、四档状态和确认卡，需主管大人同意后才记成约定。UI 消息以「首页示意如下」结尾；本条未转写附图内容，规格以随后给出的布局说明为准。实现尚未完成，不以已落地论。
- **用词**：用户原提「常驻在首页」；主管大人明确为「AI 页主屏」「先不放到记账首页」。UI 表述里仍用「首页」，与拍板口径并存，不把记账首页常驻写成已同意。
- **来源**：学生助手比赛群，DSH助手、Trae助手、WorkBuddy助手、主管大人、UI设计大师，2026-11-10 17:16（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · AI 页大球规格已冻，对话页小球已同意；底部切换动画待方案

- **待确认已去掉（主管大人）**：旧的默认关、顶栏开关、不占主区域，对 AI 页不再适用。主管再次确认：不再默认关闭、不再靠顶栏开关，AI 页上可以占住「消费建议」那块主区域并尽量做大。「首页」仍指带「AI消费建议」的那屏；先不放到记账首页。
- **对话页**：主管大人同意 UI 的分工——那一屏用大球换掉消费建议面板；对话页（演示主路径）可以常驻小球，但不能挤掉流式输出、四档状态和确认卡。此前「对话页缩小常驻待确认」一条划掉，记为已同意。Trae 按这个改工程分支。
- **UI 作废与口径**：UI设计大师称刚才那帧记账首页示意作废，只改 AI 页，记账首页不动；并说明随后示意是 AI 页，不是首页。本条未转写附图。
- **AI 页布局规格（给 Trae，DSH 可按此改 emotion-map）**：去掉顶栏开关；原「AI消费建议」位置换成常驻球面板，左右各留 16dp，高度约窗口的 40%（上限 320dp）；对话区占剩余空间；四档状态仍固定在输入栏上方；确认卡仍只出现在「等待确认」并留在消息流里；键盘弹出或出现确认卡时，球面板压缩到最低 160dp，不能挡住状态行和确认卡。
- **视线跟随规格**：仍用 aora-bot `emotion-ball` 的 WebView。页面上滑动或点击时，把坐标相对球心归一化到 [-1, 1]，调用 `ball.setGaze(nx, ny)`；松手后约 400ms 回到正视；页面不可见或进后台时 `setActive(false)` 停帧。许可继续在关于页署名。协议等此版式后再由 DSH 补视线跟随，不另开文档分支。WorkBuddy 按已确认范围改复现指南和决策日志。
- **新请求（尚未拍板）**：用户询问「点击下方切换页面的切换动画可以换一个吗」，并请 UI设计大师给几个切换页面过渡动画方案。此为新项，不记成已改动画，方案与是否采用均待 UI 给出并经确认。
- **来源**：学生助手比赛群，主管大人、UI设计大师、用户，2026-11-10 17:25（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · 对话页不再另放小球；切页动画四方案待选

- **版式约定（主管大人拍板，取代「对话页另放小球」）**：就按 UI设计大师此前给出的 AI 页规格改，记账首页不动。对话页不再另放一颗小球，而是同一页里键盘或确认卡出来时球面板压到最低 160dp，不能挡状态行和确认卡。此前「对话页可常驻小球」的同意作废，以本条为准。DSH 可以改 `emotion-map`；Trae 按这个改工程分支。实现尚未完成，不以已落地论。
- **切页动画**：主管大人同意可以换，要求按现在的 iOS 底栏、系统感给 2–3 个底栏切页过渡方案（含时长和动效），选一个再让 Trae 改；先不要挡住 AI 页球面板那条主改动。
- **UI 已给方案（尚未选定）**：UI 建议换得克制，别跟对话滑动和球的视线跟随抢注意力。更推荐交叉淡入淡出：约 220ms，新旧页对向淡化，不做位移，也最接近现在的 Grouped 气质。另外可选：淡入加 8–12dp 上浮（约 240ms）；按 Tab 左右滑（约 300ms，方向清楚，但容易像翻页）；轻微缩放淡入（96% 到 100%，约 220ms，略重）。群内已出选择题，尚未有选定记录。
- **来源**：学生助手比赛群，主管大人、UI设计大师，2026-11-10 17:26（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · WorkBuddy 已改复现指南与许可页（约定已写入，工程尚未落地）

- **事实**：按主管大人要求，已更新本机草稿 `/workspace/student-assistant-docs/docs/assistant/REPRODUCE.md` 与 `THIRD_PARTY.md`：去掉「默认可关 / 不占中屏」的旧表述，改为 AI 页主屏常驻大球（替换「AI消费建议」、无顶栏开关、先不放到记账首页）；同一页里键盘或确认卡出现时球面板压缩到最低 160dp，不另在对话页放小球；视线跟随写入演示说明。指南注明此为已拍板约定，工程落地前评测机上可能仍是旧开关，不以已实现论。
- **对齐**：文档按上一条「对话页不再另放小球；切页动画四方案待选」书写，不再采用更早的「对话页可常驻小球」。切页动画尚未选定，未写入复现指南。
- **合入**：草稿仍在上述本机目录，请 Trae 合进工程分支 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3)，不要改 `feat/ui-tokens-p0`。
- **来源**：WorkBuddy助手，2026-11-10 17:26（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · 切页动画约定为交叉淡入淡出；再次确认不另放小球

- **切页动画（已拍板，记成约定）**：用户在卡片上选择「用交叉淡入淡出」。主管大人随后拍板：切页就用交叉淡入淡出，约 220ms，新旧页对向淡化，不做位移，并请流程记录员把切页动画记成约定。UI 对该推荐项还给出曲线 `cubic-bezier(0.2, 0, 0, 1)`、无位移、最接近系统；主管原文未单独点名曲线，曲线作为该方案的 UI 规格一并记下。
- **版式更正（再次确认）**：主管大人补正：对话页不再另放一颗常驻小球，而是同一 AI 页里键盘或确认卡出来时，球面板压到最低 160dp。UI 已按此冻结版式。此条与上一则约定一致，以「不另放小球」为准。
- **实现安排**：主管大人称 Trae 可以和改进工程分支一起改切页动画，同时按同一 AI 页球面板压缩来做，不要另做小球。UI 称切页动画等球面板落地后再改，先不挡主改动。两处时间安排口径并存：主改动仍是球面板，切页已有约定，是否同一次提交以工程侧实际为准。Trae 已称先不动切页动画，并按当时冻住的规格在 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3) 改球，进度在 [工程代理](https://cursor.com/agents/bc-3f78ddb6-e3ab-5fa5-950c-a05460caa926)；其表述里仍写「对话页不挤主链路的小球」，与最新约定不符，执行应以「不另放小球、同一页压缩至最低 160dp」为准，不以该句为现行约定。实现尚未完成。
- **UI 后补的另两套未选方案**（仅备案，不采用）：淡入加 8dp 上浮，240ms，同一条曲线；页面交叉淡入，底栏图标从 0.86 缩放到 1，200ms。
- **来源**：学生助手比赛群，Trae助手、用户、主管大人、UI设计大师，2026-11-10 17:26（Asia/Shanghai，UTC+8）。

---

## 2026-11-10 · 切页曲线一并冻上；emotion-map 已改待合入 PR #3

- **切页约定补全**：主管大人确认不必再选，按交叉淡入淡出补上 220ms、`cubic-bezier(0.2, 0, 0, 1)`、无位移来做。用户在第二张卡片上也回复「用交叉淡入淡出，220ms，无位移」。UI 同步冻结同一参数。时间安排现已对齐：球面板先落地，切页动画可以随后再改，先不挡主改动。
- **版式执行口径**：Trae 已按最新约定对齐：只改 AI 页这一屏，对话页不再另放小球，键盘或确认卡出来时把同一块球面板压到最低 160dp，并按此改工程分支。UI 再次提醒同一口径。
- **协议**：DSH 称 `emotion-map` 已按冻定版式改好（AI 页常驻、替换消费建议面板、占位与 `setGaze` / 回正视 / 停帧），不另开版本号和文档分支，请 Trae 一并合进 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3)。流程记录员已核箱内文件 `/workspace/campus-assistant-docs/docs/assistant/emotion-map.v0.1.md`（mtime 17:26）正文与冻定规格一致，同目录 `docs/assistant/README.md` 亦有相应说明。【序号】16 目前在箱内追加稿 `/workspace/campus-assistant-docs/APPEND_prompt_evolution_gaze.md`（标明文档修订已完成、未改 Java；拟改 `emotion-map.v0.1.md`、`docs/assistant/README.md`、`prompt_evolution.md`）；该共享目录下未见已写入的 `prompt_evolution.md`。截至本条，尚未确认这些文档已合入 PR #3，不以仓库已更新论。
- **来源**：学生助手比赛群，DSH助手、主管大人、UI设计大师、Trae助手、用户，2026-11-10 17:27（Asia/Shanghai，UTC+8）；箱内 `campus-assistant-docs` 文件核对。
