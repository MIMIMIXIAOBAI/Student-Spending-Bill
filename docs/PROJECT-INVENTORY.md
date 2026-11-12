# 学生消费账单 APP · 项目全量识别报告

- 生成时间：2026-11-12（Asia/Shanghai）；同日完成文档归一
- 仓库：`https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill.git`，本地目录 `E:\AI\Student-Spending-Bill-pr3`
- 识别基线：`HEAD = ae6d21645552fd0e6cc860a4815eadde30da2df8`（`feat(home): replace the AI advice card with the Emotion Ball`，2026-11-10，detached 无本地分支）
- 性质：本文件是**全项目识别的唯一权威报告**。2026-11-12 文档归一后，除本文件外仅修改 `README.md`、`docs/assistant/REPRODUCE.md`，并删除被合并的 `docs/PROJECT_IDENTIFICATION_2026-11-12.md`；**未改 `app/src` 任何文件**。第 15 节给出前后 `git status` 与 `app/src` 零改动的复核记录。

---

## 0. 一句话结论

这是一个**纯 Java + XML/ViewBinding 的 Android 本地记账 App**（单模块 `:app`，包名 `com.campusbill.app`），核心是「手动记账 + 四种账单导入 + 图表化消费分析 + 预算 + 本地/远程双模 AI 消费助手」。数据只落本机 Room（`student_bill.db`），全应用唯一权限是 `INTERNET`，且只服务于 AI 对话页。仓库同时是比赛作品：仓库内嵌了一整套「中央学生助手」协议文档（Prompt / 工具 schema / 意图控制 / 过程留痕）与其 Java 实现，以及一个 WebView 承载的 3D 情绪球（第三方 Emotion Ball 上游代码，非商用）。

规模：`app/src/main/java` 92 个 `.java`、`app/src/test` 7 个、`res` 74 个 `.xml` + `AndroidManifest.xml`、`assets` 13 个文件；**无任何 `.kt` 文件**。

## 0.1 计数口径与复现边界（唯一口径）

**计数口径**（全部可复算；`main` 与 `test` 分开，避免把 99 当成 `main` 数）：

- `app/src/main/java`：**92** 个 `.java`
- `app/src/test`：**7** 个 `.java`，共 **49** 个 `@Test`（6+26+5+2+3+4+3）
- Java 合计：**99** = 92 main + 7 test
- `app/src/main/res`：**74** 个 `.xml`；`AndroidManifest.xml` **1** 个 → XML 合计 **75**
- `app/src/main/assets`：**13** 个文件
- 根目录一级条目：**16** 项（§2.1 口径：含隐藏文件，排除 `build/`、`.gradle/`、`.cache/`、`.git/`，`gradlew` 与 `gradlew.bat` 合并为 1 项；原始 `Get-ChildItem -Force` 为 20 项）
- `docs/`：归一后 **26** 个文件（含本报告；不含本报告 25 个）
- 本报告自身：**以实物为准**——初版实测 586 行 / 58,790 B，本次归一后为 **642 行 / 68,157 B**（UTF-8 LF）。后续再改请重新复算，不要把行数当常量。

```powershell
(Get-ChildItem app\src\main\java -Recurse -Filter *.java).Count                       # 92
(Get-ChildItem app\src\test -Recurse -Filter *.java).Count                            # 7
(Get-ChildItem app\src\test -Recurse -Filter *.java | Select-String '@Test').Count     # 49
(Get-ChildItem app\src\main\res -Recurse -Filter *.xml).Count                         # 74
(Get-ChildItem app\src\main\assets -Recurse -File).Count                              # 13
((Get-ChildItem -Force | Where-Object { $_.Name -notin '.git','build','.gradle','.cache' }).Count - 1)  # 16（17-1，gradlew 对合并）
(Get-ChildItem docs -Recurse -File).Count                                             # 26
```

**复现边界（快照 `ae6d216`；T5 已提交到 `feat/assistant-competition`）：HEAD `ae6d216` 不含 19 个未跟踪条目（其中 23 个功能源码文件），clone 旧提交拿到的是旧 AI 页**

- 本报告识别的是**工作树**，不是 `HEAD`。识别开始时 `git status --porcelain` 为 **219 行**（200 已修改 / 17 未跟踪 / 2 已删除）；加上两份新报告后，未跟踪变为 **19 个条目**。`git ls-tree -r HEAD` 只有 **217 个文件**。
- **19 个未跟踪条目**是 `git status` 的折叠口径，**不是源码文件数**；其中的功能源码展开为 **23 个文件**：`chat/session/` 6 个（`ChatSessionBook`、`ChatSessionRoom`、`TokenEstimate`、`ModelWindows`、`ContextUsage`、`MemoryHandoff`）、`ui/` 6 个（`ChatBallFade`、`ChatBallMetrics`、`ChatBallStage`、`ChatInputChrome`、`BallAttitudeGaze`、`AttitudeGazeMath`）、`data/` 5 个（`ChatMemoryEntity`、`ChatMessageEntity`、`ChatSessionDao`、`ChatSessionEntity`、`SelectedMonthStore`）、`chat/ai/assistant/ReplyGate.java` 1 个，加 `app/src/test` 5 个。19 个条目另含 `.cache/` 与 3 个文档（含本报告初版与另一份被合并报告）。
- `git ls-tree -r HEAD` 对 `chat/session/`、`ui/ChatBall*`、`ui/BallAttitudeGaze*`、`ui/AttitudeGazeMath*`、`data/Chat*Entity`、`data/SelectedMonthStore`、`chat/ai/assistant/ReplyGate` 的命中数为 **0**。
- 含义：**评委从 HEAD / PR #3 当前提交 clone 到的是旧 AI 页**——没有会话持久化、情绪球交互层、`ReplyGate` 插话门控与「当月」月份联动。录屏/报告与本仓库一致的前提是先由 T5 把工作树提交成非 detached 分支，并在**干净 clone** 上重建；未提交前，不能用「clone 即复现」描述本报告的功能范围。
- 归一前 `git status -sb` 首行为 `## HEAD (no branch)`（detached）；T5 已建 `feat/assistant-competition` 并提交（见下条）。
- **并发提交进展（2026-11-12 12:42 复核）**：T5 已把 app 层提交为 `feat/assistant-competition` 的 5 个 commit（`4d3242f`、`7344dad`、`8224147`、`a7e34e8`、`c7bafea`），HEAD 为 `c7bafea`；上述 23 个功能文件已入库，`docs/`（含本报告、T4 `DEMO-SCRIPT.md`、`session.v0.1.md`）仍未提交。故「clone 到旧 AI 页」只适用于 `ae6d216` / PR #3 旧提交；复现应 clone 该分支。T6 的预算修复与 `BillAnalysisUtilsTest` 已随 `c7bafea` 入库，HEAD 实测 **8 个测试类 / 50 个 @Test**；本报告 §0.1 的 7/49 与 §5–§8 的逐文件清单是 `ae6d216` 快照口径。

---

## 1. 技术栈与构建配置（实测值）

| 维度 | 实测值 | 证据 |
|---|---|---|
| 语言 | 纯 Java 17（源码内 `java.time`、lambda、switch 表达式） | 无 `.kt`；`app/build.gradle` `compileOptions` Java 17 |
| 构建 | Gradle Wrapper **9.6.0** + AGP **9.4.0** | `gradle/wrapper/gradle-wrapper.properties`、根 `build.gradle` |
| SDK | `compileSdk = 37`、`minSdk = 29`（Android 10）、`targetSdk = 36` | `app/build.gradle` |
| 版本 | `versionCode 1` / `versionName "1.0"`，`applicationId com.campusbill.app` | `app/build.gradle` |
| UI | XML 布局 + `buildFeatures { viewBinding = true }`；`Theme.AppCompat.Light.NoActionBar`，未引入 Material 组件库 | `app/build.gradle`、`res/values/themes.xml` |
| 权限 | 仅 `android.permission.INTERNET`；`allowBackup=false` | `AndroidManifest.xml` |
| 屏幕 | 10 个 Activity 全部 `screenOrientation="portrait"` | `AndroidManifest.xml` |
| 数据 | Room 2.6.1（annotationProcessor），库名 `student_bill.db`，`version = 3`，`exportSchema = false` | `data/AppDatabase.java` |
| 图表 | MPAndroidChart `v3.1.0`（jitpack）+ 自研 `IncomeTimelineView`、`CandyBarChartRenderer` | `app/build.gradle`、`chart/` |
| 导入解析 | OpenCSV 5.9（CSV）、PDFBox-Android 2.0.27.0（PDF）、自写 Zip+XmlPullParser（XLSX）、手写文本切分（粘贴） | `imports/`、`app/build.gradle` |
| 网络 | `HttpURLConnection` 直连 OpenAI 兼容 `/chat/completions`（SSE 流式），无 OkHttp/Retrofit | `chat/ai/AiRemoteClient.java`、`DeepSeekStreamClient.java` |
| 测试 | JUnit 4.13.2 + `org.json:20240303`，纯 JVM 单元测试 7 个类，不依赖设备 | `app/build.gradle`、`app/src/test` |
| 文档同步 | 自定义 Gradle 任务 `syncAssistantDocs`（`Copy`）在 `preBuild` 前把 `docs/assistant` 的 5 个协议文件复制进 `assets/assistant` | `app/build.gradle` |

> 构建脚本里有一处刻意的兼容处理：`settings.gradle` 在 `gradle.settingsEvaluated {}` 中**再补一次 jitpack 仓库**，注释说明是因为本机全局 Gradle 镜像脚本会清空仓库列表（`prompt_evolution.md` 第 1 条也记录了同一原因）。

---

## 2. 目录树

### 2.1 根目录一级条目

```
E:\AI\Student-Spending-Bill-pr3\
├── .gitignore                  # 17 行：构建产物、IDE、本机密钥、过程原始日志
├── README.md                   # 143 行：定位 / 功能表 / 合规 / 环境 / 依赖 / 构建 / 使用 / 结构
├── build.gradle                # 3 行：仅声明 AGP 9.4.0 插件不应用
├── settings.gradle             # 26 行：仓库与 include ':app'
├── gradle.properties           # 3 行：JVM 内存与 AndroidX 开关
├── gradlew / gradlew.bat       # Wrapper 启动脚本
├── local.properties            # 本机 SDK 路径（已被 .gitignore）
├── prompt_evolution.md         # 656 行：Prompt 迭代记录（逐序号任务 + 改动文件清单）
├── 给豆包的提示词.txt            # 61 行：第一版需求提示词原文
├── 制作过程.docx               # 1,724,214 B：参赛《制作过程文档》
├── app\                        # 唯一 Gradle 模块
├── docs\                       # 助手协议、过程留痕、UI 刷新设计与计划
├── gradle\                     # wrapper
├── Promat历史\                 # 早期 UI 刷新 Prompt 归档
├── screenshots\                # 10 张界面截图
└── 模拟账单\                   # 微信支付账单样例（PDF + XLSX）
```

（`build/`、`.gradle/`、`.cache/`、`.git/` 为构建/缓存/版本库目录，不计入源码条目。）

### 2.2 `app/` 与 `app/src/` 全量源码树

```
app\
├── build.gradle                                  # 模块构建脚本（含 syncAssistantDocs）
├── proguard-rules.pro                            # 仅 1 行占位注释
└── src\
    ├── main\
    │   ├── AndroidManifest.xml
    │   ├── assets\
    │   │   ├── assistant\
    │   │   │   ├── SYSTEM_PROMPT.v0.1.md
    │   │   │   ├── tools.schema.v0.1.json
    │   │   │   ├── intent-control.v0.1.md
    │   │   │   ├── think-protocol.v0.1.md
    │   │   │   └── emotion-map.v0.1.md
    │   │   └── emotion-ball\
    │   │       ├── host.html
    │   │       ├── ball.js
    │   │       ├── emotions.js
    │   │       ├── engine.js
    │   │       ├── rings.js
    │   │       ├── ATTRIBUTION.txt
    │   │       ├── UPSTREAM_LICENSE
    │   │       └── UPSTREAM_NOTICE.md
    │   ├── java\com\campusbill\app\
    │   │   ├── MainActivity.java
    │   │   ├── AddBillActivity.java
    │   │   ├── EditBillActivity.java
    │   │   ├── CalendarActivity.java
    │   │   ├── HistoryActivity.java
    │   │   ├── ImportActivity.java
    │   │   ├── IncomeDetailActivity.java
    │   │   ├── MerchantRankActivity.java
    │   │   ├── AiChatActivity.java
    │   │   ├── AssistantProcessActivity.java
    │   │   ├── DayBillAdapter.java
    │   │   ├── HistoryAdapter.java
    │   │   ├── MerchantRankAdapter.java
    │   │   ├── chart\  CandyBarChartRenderer.java  IncomeTimelineView.java
    │   │   ├── chat\   ChatAdapter.java  ChatMessage.java
    │   │   │   ├── ai\  AiConfig.java  AiRemoteClient.java  AiReplyConstants.java
    │   │   │   │        BillAnalysisUtils.java  BillContextBuilder.java
    │   │   │   │        DeepSeekKeyProbe.java  DeepSeekKeyStore.java  DeepSeekStreamClient.java
    │   │   │   │        IntentMatcher.java  ReplyGenerator.java
    │   │   │   │   ├── assistant\  30 个文件（见 5.5）
    │   │   │   │   ├── presets\  AiPresets.java
    │   │   │   │   └── skills\   AiSkill.java  AiSkills.java
    │   │   │   └── session\  ChatSessionBook.java  ChatSessionRoom.java  ContextUsage.java
    │   │   │                 MemoryHandoff.java  ModelWindows.java  TokenEstimate.java
    │   │   ├── data\   AppDatabase.java  BillDao.java  BillEntity.java  CategoryAmount.java
    │   │   │           CategoryClassifier.java  ChatMemoryEntity.java  ChatMessageEntity.java
    │   │   │           ChatSessionDao.java  ChatSessionEntity.java  DBHelper.java
    │   │   │           ImportBatchEntity.java  MerchantAmount.java  SelectedMonthStore.java
    │   │   ├── imports\  BillCsvParser.java  BillPasteParser.java  BillPdfParser.java
    │   │   │              BillXlsxParser.java  ImportResult.java
    │   │   └── ui\   AttitudeGazeMath.java  BallAttitudeGaze.java  BottomTabHelper.java
    │   │             ChatBallFade.java  ChatBallMetrics.java  ChatBallStage.java
    │   │             ChatInputChrome.java  PressScaleHelper.java
    │   └── res\
    │       ├── anim\        tab_fade_in.xml  tab_fade_out.xml
    │       ├── interpolator\ tab_crossfade.xml
    │       ├── drawable\    44 个（见 6.2）
    │       ├── layout\      21 个（见 6.1）
    │       ├── mipmap-anydpi-v26\ ic_launcher.xml
    │       └── values\      arrays.xml  colors.xml  dimens.xml  strings.xml  themes.xml
    └── test\
        ├── java\com\campusbill\app\
        │   ├── chat\ai\AiReplyTest.java
        │   ├── chat\ai\assistant\AssistantCoreTest.java
        │   ├── chat\session\SessionRulesTest.java
        │   └── ui\  AttitudeGazeMathTest.java  ChatBallFadeTest.java
        │            ChatBallMetricsTest.java  ChatInputChromeTest.java
        └── resources\                       # 空目录（无测试资源）
```

> 说明：`chat/ai/assistant` 下 30 个文件为 `AssistantCallException`、`AssistantController`、`AssistantDocuments`、`AssistantHost`、`AssistantIntentPlanner`、`AssistantStatus`、`AssistantStatusMachine`、`BillDraft`、`BillLedger`、`ChatTurn`、`DiaryGate`、`DiaryNoteStore`、`EmotionHooks`、`FailureClassifier`、`FailureKind`、`FileProcessLog`、`FortuneComposer`、`LedgerBill`、`ProcessEvents`、`ReplyGate`、`RoomBillLedger`、`SecretRedactor`、`SpendQuery`、`SseAccumulator`、`StreamResult`、`SystemPromptBody`、`ThinkTagSplitter`、`ToolCall`、`ToolSchemaCatalog`、`WeekRange`。

---

## 3. 根目录一级条目职责

| 条目 | 类型 | 职责 |
|---|---|---|
| `app/` | 目录 | 唯一 Gradle 模块，含全部源码、资源、assets 与单元测试。 |
| `docs/` | 目录 | 比赛层「中央学生助手」的协议文档（`assistant/`）、过程留痕（`assistant/process/`）、2026-11-04 UI 系统化升级的设计与计划归档（`superpowers/`）。 |
| `gradle/` | 目录 | Gradle Wrapper：`gradle-wrapper.jar`（48,462 B）+ `gradle-wrapper.properties`（锁定 9.6.0）。 |
| `Promat历史/` | 目录 | 早期 UI 刷新提示词归档，当前仅 `ui-refresh-2026-11-04/P1_trae_prompt.md`。 |
| `screenshots/` | 目录 | 10 张界面截图（主界面上下半屏、消费明细、日历、收入详情、记一笔、导入、商户排行、AI 对话、历史），README 与制作过程文档引用，不做运行期读取。 |
| `模拟账单/` | 目录 | 导入功能的真实样例数据：微信支付账单流水 PDF（482,522 B）与 XLSX（22,289 B），仅供测试/演示。 |
| `.gitignore` | 配置 | 忽略 `*.iml`、`.gradle/`、`local.properties`、`.idea/`、`.DS_Store`、`build/`、`captures/`、`.externalNativeBuild/`、`.cxx/`、`*.jks`、`*.keystore`、`secrets.properties`、`docs/assistant/process/runtime/`（后者为设备原始 JSONL 过程日志，可能含明细）。 |
| `README.md` | 文档 | 项目主说明：定位、8 大功能、5 条合规设计、环境要求（JDK 17 / API 37 / Gradle 9.6.0）、依赖清单、三平台构建命令、使用步骤、目录结构、测试情况、比赛层入口。 |
| `build.gradle` | 构建 | 根脚本只声明 `com.android.application` 插件版本 `9.4.0` 并 `apply false`，插件在各模块内应用。 |
| `settings.gradle` | 构建 | 配置 `pluginManagement`（google/mavenCentral/gradlePluginPortal）与 `dependencyResolutionManagement`（`FAIL_ON_PROJECT_REPOS` + google/mavenCentral/jitpack），`rootProject.name='StudentSpendingBill'`，`include ':app'`。 |
| `gradle.properties` | 构建 | `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`（保证中文源码编译不乱码）、`android.useAndroidX=true`、`android.nonTransitiveRClass=true`。 |
| `gradlew` / `gradlew.bat` | 构建 | 标准 Wrapper 启动脚本（POSIX / Windows），无需本机预装 Gradle。 |
| `local.properties` | 本机配置 | 唯一一行 `sdk.dir=C:\Users\ZXM\AppData\Local\Android\Sdk`；已被 `.gitignore` 排除，属本机私有配置。 |
| `prompt_evolution.md` | 文档 | 656 行「Prompt 迭代记录」：按【序号】逐条记录每次开发任务的时间、任务名、完成情况（含构建/测试结论）、改动文件清单，是理解仓库演进顺序的时间线档案。 |
| `给豆包的提示词.txt` | 文档 | 61 行第一版需求提示词原文（交给豆包优化前的手写规格）：UI 单页滚动布局、AI 面板、预算面板蓝/红白渐变、每日与分类柱状图、收入饼图 + 纵向时间轴、商户排行、消费日历、编辑账单、导入账单等。 |
| `制作过程.docx` | 文档（二进制） | 参赛用《学生智能消费账单 APP 制作过程文档》，74 段、8 个一级章节：制作起点（Trae/豆包/DSH）、项目详情与使用方式、项目亮点、AI 测试技术落地、关键技术、截图清单、发表/获奖说明、原创性说明。 |

---

## 4. 构建与配置文件职责

| 文件 | 职责 / 关键内容 |
|---|---|
| `app/build.gradle` | 模块构建：`namespace com.campusbill.app`、`compileSdk 37`、`minSdk 29`、`targetSdk 36`、Java 17、`viewBinding = true`；release 不混淆（`minifyEnabled = false`，仅接 `proguard-android-optimize.txt` + `proguard-rules.pro`）；依赖 appcompat 1.7.1 / constraintlayout 2.2.1 / recyclerview 1.3.2 / room 2.6.1 / opencsv 5.9 / pdfbox-android 2.0.27.0 / MPAndroidChart v3.1.0 / junit 4.13.2 / org.json 20240303；注册 `syncAssistantDocs` 拷贝任务并挂到 `preBuild`。 |
| `app/proguard-rules.pro` | 仅一行占位注释；因 release 未开启 minify，当前不参与实际混淆。 |
| `app/src/main/AndroidManifest.xml` | 69 行：唯一权限 `INTERNET`；`allowBackup=false`、`supportsRtl`、`theme=@style/Theme.StudentBill`、自适应图标；注册 10 个 Activity（`MainActivity` 为 LAUNCHER、`exported=true`、`singleTop`；`AiChatActivity` 额外 `adjustResize`），全部锁竖屏。 |
| `gradle/wrapper/gradle-wrapper.properties` | 锁定 `gradle-9.6.0-bin.zip`；`networkTimeout=10000`、`retries=0`、`retryBackOffMs=500`、`validateDistributionUrl=false`（便于内网/镜像环境下载）。 |

---

## 5. `app/src/main/java` 逐文件职责（92 个）

### 5.1 根包 `com.campusbill.app`（13）— Activity 与列表适配器

| 文件 | 职责 |
|---|---|
| `MainActivity.java`（653 行） | 首页：顶部 AI 面板已替换为常驻情绪球（轻点进对话、手机姿态驱动视线），下方为预算面板（输入并持久化当月预算、剩余/超支与蓝红渐变）、图表面板（每日折线 + 分类柱状、`MPAndroidChart`）、各功能入口；`onResume` 汇总刷新，月份切换弹窗只列库中已有月份。 |
| `AddBillActivity.java` | 手动记一笔：支出/收入切换后分类下拉联动，录入金额、分类、商户、备注与日期时间选择器，保存进 Room 后返回。 |
| `EditBillActivity.java` | 编辑账单：按 id 载入并回填原账单（类型/分类/金额/商户/备注/日期时间），保存写回 Room 后返回上一页。 |
| `CalendarActivity.java`（365 行） | 消费日历：按月渲染日历网格，有收支的日期打点；点日期显示当日收入/支出汇总与账单列表，支持编辑、删除（二次确认）与新建；`singleTop` + `onNewIntent` 支持从底栏复用同一实例并切月。 |
| `HistoryActivity.java`（430 行） | 历史记录：把导入批次与账单混排成带分组头的列表，支持关键词搜索、全部/支出/收入筛选、月份筛选、编辑/删除单条、按批次整体清除，并统计总数与合计。 |
| `ImportActivity.java` | 账单导入页：粘贴导入（仅在按钮回调里读一次剪贴板）、CSV 导入、PDF/XLSX 导入三条通道，均通过 SAF 系统选择器拿 `Uri`；单线程 `ExecutorService` 解析后经 `CategoryClassifier` 细化分类、按批次入库；启动时 `PDFBoxResourceLoader.init`。 |
| `IncomeDetailActivity.java`（313 行） | 收入分类详情：本月收入饼图 + 图例 + 纵向（自下而上）收入时间轴 `IncomeTimelineView`，点节点弹窗看账单详情；按所选月份只统计收入。 |
| `MerchantRankActivity.java` | 商户消费排行：按所选月份聚合支出，按金额降序展示；`unknown` 商户合并为「未知商户」。 |
| `AiChatActivity.java`（1310 行，全项目最大） | AI 对话页：装配 `AssistantHost`/`AssistantController` 跑「思考 → 调工具 → 等待确认 → 出错」四态，渲染流式正文与 `<think>` 分离、确认卡、密钥/模型对话框、过程页入口、上下文占用百分比、历史会话抽屉；顶部情绪球面板（WebView）与键盘/确认卡的高度联动、姿态视线、点击穿透到消息列表。 |
| `AssistantProcessActivity.java` | 过程页：读取本机 JSONL 过程日志（Prompt 版本、工具参数与结果、确认记录、状态时间线）并逐行格式化展示，供评委点开核对。 |
| `DayBillAdapter.java` | 日历页当日账单列表适配器：收入行淡绿底、支出行淡红底，右侧编辑/删除按钮回调到 Activity。 |
| `HistoryAdapter.java` | 历史页双 ViewType 适配器：`TYPE_BATCH` 渲染导入批次头（名称/条数/时间/清除按钮），`TYPE_BILL` 渲染账单行（日期/分类/商户/金额/编辑/删除）。 |
| `MerchantRankAdapter.java` | 商户排行列表适配器：金额降序，前三名放大字号并用金/银/铜徽标，其余用普通徽标。 |

### 5.2 `chart`（2）— 图表自定义

| 文件 | 职责 |
|---|---|
| `CandyBarChartRenderer.java` | 继承 `BarChartRenderer`，重写 `drawDataSet` 实现「柱体圆角 + 自底向上白色→分类糖果色渐变」的柱状图外观。 |
| `IncomeTimelineView.java`（320 行） | 自研收入时间轴 View：自下而上绘制（最早日期在底部），每个有收入的日期一个节点，横线长度与金额成正比、按分类着色，支持触摸选中节点并回调 Activity 弹窗；绘制层预计算避免每帧对象分配（`制作过程.docx` 记录该热点修复使首页滚动帧耗时中位数 250ms→26ms）。 |

### 5.3 `chat`（2）— 对话 UI 数据与适配

| 文件 | 职责 |
|---|---|
| `ChatAdapter.java` | 对话列表 RecyclerView 适配器：按消息类型返回两种 ViewType，用户消息右气泡、AI 消息左气泡。 |
| `ChatMessage.java` | 一条对话消息的 POJO，用类型字段区分用户与 AI，供 `ChatAdapter` 绑定。 |

### 5.4 `chat/ai`（10）— 本地规则引擎 + 远程模型客户端

| 文件 | 职责 |
|---|---|
| `AiConfig.java` | AI 配置门面：DeepSeek API Key 交给 `DeepSeekKeyStore` 存 Keystore，偏好里只留 Base URL 与模型名，不留明文密钥。 |
| `AiRemoteClient.java` | 远程 AI 客户端：`HttpURLConnection` 请求 OpenAI 兼容 `/chat/completions`，后台线程执行、结果回主线程；仅在 AI 页且用户已配置 Key 时联网。 |
| `AiReplyConstants.java` | 全部 AI 回复话术模板常量，用 `String.format` 填数值占位符，供 `ReplyGenerator` 套用。 |
| `BillAnalysisUtils.java` | 账单分析工具：统一从 Room 读数据并计算（当月支出、某日支出、预算剩余、分类排行、商户 Top、预算读写），所有 DB 操作走 `DBHelper` 子线程并回调主线程。 |
| `BillContextBuilder.java` | 账单上下文构建器：联网前把本月支出、收入、分类、商户、近期账单汇总成结构化「账单快照」文本并注入 system prompt；取数失败有 `fallback`。 |
| `DeepSeekKeyProbe.java` | 用一次最短的 chat completion 校验 Key 是否可用；异常信息中不回显密钥。 |
| `DeepSeekKeyStore.java` | 密钥安全存储：用 Android Keystore 的 AES 密钥加密，SharedPreferences 只存密文；提供 `hasKey/get/save/clear` 与旧版明文偏好的迁移（`legacyPrefs`）。 |
| `DeepSeekStreamClient.java` | `AssistantController.Streamer` 实现：阻塞读取 DeepSeek SSE，拼 `messages` JSON（含 `JSONException` 声明），供后台线程使用。 |
| `IntentMatcher.java` | 本地意图识别引擎：纯关键词/正则、按优先级命中即返回（含「今日」查询特例），无模型、无网络。 |
| `ReplyGenerator.java` | 回复生成器：按命中意图与账单计算结果套 `AiReplyConstants` 模板，产出预算/月度总额/当日总额/分类/商户排行/省钱建议/帮助/兜底文案。 |

### 5.5 `chat/ai/assistant`（30）— 「中央学生助手」核心

| 文件 | 职责 |
|---|---|
| `AssistantController.java`（533 行） | 一轮助手会话的中枢：流式思考、四个工具（`query_spend`/`propose_add_bill`/`request_diary_context`/`daily_fortune`）编排、确认后才写 Room 并回读；约定 `start`/`confirm`/`cancel` 同线程调用，内含 `Docs`/`Streamer` 两个可替换接口。 |
| `AssistantHost.java` | 控制器与 UI 的线程边界：控制器常驻后台线程，`Status/Transcript/ConfirmCard/Usage` 回调切回主线程；提供 `startOrIgnore`（防重入）与 `retry`。 |
| `AssistantDocuments.java` | 运行期文档加载点（`AssistantController.Docs` 实现）：从 `assets/assistant/` 读取协议与 schema，文件名与 DSH v0.1 对齐，不再另写一套。 |
| `AssistantIntentPlanner.java` | 模型漏发工具调用时的兜底规划：按 `intent-control.v0.1.md` 优先级补一次调用，工具名/参数形状与 `tools.schema.v0.1.json` 一致（含「整个账本」「历史导入」判定与本地快捷句）。 |
| `AssistantStatus.java` | 文字状态枚举：仅四档（思考 / 调用工具 / 等待确认 / 出错），空闲留空不算第五档；「出错」只用于密钥、余额配额、网络失败。 |
| `AssistantStatusMachine.java` | 状态机：当前状态唯一决定确认卡是否可见（`confirmVisible`），避免 UI 自己判断。 |
| `BillDraft.java` | `propose_add_bill` 的预览对象：解析参数、校验、生成确认卡正文与预览文案，`toBill()` 才转成 `BillEntity`；构造它绝不写账。 |
| `BillLedger.java` | 账本读写抽象接口（读现有账、确认后插入并回读），由 `RoomBillLedger` 落到 Room，便于测试替身。 |
| `RoomBillLedger.java` | `BillLedger` 的 Room 实现：直接复用现有 `BillDao`/`BillEntity`，不复制第二套账本；要求调用方在后台线程执行。 |
| `LedgerBill.java` | 助手视角的账单行 POJO，字段与 `BillEntity` 对齐（`copy` 转换），本身不是账本。 |
| `SpendQuery.java` | `query_spend` 工具实现：只读，按 schema 的 `period`/商户/分类筛选，做区间、类型、文本匹配与分组汇总；含 `error()` 与参数校验。 |
| `WeekRange.java` | 本地时区「周一至周日」区间计算（含首尾），供 `period=this_week` 使用。 |
| `ReplyGate.java` | 答复闸门：涉账单的一轮在 `query_spend` 成功前，`withhold` 掉带金额或笔数的文字，防止模型编造数字。 |
| `ChatTurn.java` | OpenAI 消息结构 `role` 的构造器工厂（`system`/`user`/`assistant`/`assistantTools`/`tool`），承载 `toolCalls` 与 `toolCallId`。 |
| `ToolCall.java` | 一次工具调用（`id`/`name`/`arguments`）的值对象，带空值兜底（`call_0`、`{}`）。 |
| `StreamResult.java` | 一次流式请求的聚合结果：`think`、`content`、`calls`、失败类型与 token 用量，工厂方法 `text/tools/fail`。 |
| `SseAccumulator.java` | 逐行累积 OpenAI/DeepSeek 兼容 SSE `data:` 行，抽出思考文本、正文、分片工具参数与 prompt/completion token。 |
| `ThinkTagSplitter.java` | 流式把 `<think>` 与可见正文拆开，处理标记被切到两个分片里的情况（`suffixPrefix`）。 |
| `SystemPromptBody.java` | 读取 `SYSTEM_PROMPT.v0.1.md` 并只取分隔线之后的正文（分隔线前是给作者的说明），另提供 `sha256` 便于留痕。 |
| `ToolSchemaCatalog.java` | 只解析 `tools.schema.v0.1.json` 的工具清单（版本、`tools` 数组、工具名），不内置第二套工具名。 |
| `EmotionHooks.java` | 把助手状态/场景映射为 Emotion Ball 官方 `emotionId`（默认对齐 `emotion-map.v0.1.md`），并支持协议文件里 `hook:出错=38` 这类覆盖行。 |
| `DiaryGate.java` | 日记正文的引用闸门：只有用户确认过本次 `request_diary_context` 才能 `mayCite`，否则返回空串。 |
| `DiaryNoteStore.java` | 最小日记钩子：只读应用私有目录 `assistant/diary.txt`，无编辑页；正文仅在确认后进入对话。 |
| `FortuneComposer.java` | `daily_fortune` 的本地文案生成（塔罗只是 `style=tarot_lite` 的一种玩法）。 |
| `ProcessEvents.java` | 按 `think-protocol.v0.1.md` 生成 JSONL 过程事件（状态/Prompt/思考/工具调用/工具结果/确认/决策），每条都过密钥擦除。 |
| `FileProcessLog.java` | `Consumer<String>` 实现：把 JSONL 写入应用私有目录（对应仓库里被忽略的 `process/runtime`）。 |
| `SecretRedactor.java` | 过程日志的密钥擦除器，保证 Key 不落日志。 |
| `FailureClassifier.java` | 把 DeepSeek 的 HTTP 结果归为密钥 / 配额 / 网络三类，并给出状态行文案。 |
| `FailureKind.java` | 会把状态行置为「出错」的失败类型枚举（账本读写失败不在此列）。 |
| `AssistantCallException.java` | 带 `FailureKind` 的运行时异常，供流式/工具调用链路抛出并经 `FailureClassifier` 归类。 |

### 5.6 `chat/ai/presets` 与 `chat/ai/skills`（3）

| 文件 | 职责 |
|---|---|
| `AiPresets.java` | AI 预设配置：系统提示词与快捷提问常量，供接入真实模型时使用（可继续扩充同目录其他预设）。 |
| `AiSkill.java` | 一条内置 AI 技能的值对象（名称、说明、提示词模板）。 |
| `AiSkills.java` | 内置技能列表的静态登记；Javadoc 明示「当前为预留配置，未参与对话逻辑」。 |

### 5.7 `chat/session`（6）— 会话持久与上下文预算

| 文件 | 职责 |
|---|---|
| `ChatSessionBook.java` | 本地会话本：切页/退后台/重进都回到当前会话 id 而不自动新开；提供 `restore/open/append/replaceLast/rename/delete` 与内存要点。 |
| `ChatSessionRoom.java` | 会话本落库：把会话与消息写进 Room（`chat_sessions`/`chat_messages`/`chat_memory`），当前 id 另存偏好；密钥不进表。 |
| `ContextUsage.java` | 上下文占用百分比：只用 token 与模型窗口计算（`percent/level/wouldNotFit`），不用消息条数。 |
| `MemoryHandoff.java` | 要点交接策略：约 80% 只在后台预存要点、不切换；约 95% 或本轮放不下才开下一场（95% 且无要点时先抽取再切换）。 |
| `ModelWindows.java` | 模型上下文窗口表：`deepseek-chat`/`reasoner` 为 128K，`tokensFor` 提供查询。 |
| `TokenEstimate.java` | 单段待发送文本的 token 估算（`ofText`），不按消息条数估。 |

### 5.8 `data`（13）— Room 数据层

| 文件 | 职责 |
|---|---|
| `AppDatabase.java` | Room 单例（`student_bill.db`，version 3，`exportSchema=false`）：聚合 5 个实体，手写 `MIGRATION_1_2`（加 `bills.batch_id`、建 `import_batches`）与 `MIGRATION_2_3`（建 `chat_sessions`/`chat_messages` + sessionId 索引/`chat_memory`）。 |
| `BillDao.java` | 账单与导入批次的 DAO：插入（单条/批量）、更新、删除（按 id/按批次/清空）、按 id/按日/按月查询、按月+类型聚合分类金额与商户金额、按月+类型求和、总条数、`DISTINCT` 已有月份、批次的增删查。 |
| `BillEntity.java` | 账单实体（表 `bills`）：`id/type/category/amount/merchant/remark/date/time/create_time/batch_id`；`TYPE_EXPENSE=0`、`TYPE_INCOME=1`，`isIncome()`。 |
| `ImportBatchEntity.java` | 导入批次实体（表 `import_batches`）：`id/name/source/create_time`，用于历史页按批次管理。 |
| `CategoryAmount.java` | 分类金额聚合结果（`@ColumnInfo category/total`），承接 DAO 的投影查询。 |
| `MerchantAmount.java` | 商户金额聚合结果（`merchant/total`），同上。 |
| `CategoryClassifier.java` | 本地分类细化：按商户、备注与原始分类关键词把导入账单归入更细的消费/收入分类；纯本机规则，不改金额时间等字段，含 `fallback` 兜底。 |
| `DBHelper.java` | 数据层门面：单例 + 单线程 `ExecutorService` 执行所有 DB 操作、回调回主线程（`DataCallback<T>`），并提供插入/批量插入/更新/删除/按 id/按日/按月等便捷方法；首次执行存量账单分类细化。 |
| `SelectedMonthStore.java` | 首页图表当前选中月份的本机持久化（`save/get`），供各页读取同一份，避免依赖启动 Intent 的旧 extra。 |
| `ChatSessionEntity.java` | 会话实体（表 `chat_sessions`）：`id/title/createdAt/updatedAt/scrollPosition/injectedMemory/showMemoryTip`。 |
| `ChatMessageEntity.java` | 会话消息实体（表 `chat_messages`，`sessionId` 建索引）：`id/sessionId/type/content/position`。 |
| `ChatMemoryEntity.java` | 跨会话记忆要点实体（表 `chat_memory`，固定 `id=1`）：`points/sourceSessionId/ready`。 |
| `ChatSessionDao.java` | 会话 DAO：拉全部会话/全部消息、清空、批量插入、`REPLACE` 保存要点、读取要点。 |

### 5.9 `imports`（5）— 四路账单解析

| 文件 | 职责 |
|---|---|
| `BillCsvParser.java`（249 行） | CSV 解析：`detectCharset` 探测编码（含 BOM/UTF-8 校验）、OpenCSV 读全表、`findHeaderRow` 定位表头、`resolveIndexes` 按列名解析列序（失败用 `defaultIndexes`）、逐行 `parseFields` 生成 `BillEntity`，坏行计入 `failed`；同时对外提供 `defaultIndexes()` 与静态 `parseFields` 给粘贴解析复用。 |
| `BillPasteParser.java` | 粘贴文本解析：按行切分，跳过表头行（含「金额」「日期/交易时间」），先按逗号/中文逗号/制表符分列，列数不足再按空白切，复用 `BillCsvParser.parseFields` 出账单。 |
| `BillPdfParser.java`（397 行） | PDF 解析（PDFBox-Android）：逐页 `PDFTextStripper` 收集文本 run 并按坐标聚成行（`collectRuns/buildRows/rowText/findX`），用日期/时间/金额正则（`DATE_PATTERN`/`TIME_PATTERN`/`MONEY_PATTERN`）把行分成主行、状态行、金额三类再对齐，兼容支付宝/微信账单版式。 |
| `BillXlsxParser.java`（449 行） | XLSX 解析（不引三方库）：用 `ZipInputStream` 解包 `xl/` 内条目，`sharedStrings.xml` 解析共享字符串，`XmlPullParser` 逐 cell 读出工作表，`findHeaderRow` + `resolveIndexes` 定位类型/金额/时间等列后生成账单；缺列时提前返回空结果。 |
| `ImportResult.java` | 解析结果容器：`List<BillEntity> bills` + `int failed`（失败行计数），四种解析器统一返回它。 |

### 5.10 `ui`（8）— 复用交互与情绪球几何

| 文件 | 职责 |
|---|---|
| `BottomTabHelper.java` | 底栏（include_bottom_tabs）统一绑定：`Tab` 枚举（首页/日历/AI/历史）映射 `viewId` 与目标 Activity，处理选中着色、点击跳转（带 `extra_month`、复用单例）与 system bar inset。 |
| `PressScaleHelper.java` | iOS 式按压缩放：`ACTION_DOWN` 缩到 0.96、抬起/取消回弹 1.0，100ms `DecelerateInterpolator`，`attach(View)` 一次性挂载且不吞点击。 |
| `AttitudeGazeMath.java` | 手机姿态转视线的纯算逻辑：8° 内视为抖动不驱动、12° 才看向倾斜方向、约 30° 对应视线 ±1，阈值集中便于替换；`clear/step` 无 Android 依赖（可单测）。 |
| `BallAttitudeGaze.java` | `SensorEventListener` 实现：优先旋转矢量、退化到游戏旋转矢量、再退化到重力+陀螺仪驱动情绪球视线；手指按球时不写视线，页面不可见即停传感器。 |
| `ChatBallStage.java` | 对话页情绪球舞台 `FrameLayout`：消息列表铺在球下方，重写 `dispatchTouchEvent` 把球体以外的点击/滑动交给列表，避免方形 WebView 吃掉事件。 |
| `ChatBallFade.java` | 对话页球的渐隐几何：按 emotion-ball 的 `viewBox(-15,-15,259,259)` 与 blob 轮廓算出球顶/球底/渐隐带，不写死 dp。 |
| `ChatBallMetrics.java` | 球面板高度：展开约窗口 1/3，键盘或确认卡时压到 160dp 硬下限；首页大球不走此类。 |
| `ChatInputChrome.java` | 底部输入区冻结高度：空闲 56dp 输入行、状态行 32dp、多行输入上限 96dp、上下文百分比仅在占用时才占位，保证键盘弹起不跳动。 |

---

## 6. `app/src/main/res` 逐文件职责（74 个 XML）

### 6.1 `layout/`（21）

| 文件 | 职责 |
|---|---|
| `activity_main.xml`（469 行） | 首页单页滚动：标题区、常驻情绪球（WebView + 触摸层）、预算面板、图表面板（折线 `LineChart` + 分类 `BarChart`）、功能入口与底栏 include。 |
| `activity_add_bill.xml`（291 行） | 记一笔表单：返回/标题、支出收入切换、金额输入、分类 `Spinner`、商户、备注、日期时间按钮、保存与隐私提示。 |
| `activity_edit_bill.xml`（291 行） | 编辑账单表单，结构与 `activity_add_bill` 一致（同一套字段 id），仅入口语义不同。 |
| `activity_calendar.xml`（245 行） | 日历页：月份切换头、周标题行、7×N 日历网格、当日汇总卡、当日账单 RecyclerView 与空态。 |
| `activity_history.xml`（237 行） | 历史页：标题与清空按钮、汇总、搜索框、全部/支出/收入筛选 chip、月份筛选、账单/批次混合列表与空态容器。 |
| `activity_import.xml`（168 行） | 导入页：返回/标题、粘贴导入 / CSV 导入 / PDF·XLSX 导入三个按钮、隐私提示。 |
| `activity_income_detail.xml`（182 行） | 收入详情：标题与总额、月份标签、`PieChart` 饼图、图例容器、收入时间轴 `IncomeTimelineView` 与空态。 |
| `activity_merchant_rank.xml`（90 行） | 商户排行：返回/标题、汇总文案、`RecyclerView` 与空态。 |
| `activity_ai_chat.xml`（503 行） | AI 对话页：`ChatBallStage` 球舞台、WebView 球体、渐隐层、消息 `RecyclerView`、上下文提示、输入区与底栏 include，头部含会话抽屉/过程页/API KEY 入口。 |
| `activity_assistant_process.xml`（82 行） | 过程页：返回、标题、`ScrollView` 内的单一大 `TextView` 承载 JSONL 文本。 |
| `include_bottom_tabs.xml`（137 行） | 四宫格底栏（首页/日历/AI/历史），供各页 `include` 复用。 |
| `dialog_api_key.xml`（114 行） | API KEY 配置对话框：Key / Base URL / Model 三个输入 + 引导折叠区。 |
| `dialog_deepseek_key.xml`（46 行） | DeepSeek 专用密钥输入对话框（Key 输入 + 校验结果文本）。 |
| `dialog_income_detail.xml`（155 行） | 时间轴节点点击后的账单详情弹窗：金额、分类、色标、商户、备注、时间与关闭按钮。 |
| `item_bill_day.xml`（106 行） | 日历页当日账单行：分类、金额、商户、时间、编辑、删除。 |
| `item_history_bill.xml`（103 行） | 历史页账单行：日期、分类、商户、金额、编辑、删除。 |
| `item_history_batch.xml`（79 行） | 历史页批次头：批次名、条数、时间、清除按钮。 |
| `item_merchant_rank.xml`（78 行） | 商户排行行：名次、商户名、占比 `ProgressBar`、金额、百分比。 |
| `item_income_legend.xml`（41 行） | 收入饼图图例项：色点、分类名、金额、百分比。 |
| `item_chat_user.xml`（25 行） | 用户消息气泡（右对齐单个 `TextView`）。 |
| `item_chat_ai.xml`（24 行） | AI 消息气泡（左对齐单个 `TextView`）。 |

### 6.2 `drawable/`（44）

全部为 `shape`（一处 `layer-list`、两处 `vector`），按用途分组：

| 文件 | 职责 |
|---|---|
| `bg_page_gradient.xml` | 页面底色（`themes.xml` 的 `windowBackground`），纯色底消除白闪。 |
| `bg_y2k_page.xml` | Y2K 风格页底备选，与 `bg_page_gradient` 同为纯色 shape。 |
| `bg_card.xml`、`bg_candy_panel.xml`、`bg_candy_glass.xml`、`bg_dialog_candy.xml`、`bg_chart_surface.xml`、`bg_glass_panel.xml` | 各类卡片/面板/图表/对话框底：奶油白填充 + 16dp 圆角（`bg_glass_ai`、`bg_page_gradient` 为无圆角纯色）。 |
| `bg_glass_ai.xml` | AI 面板专用无圆角浅色底。 |
| `bg_button_primary.xml`、`bg_button_candy.xml`、`bg_gradient_blue.xml`、`bg_gradient_red.xml`、`bg_y2k_pill.xml` | 主按钮与渐变色块：蓝色渐变主 CTA、糖果渐变按钮、预算未超支（蓝）/超支（红）渐变的两个数据源。 |
| `bg_button_ghost.xml` | 次级/幽灵按钮底（纯色 + 圆角）。 |
| `bg_input.xml`、`bg_y2k_search.xml` | 输入框 / 搜索框底：浅色 + 圆角（搜索框带细描边）。 |
| `bg_y2k_chip.xml`、`bg_y2k_pill_ghost.xml`、`bg_candy_pill.xml`、`bg_soft_capsule.xml`、`bg_quick_chip.xml` | 胶囊系：Y2K chip、幽灵胶囊、糖果胶囊、柔和胶囊、快捷问句 chip。 |
| `bg_chip_selected.xml`、`bg_toggle_selected.xml`、`bg_toggle_expense.xml`、`bg_toggle_income.xml` | 选中态与收支切换按钮底：选中色、支出色、收入色三套。 |
| `bg_bill_expense.xml`、`bg_bill_income.xml` | 账单行收入（淡绿）/支出（淡红）底，带描边。 |
| `bg_budget_ok.xml`、`bg_budget_over.xml` | 预算面板正常/超支两态底（超支带警示描边）。 |
| `bg_tab_bar.xml` | 底栏 `layer-list`：白底 + 顶部 1dp 分隔线（iOS Tab 观感）。 |
| `bg_tab_item_selected.xml` | 底栏选中项底（浅色圆角块）。 |
| `bg_chat_bubble_user.xml`、`bg_chat_bubble_ai.xml` | 对话左右气泡底。 |
| `bg_calendar_day_selected.xml`、`bg_calendar_dot.xml` | 日历选中日底 / 有收支日期的小圆点（`size` 定尺寸）。 |
| `bg_rank_badge.xml`、`bg_rank_badge_normal.xml` | 商户排行前三名金银铜徽标 / 普通名次徽标。 |
| `bg_ai_badge.xml` | AI 小标签底。 |
| `bg_legend_dot.xml`、`bg_list_separator.xml` | 图例色点 / 列表分隔线（`size` 定 1dp 高）。 |
| `bg_y2k_glass.xml` | Y2K 玻璃卡片底（纯色 + 圆角 + 描边）。 |
| `ic_launcher_background.xml`、`ic_launcher_foreground.xml` | 自适应图标的前后两层 `vector` 矢量图。 |

### 6.3 `anim/`、`interpolator/`（3）

| 文件 | 职责 |
|---|---|
| `anim/tab_fade_in.xml` | 底栏切页淡入（`alpha` 0→1）。 |
| `anim/tab_fade_out.xml` | 底栏切页淡出（`alpha` 1→0）。 |
| `interpolator/tab_crossfade.xml` | 底栏 220ms 交叉淡入淡出所用的 `pathInterpolator` 曲线。 |

### 6.4 `values/`（5）

| 文件 | 职责 |
|---|---|
| `strings.xml`（227 行） | 全部用户可见文案：`app_name=学生消费账单`、`main_title=智能消费账单助手`、`main_subtitle=AI 帮你看懂每月生活消费`、AI 面板/预算/图表/导入/历史/对话/过程页等各页面文案与 toast。 |
| `colors.xml`（103 行） | 设计 token 色板：`color_sky`、`color_pink_soft_cta`、`color_primary`/`_dark`/`_light`（校园蓝系）、`color_warning`、页底/卡片/文字/语义色等。 |
| `dimens.xml`（26 行） | 间距（`page_padding 20dp`、`card_padding 16dp`、`space_xs..xl`）、圆角（`card_radius 16dp`、`radius_input 12dp`、`radius_pill 999dp`、`tab_bar_height 56dp`）、字号（`text_title 24sp` … `text_caption 12sp`）三组 token，注释明确「圆角档锁死，禁止中间魔法值」。 |
| `arrays.xml`（58 行） | 三套分类数组：`bill_categories` 21 项（两类型共用，编辑页用）、`expense_categories` 15 项、`income_categories` 8 项（生活费/兼职/奖学金/红包/转账/退款/报销/其他）。 |
| `themes.xml`（14 行） | 唯一主题 `Theme.StudentBill`（parent `Theme.AppCompat.Light.NoActionBar`）：主色映射、浅色状态栏、`statusBarColor=page_bg_start`、`navigationBarColor=bg_card`、`windowBackground=bg_page_gradient`、主/次文字色。 |

### 6.5 `mipmap-anydpi-v26/`（1）

| 文件 | 职责 |
|---|---|
| `ic_launcher.xml` | 自适应图标描述（`adaptive-icon` 引用 `ic_launcher_background` + `ic_launcher_foreground`）；Manifest 中 `icon`/`roundIcon` 均指向 `@mipmap/ic_launcher`。 |

---

## 7. `app/src/test` 逐文件职责（7 个测试类 / 49 个 `@Test`，全部纯 JVM，不依赖设备）

> 49 = `AiReplyTest` 6 + `AssistantCoreTest` 26 + `SessionRulesTest` 5 + `AttitudeGazeMathTest` 2 + `ChatBallFadeTest` 3 + `ChatBallMetricsTest` 4 + `ChatInputChromeTest` 3。2026-11-12 12:27 的 `testDebugUnitTest` 结果为 49 tests / 0 failures / 0 errors（报告见 `app/build/test-results/testDebugUnitTest/`，可用第 13 节命令复跑）。

| 文件 | 职责 |
|---|---|
| `chat/ai/AiReplyTest.java` | 本地规则引擎单测：7 类意图优先级识别、今日查询、预算有剩余/超支话术、总额与分类排行、商户排行与省钱建议、空数据兜底文案。 |
| `chat/ai/assistant/AssistantCoreTest.java`（646 行，测试最大） | 中央助手纯逻辑：四状态且确认卡只在等待确认、schema 正好四个工具且塔罗不是独立工具、system prompt 只取正文、本周为周一至周日、`query_spend` 食堂按分类或商户命中且只统计支出、当月跟随首页选中月、`period=all` 含导入与手动且合计不因明细截断、planner 兜底、SSE 拼出思考/正文/分片工具参数。 |
| `chat/session/SessionRulesTest.java` | 会话规则：写盘再读回同一消息与滚动位置、标题取首条用户消息（空标题回退）、80% 只预存不切换 / 95% 才带记忆开新场、百分比按 token 与窗口而非条数、默认对话路径不再挂语音引擎。 |
| `ui/AttitudeGazeMathTest.java` | 视线映射：8° 内不驱动、12° 看向倾斜、30° 接近满偏；松开阈值有迟滞且非待机立即停止。 |
| `ui/ChatBallFadeTest.java` | 球渐隐几何：渐隐从球体下沿拉到上沿，高面板与 160dp 都跟随；球顶到舞台顶是无透明缝的实心浅灰；球两侧不在命中范围。 |
| `ui/ChatBallMetricsTest.java` | 球面板高度：展开约窗口 1/3、键盘/确认卡先压到 160dp、空间不足仍守 160dp、1/3 放不下但够 160dp 时缩到可用空间。 |
| `ui/ChatInputChromeTest.java` | 输入区高度：空闲 56dp 且不预留状态行与上下文、四档状态 32dp、多行输入上限 96dp、上下文有字才占位。 |

---

## 8. `app/src/main/assets` 逐文件（13）

| 文件 | 职责 |
|---|---|
| `assistant/SYSTEM_PROMPT.v0.1.md`（5,377 B） | 发给模型的系统提示词正文（分隔线前是作者说明，运行时由 `SystemPromptBody` 截掉）；同时被 `syncAssistantDocs` 从 `docs/assistant` 同步。 |
| `assistant/tools.schema.v0.1.json`（7,707 B） | DeepSeek 兼容 function calling 的四个工具 schema（`query_spend`/`propose_add_bill`/`request_diary_context`/`daily_fortune`），含 `chart_month` 等参数约定；运行时由 `ToolSchemaCatalog` 解析。 |
| `assistant/intent-control.v0.1.md`（4,947 B） | 意图优先级、反例话术与演示脚本对齐规则，`AssistantIntentPlanner` 依此兜底补调用。 |
| `assistant/think-protocol.v0.1.md`（3,152 B） | 流式思考可视化与 JSONL 过程留痕事件枚举，`ProcessEvents` 按此产出。 |
| `assistant/emotion-map.v0.1.md`（9,943 B） | 情绪/状态映射：文字状态为主体 + Emotion Ball 官方 32 个 `emotionId`（空闲 02、思考 30、流式出字 39、成功 33、查账 19/40、工具 32、等待确认 03、出错 34）；`EmotionHooks` 默认对齐此文件。 |
| `emotion-ball/host.html`（1,169 B） | 本 App 唯一新增的球体宿主页：加载并驱动下面四个上游脚本的 WebView 容器（其余 js 均为上游未修改代码）。 |
| `emotion-ball/ball.js`（23,826 B） | 上游球体渲染主逻辑（未修改）。 |
| `emotion-ball/emotions.js`（21,922 B） | 上游 32 个情绪/表情定义（未修改）。 |
| `emotion-ball/engine.js`（30,644 B） | 上游动画/渲染引擎（未修改）。 |
| `emotion-ball/rings.js`（41,780 B） | 上游环形/装饰层绘制（未修改，体积最大的前端文件）。 |
| `emotion-ball/ATTRIBUTION.txt`（512 B） | 上游署名与使用边界：源自 `github.com/sam70361/aora-bot` 的 `emotion-ball` 分支 `f793bb4`；球体造型**非商用**，仅用于学习与本比赛演示；`host.html` 为本项目新增。 |
| `emotion-ball/UPSTREAM_LICENSE`（5,027 B） | 上游原始许可全文。 |
| `emotion-ball/UPSTREAM_NOTICE.md`（3,234 B） | 上游 NOTICE/来源说明。 |

---

## 9. `docs/` 逐文件

> 归一后 `docs/` 共 **26** 个文件：下列 25 个 + 本报告（`PROJECT-INVENTORY.md`）。`PROJECT_IDENTIFICATION_2026-11-12.md` 的独有事实已并入本报告 §0.1 / §11.1 / §14，原文件已删除。

### 9.1 `docs/assistant/`（11）

| 文件 | 职责 |
|---|---|
| `README.md`（6,839 B） | 助手层**总纲**：v0.1 范围（查账/记账主线、日记作经确认的上下文、一个娱乐工具）、文件清单、四档文字状态、32 个 emotionId 映射、语音输入已冻结取消、`chart_month` 跟随首页选中月、会话与上下文策略、以及逐版 APK 交付记录（`测试v0.9/0.10/0.11.apk` 的大小/SHA256/PR #3 提交号）。 |
| `REPRODUCE.md`（22,064 B） | 「10 分钟极速复现指南」：环境、依赖、变量模板、演示话术与过程页核对步骤。 |
| `SUBMISSION-CHECKLIST.md`（2,907 B） | 交赛材料清单（文档、截图、过程留痕、APK 等齐备性核对项）。 |
| `THIRD_PARTY.md`（4,821 B） | 第三方许可说明：Emotion Ball 球体造型不可商用须署名；离线语音为 Vosk Apache-2.0 与中文小模型（该能力后已冻结）。 |
| `SYSTEM_PROMPT.v0.1.md`（5,377 B） | 系统提示词源文件，构建时同步到 assets。 |
| `tools.schema.v0.1.json`（7,707 B） | 工具 schema 源文件，构建时同步到 assets。 |
| `intent-control.v0.1.md`（4,947 B） | 意图控制与优先级源文件，构建时同步。 |
| `think-protocol.v0.1.md`（3,152 B） | 思考可视化与过程留痕协议源文件，构建时同步。 |
| `emotion-map.v0.1.md`（9,943 B） | 情绪映射源文件，构建时同步。 |
| `session.v0.1.md`（3,690 B，未跟踪新文件） | 会话与上下文协议（已冻）：会话/消息存本地 Room 且当前 id 写盘、切页退后台不新开、仅「新建对话」手动新开、历史抽屉 80% 屏宽最大 320dp、上下文百分比 = 实际 token / 模型窗口、80% 预存要点不切换 / 95% 或放不下才开新场。 |
| `env.example`（1,237 B） | 环境变量模板（无真实 Key），供复现时参考。 |

### 9.2 `docs/assistant/process/`（过程留痕）

| 文件 | 职责 |
|---|---|
| `README.md`（1,972 B） | 过程日志目录约定：`process/YYYY-MM-DD-<id>.jsonl` 命名、只记事实、禁止写 Key；并给出 `DECISION_LOG.md` + `templates/` + `sessions/` + `runtime/`（gitignore）的目录约定。 |
| `DECISION_LOG.md`（189,396 B） | 决策与范围演进只追加日志（仓库内最大的文档），记录每一轮范围/协议/交付决策。 |
| `sessions/.gitkeep`（0 B） | 占位文件，保证 `sessions/`（脱敏后的演示归档）目录入库。 |
| `templates/session-index.md` | 会话索引空白模板（日期、Prompt 版本、工具调用、结果）。 |
| `templates/prompt-record.md` | Prompt 记录空白模板。 |
| `templates/tool-call.md` | 单次工具调用参数/结果记录模板。 |
| `templates/confirm-record.md` | 写库前确认记录模板。 |

### 9.3 `docs/superpowers/`（UI 系统化升级的规格与计划）

| 文件 | 职责 |
|---|---|
| `specs/2026-11-04-apple-system-ui-design.md`（4,347 B） | 已定稿设计规格：「Apple 系统感为主、极淡粉彩只点缀」全 App 升级；明确页底 `#F2F2F7`、卡片 `#FFFFFF` 圆角 16–20dp 无描边、按压缩放 ≈0.96、iOS 式底栏、主 CTA 改「天空蓝→淡粉渐变」，并声明不再遵循 `Promat历史` 的 Y2K 半融合约束。 |
| `plans/2026-11-04-apple-system-ui.md`（16,464 B） | 上述设计的落地实施计划（分任务拆解与环境/验收口径）。 |
| `plans/sdd-task-1-report.md` | SDD 任务 1 执行报告（token 层/页底）。 |
| `plans/sdd-task-2-report.md` | SDD 任务 2 执行报告（首页与图表）。 |
| `plans/sdd-task-3-report.md` | SDD 任务 3 执行报告（表单/日历/导入）。 |
| `plans/sdd-task-4-report.md` | SDD 任务 4 执行报告（AI 对话/History）。 |
| `plans/sdd-task-5-6-report.md` | SDD 任务 5–6 执行报告（收尾/统一核对）。 |

---

## 10. 其他目录

| 路径 | 职责 |
|---|---|
| `Promat历史/ui-refresh-2026-11-04/P1_trae_prompt.md`（215 B） | 早期 UI 刷新第一期（首页 + 图表重排）交给 Trae 的提示词归档：「Apple 骨架 × Y2K 点缀、主色 #2F6FED；activity_main 拆 AI/预算，图表近白，主 CTA 蓝」。与 `docs/superpowers/specs` 属同一视觉演进的先后两版口径。 |
| `screenshots/*.png`（10 张） | `01_main_top`、`02_main_bottom`、`03_expense_detail`、`04_calendar`、`05_income_detail`、`06_add_bill`、`07_import`、`08_merchant_rank`、`09_ai_chat`、`10_history`；README 与制作过程文档的图 1–图 10 引用源。 |
| `模拟账单/`（2 个文件） | 真实微信支付账单导出：`微信支付账单流水(20260724-20261024).pdf` 482,522 B 与 `..._20261024152455.xlsx` 22,289 B，用于验证 `BillPdfParser` / `BillXlsxParser`。 |

---

## 11. 数据存储设计（Room `student_bill.db`，version 3）

| 表 | 实体 | 字段 |
|---|---|---|
| `bills` | `BillEntity` | `id`(PK auto)、`type`(0=支出/1=收入)、`category`、`amount`、`merchant`、`remark`、`date`(字符串 `yyyy-MM-dd`)、`time`(字符串 `HH:mm`)、`create_time`、`batch_id`(默认 0) |
| `import_batches` | `ImportBatchEntity` | `id`、`name`、`source`、`create_time` |
| `chat_sessions` | `ChatSessionEntity` | `id`、`title`、`createdAt`、`updatedAt`、`scrollPosition`、`injectedMemory`、`showMemoryTip` |
| `chat_messages` | `ChatMessageEntity` | `id`、`sessionId`(索引)、`type`、`content`、`position` |
| `chat_memory` | `ChatMemoryEntity` | `id`(固定 1)、`points`、`sourceSessionId`、`ready` |

**按月聚合的实现细节**：DAO 全部用 `substr(date, 1, 7) = :month` 做月份过滤（`date` 存字符串，无独立月份列），`GROUP BY category` / `GROUP BY merchant` + `SUM(amount)`，并用 `SELECT DISTINCT substr(date,1,7)` 提供「只列已有账单月份」的切换弹窗数据。

**非 Room 的本机状态**：预算（SharedPreferences）、首页选中月份（`SelectedMonthStore`）、AI Base URL/模型名（SharedPreferences）、DeepSeek 密钥密文（SharedPreferences 存密文 + Android Keystore 存 AES 密钥）、当前会话 id（偏好）、过程日志（应用私有目录 JSONL，对应仓库被忽略的 `process/runtime`）。

### 11.1 隐私与安全边界（唯一口径）

- **权限最小化**：`AndroidManifest.xml` 仅声明 `android.permission.INTERNET`；无存储、通讯录、定位、相机、录音权限。文件导入走 SAF，剪贴板只在点「粘贴导入」时读取一次，无后台监听。
- **本地持久化**：账单、导入批次、会话/消息/记忆都在 Room `student_bill.db`；预算、选中月份、AI Base URL/模型名、当前会话 id 在本机 SharedPreferences。`android:allowBackup="false"`，**卸载或换机即丢账本**，无云同步、无账号体系。
- **密钥**：DeepSeek Key 不进 `BuildConfig`/仓库；应用内向导写入后用 AndroidKeyStore 的 AES 密钥加密，SharedPreferences 只存密文与 IV；旧版明文有一次性迁移；过程日志用 `SecretRedactor` 擦除密钥。
- **联网外发（必须如实告知）**：账单默认只存在本机 Room；只有用户主动发起联网问答时，才会把**当月收支汇总、预算（仅已设置时）、Top5 分类与商户、最近 15 条账单**拼进 Prompt 发给 DeepSeek——旧版 AI 页 `AiChatActivity` 是 `BillContextBuilder` 的唯一调用点（`MAX_RECENT_BILLS=15`、`MAX_TOP=5`；未设预算时不拼预算项）。中央助手对话还会发送用户消息与 `query_spend` 的查询结果；其余情况不联网、不上传，不做账单云同步。
- **语音**：离线 Vosk / 系统语音已冻结取消，依赖与模型不参与当前构建；对话输入只保留文字。
- **文案同步（T6 已随 T5 入库）**：`app/src/main/res/values/strings.xml` 的 `edit_privacy_tip`、`chat_welcome`、`import_privacy_tip` 已由 T6 改为分层口径（`strings.xml:100/121/214`：仅用户主动联网问答时外发上述字段），归一口径以本节为准。

---

## 12. 关键业务流

1. **手动记账**：`MainActivity` → `AddBillActivity`（类型切换驱动 `expense_categories`/`income_categories`）→ `DBHelper.insert`（单线程池）→ 回主线程 → 返回首页 `onResume` 全量重算预算/图表。
2. **编辑 / 删除**：`CalendarActivity` 当日列表或 `HistoryActivity` 列表 → `EditBillActivity`（按 id 回填）→ `DBHelper.update`；删除走 `AlertDialog` 二次确认 → `deleteById`。
3. **账单导入**：`ImportActivity` 三个入口 → 粘贴（`BillPasteParser`）/ CSV（`BillCsvParser`）/ PDF 或 XLSX（`BillPdfParser`、`BillXlsxParser`，SAF `Uri` 流式读）→ 统一 `ImportResult` → `CategoryClassifier.classify` 细化分类 → 建 `ImportBatchEntity` + 批量 `insertAll`（带 `batchId`）→ 历史页可按批次整体删除。
4. **统计与图表**：`BillAnalysisUtils`（预算/总额/分类/商户）与各 Activity 直接走 `BillDao` 聚合 → `MPAndroidChart` 折线/柱状/饼图 + `IncomeTimelineView` 时间轴；`MainActivity` 月份切换后通过 `SelectedMonthStore` 广播给日历/明细/排行页。
5. **本地 AI 问答**：`IntentMatcher` 关键词优先级命中意图 → `BillAnalysisUtils` 读库计算 → `ReplyGenerator` 套 `AiReplyConstants` 模板；全程零网络。
6. **远程 AI 对话**：`AiChatActivity` → `AssistantHost.startOrIgnore` → `AssistantController`（后台线程）：组装 system prompt（`SystemPromptBody`）+ 账单快照（`BillContextBuilder`）+ 历史 `ChatTurn` → `DeepSeekStreamClient` 读 SSE（`SseAccumulator` + `ThinkTagSplitter`）→ 产出工具调用或正文；`AssistantStatusMachine` 只凭状态决定确认卡；`ReplyGate` 挡住未查账先报数。
7. **确认后写库并回读**：`propose_add_bill` 只生成 `BillDraft` 预览 → 用户点确认 → `AssistantController.confirm` → `RoomBillLedger.insertAndRead` → 回读结果拼进对话，`ProcessEvents` 同时写 JSONL 留痕（`SecretRedactor` 擦密钥）。
8. **会话与上下文**：`ChatSessionBook` 写盘当前会话 id，`ChatSessionRoom` 落 Room；`TokenEstimate` + `ModelWindows` → `ContextUsage` 出百分比；`MemoryHandoff` 在 80% 预存要点、95% 或放不下时开下一场。
9. **情绪球**：首页大球与对话页球面板均由 `WebView` 加载 `assets/emotion-ball/host.html`；`EmotionHooks` 把四档状态映射成官方 `emotionId`；`BallAttitudeGaze` + `AttitudeGazeMath` 用手机姿态驱动视线，`ChatBallStage`/`ChatBallFade`/`ChatBallMetrics`/`ChatInputChrome` 负责事件穿透、渐隐与高度联动。

---

## 13. 构建、运行与验证命令

**环境**：JDK 17；Android SDK Platform **API 37** + Build-Tools；Gradle 用项目自带 Wrapper（9.6.0，无需本机安装）；首次构建需在根目录存在 `local.properties` 的 `sdk.dir`（本仓库已有，且被 gitignore）。

```powershell
# 构建 debug APK → app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat assembleDebug

# 构建并安装到已连接设备/模拟器
.\gradlew.bat installDebug

# 运行单元测试（7 个纯 JVM 测试类）
.\gradlew.bat testDebugUnitTest

# 等价测试报告位置
# app\build\reports\tests\testDebugUnitTest\index.html

# 清理
.\gradlew.bat clean
```

```bash
# macOS / Linux
./gradlew assembleDebug installDebug testDebugUnitTest
```

**运行入口**：`AndroidManifest.xml` 中 `MainActivity`（`MAIN`/`LAUNCHER`，`exported=true`，`singleTop`）。也可直接用 Android Studio 打开仓库根目录，Gradle Sync 后 Run。

**只读校验命令**（本报告第 15 节使用的同一套）：

```powershell
git rev-parse HEAD
git status --porcelain | Out-File -Encoding utf8 <临时文件>
Get-FileHash <临时文件> -Algorithm SHA256
```

---

## 14. 一致性核对：文档与代码的差异（均已实测确认；标「已修」的两项已在本轮归一处理）

| # | 说法来源 | 实际代码 | 影响 |
|---|---|---|---|
| 1 | `README.md` 第 102–116 行「项目结构」把解析器写成 `parser/` 包，并写 `chat/ai/` 只有「本地规则引擎 + AiRemoteClient」 | 实际包名是 **`imports/`**；`chat/ai/` 下有 10 个类，另有 `chat/ai/assistant`（30）、`chat/ai/presets`、`chat/ai/skills`、`chat/session`（6）、`ui`（8）等 README 未提及的包 | 目录结构描述落后于当前代码，**建议按本报告更新 README** |
| 2 | `README.md` 第 30 行「全部 **9 个** Activity 锁定竖屏」；`制作过程.docx` 2.1 节亦写「全部 9 个页面」 | `AndroidManifest.xml` 实测 **10 个** Activity（多出 `AssistantProcessActivity`，过程页） | 数量口径不一致；**README 已于 2026-11-12 归一修为 10**，docx 仍待修 |
| 3 | `README.md` 依赖表写 `androidx.appcompat **1.7.0**` | `app/build.gradle` 实为 `1.7.1` | **已于 2026-11-12 归一修为 1.7.1** |
| 4 | `制作过程.docx` 五节写「XLSX 复用现有解析框架**预留入口**」 | `imports/BillXlsxParser.java` 已是 449 行完整实现（Zip + `sharedStrings.xml` + `XmlPullParser`） | 文档低估了实现程度 |
| 5 | `README.md` 功能表说导入「自动识别支付宝/微信账单格式」 | 代码层面 `BillPdfParser`/`BillXlsxParser` 主要按**列名/坐标版式**做通用识别（`findHeaderRow`/`resolveIndexes`），并未按渠道分支判断 | 表述偏宽，实际是版式通用解析 |

另需注意：**识别开始时**工作树共 **219 行** = 200 M + 17 ?? + 2 D（`~$制作过程.docx`、`基础提示词.txt` 为删除项），`MainActivity.java` 当时差异 52 insertions / 31 deletions，属真实内容改动而非行尾符差异。T5 并行提交后状态已前移，当前口径以 §15 末尾「文档归一后的复核」与 §0.1「并发提交进展」为准（179 行 = 173 M + 4 ?? + 2 D；`app/src` 的 138 行 M 均为 `core.autocrlf` 行尾刷新）。本 Task 未改 `app/src`，删除的只是被合并的 `docs/PROJECT_IDENTIFICATION_2026-11-12.md`。

---

## 15. 只读性证明

- 识别开始时记录基线：`git status --porcelain` 输出 **219 行**（200 已修改 / 17 未跟踪 / 2 已删除），落盘为 `%TEMP%\ssb_baseline_status.txt`，SHA256 = `1F55E4F4A7E99AF4BE617FA6D9FAC108CA09438AE6EE89C31EE22F5EC8CB8668`。
- 结束时重算同一命令，输出 **221 行**；其中新增的 2 行全部是未跟踪的 Markdown 报告：
  - `?? docs/PROJECT-INVENTORY.md`（本报告）
  - `?? docs/PROJECT_IDENTIFICATION_2026-11-12.md`（**另一份同名主题报告，12:21:03 由同项目的其他执行者写入，非本报告作者所为**；两份内容重叠，2026-11-12 归一后已删除，独有事实并入本报告）
- 把上述 2 行从未跟踪列表中排除后，与基线逐行 `Compare-Object` 的结果为 **IDENTICAL（零差异）**，行数同为 219；`git rev-parse HEAD` 仍为 `ae6d21645552fd0e6cc860a4815eadde30da2df8`。
- 结论：**识别阶段**除上述 Markdown 报告外，没有任何原有文件被修改、移动或删除（本次归一对 `README.md`、`docs/assistant/REPRODUCE.md` 的编辑见下条）。其余操作全部为读（`Get-ChildItem` / `Get-Content` / `Select-String` / `git status` / `git diff` / `git log` / `python-docx` 只读解析）或写入 `%TEMP%` 下的临时事实文件。

> 附：`git diff` 在本仓库**不可用于统计**——工作树中存在已删除的 `~$制作过程.docx`（Office 锁文件），`git diff` 会报 `fatal: unable to read files to diff` 中途终止，因此 `git diff --name-only` 只输出 37 个就断掉（远少于真实的 200 个修改）；同时因 `core.autocrlf` 会对几乎每个文件刷 `LF will be replaced by CRLF` 警告。所以只读性验证以 `git status --porcelain` 的全文件哈希加逐行比对为准，而不是 `git diff` 行数。

- **2026-11-12 文档归一后的复核（截至 12:42）**：删除 `docs/PROJECT_IDENTIFICATION_2026-11-12.md` 后，T5 并行提交使 `git status` 基线大幅前移；当前 `git status --porcelain` 为 **179 行**（173 M + 4 ?? + 2 D），HEAD 为 `c7bafea`（`feat/assistant-competition`，5 个模块 commit），不再是 `ae6d216`。`git status --porcelain -- app/src` 仍有 138 行 M，但 `git diff --ignore-space-at-eol --numstat -- app/src` 为 **0 个文件**——全部是 T5 提交触发的 `core.autocrlf` 行尾刷新，无内容改动。本 Task 的写操作仅限 `README.md`、`docs/assistant/REPRODUCE.md`、本报告三份文档；`app/src` 的唯一内容变更是 T6 的预算修复/文案/`BillAnalysisUtilsTest`，已随 `c7bafea` 入库。归一口径下 `docs/PROJECT-*.md` 只剩本报告 1 个；T4 `DEMO-SCRIPT.md` 与 `session.v0.1.md` 仍为未跟踪，`docs/` 共 27 个文件（快照口径 26）。

---

## 16. 文档归一记录（2026-11-12）

- **唯一报告**：以本报告为权威；删除 `docs/PROJECT_IDENTIFICATION_2026-11-12.md`，其独有事实并入 §0.1（计数口径 / 复现边界）、§11.1（隐私、密钥、语音、文案风险）、§14（风险清单）。
- **README**：Activity 9→10、appcompat 1.7.0→1.7.1；隐私统一为「账单默认只存在本机 Room；只有用户主动发起联网问答时，才把当月收支汇总、预算、Top5 分类与商户、最近 15 条账单发给 DeepSeek；其余不联网、不上传；`allowBackup=false` 卸载/换机丢账本」；演示链拆为「离线快捷芯片段」与「联网 planner 段」，后者须预置已测通 Key，无 Key 出错 `API_KEY`；单测改为 7 类 49 项（`ae6d216` 快照；T5 最新 HEAD `c7bafea` 实测 8 类 50 项）。
- **REPRODUCE**：旧 APK 证据（37,770,505 B / SHA256 `609ba8…` / 43 项 / `e4dfd1b`）统一标注为历史，不再作当前证据；补 49 项明细（快照）与「APK 以最终提交后干净 clone 重建为准」；新增 §5.1「无 Key 时的预期行为」；本地路径改为示例路径。
- **未改代码**：本 Task 未改 `app/src` 任何文件，只编辑 3 个文档；T6 的预算修复、文案与 `BillAnalysisUtilsTest` 已随 T5 `c7bafea` 入库，当前 `app/src` 工作树仅有 `core.autocrlf` 行尾刷新（`git diff --ignore-space-at-eol --numstat -- app/src` 为空）。
