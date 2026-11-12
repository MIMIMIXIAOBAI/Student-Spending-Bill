# 学生消费账单（中央学生助手）

一款面向学生场景的 Android 记账应用，提供手动记账、账单导入、预算与图表分析，以及接入 DeepSeek 的中央学生助手。普通账本功能在本机运行；联网助手支持流式对话、账单查询、确认后记账和过程留痕。

## 项目概览

| 项目 | 当前配置 |
|---|---|
| 应用名称 | 学生消费账单 |
| 包名 | `com.campusbill.app` |
| 版本 | `versionCode 1` / `versionName 1.0` |
| 开发语言 | 纯 Java，源码与目标兼容性均为 Java 17，无 Kotlin 代码 |
| UI | XML 布局 + ViewBinding；10 个 Activity 均锁定竖屏 |
| 本地数据 | Room 数据库 `student_bill.db`，保存账单、导入批次、会话、消息与会话记忆 |
| 图表 | MPAndroidChart + 自定义时间轴和柱状图渲染器 |
| 网络 | `HttpURLConnection` 连接 DeepSeek，SSE 流式读取回复 |
| 系统要求 | Android 10（API 29）及以上；`compileSdk 37`、`targetSdk 36` |

## 功能一览

| 模块 | 功能 |
|---|---|
| 手动记账 | 支出/收入切换，分类联动，记录金额、商户、备注和日期时间 |
| 账单管理 | 消费日历按日查看；账单编辑与删除；历史记录支持关键词、收支类型和月份筛选，并可按导入批次或全部清空 |
| 预算管理 | 当月预算持久化，显示剩余金额；超支时切换为红色警示样式 |
| 图表分析 | 每日支出折线图、分类支出柱状图、分类饼图、收支时间轴和商户消费排行 |
| 月份联动 | 首页只列出已有账单的月份；选中的月份会持久化，并传递给明细、日历、排行及助手查询 |
| 账单导入 | 支持粘贴文本、CSV、PDF、XLSX，适配常见微信/支付宝账单字段；导入结果按批次保存和管理 |
| 中央学生助手 | 本地快捷问答、DeepSeek 流式对话、四类工具调用、确认后写入、状态展示和本机过程记录 |
| 情绪球交互 | 首页和对话页展示 Emotion Ball，支持触摸视线跟随、手机姿态驱动和助手状态表情 |
| 历史会话 | 会话与消息保存在 Room，支持新建、切换、重命名和删除；显示上下文占用，并支持记忆交接 |

## 中央学生助手

### 离线与联网行为

- 「本月总支出」「预算剩余」「消费分类」「省钱建议」四个快捷问题由本地规则和 Room 数据作答，无需 API Key。
- 帮助、商户排行等未被中央助手规划接管的本地意图可离线处理；未匹配且未配置 Key 的普通文本显示本地兜底提示。
- 查账、AI 记账、运势和日记上下文等中央助手链路需要已测通的 DeepSeek Key；未配置时显示 `API_KEY` 错误，不伪造工具调用或账单结果。
- 联网回复采用流式输出，文字状态分为「思考 / 调用工具 / 等待确认 / 出错」。
- 语音输入已取消，当前输入方式为文字，不包含麦克风、系统语音识别或 Vosk 功能。

### 工具与写入约束

| 工具 | 作用 |
|---|---|
| `query_spend` | 只读查询本机账本，支持今天、本周、首页选中月份、自定义区间和全部历史，并可按商户或分类筛选 |
| `propose_add_bill` | 生成待确认账单；只有用户在确认卡中同意后才写入 Room，并回读核对 |
| `request_diary_context` | 申请引用应用私有目录中的日记文本；确认前不会把正文加入对话 |
| `daily_fortune` | 生成今日运势、情绪晴雨表或轻量塔罗文案 |

涉及账单的回复会先查询真实账本；查询成功前，应用会拦截包含金额或笔数的模型回复。`query_spend` 的合计和分类汇总覆盖全部匹配记录，随请求发送的明细最多 20 条。

对话页提供「过程」入口，可查看本机 JSONL 中的 Prompt 版本与摘要、工具参数、状态、思考摘要、确认记录和写入回读结果。

### DeepSeek 配置

首次进入对话页且尚未配置密钥时，会打开 DeepSeek 密钥向导；也可点击右上角「API KEY」重新打开。

1. 粘贴自己的 DeepSeek API Key。
2. 点击「测通并保存」。应用会发送一次最小请求验证密钥，成功后保存。
3. 开始联网问答；需要时可在向导中清除密钥。

当前向导只连接 DeepSeek，默认配置为：

```text
Base URL: https://api.deepseek.com/chat/completions
Model:    deepseek-chat
```

密钥不是 Gradle 属性或编译期环境变量，也不需要写入仓库。API Key 使用 Android Keystore 中的 AES/GCM 密钥加密，SharedPreferences 只保存密文和 IV。

## 权限与隐私

1. Manifest 仅申请 `android.permission.INTERNET`，用于密钥测通和联网助手；普通记账、导入、日历和图表功能不发起网络请求。
2. 剪贴板仅在用户点击「粘贴导入」时读取一次，无后台监听或启动时读取。
3. CSV、PDF、XLSX 均通过 SAF 系统文件选择器读取，不申请存储权限，不使用爬虫、无障碍服务或自动拉取微信/支付宝账单。
4. 账单、导入批次、会话和记忆默认保存在本机 Room；`allowBackup=false`，不提供云同步，卸载应用会删除这些本机数据。
5. 联网助手会向 DeepSeek 发送系统提示词、当前对话内容和生成回复所需的上下文。工具被调用后，相应结果也会进入后续请求，可能包括账单汇总、最多 20 条匹配明细、确认后的账目字段或经用户确认的日记文本；会话记忆提炼同样会发送当前对话文本。
6. 过程日志保存在应用私有目录 `assistant/process/runtime/`，写入前会脱敏密钥，但仍可能包含账单字段或对话相关内容，不会自动同步到仓库。

## 构建环境

| 项目 | 要求 |
|---|---|
| Android Studio | Narwhal 或更新版本，使用与 AGP 9.4.0 兼容的内置 JBR |
| Java | 源码和目标兼容性为 Java 17 |
| Android SDK | Platform API 37 与相应 Build-Tools |
| Gradle | 使用项目 Wrapper，版本 9.6.0，无需单独安装 |
| 运行设备 | Android 10（API 29）及以上的模拟器或真机 |

首次在本机通过命令行构建前，在项目根目录创建 `local.properties`：

```properties
sdk.dir=C:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk
```

`local.properties` 已被 `.gitignore` 排除。也可以直接在 Android Studio 中配置 SDK，然后打开项目根目录并等待 Gradle Sync 完成。

## 构建与运行

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
.\gradlew.bat testDebugUnitTest
```

macOS / Linux：

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew testDebugUnitTest
```

- `assembleDebug`：生成 `app/build/outputs/apk/debug/app-debug.apk`。
- `installDebug`：安装到已连接并启用调试的设备或模拟器。
- `testDebugUnitTest`：运行本地 JVM 单元测试。
- 也可在 Android Studio 中点击 **Run** 构建并运行。

## 使用说明

1. **录入账单**：点击「记一笔」手动录入，或进入「导入账单」选择粘贴、CSV、PDF/XLSX 导入。
2. **设置预算**：在首页预算输入框填写金额，剩余与超支状态会随账单变化自动更新。
3. **查看分析**：在首页切换月份并查看折线图、柱状图；进入收支明细、商户排行或消费日历查看更多数据。
4. **管理历史**：在「历史」页搜索、筛选、编辑或删除账单，也可清除某个导入批次。
5. **使用助手**：轻点首页情绪球进入对话。四个快捷问题可离线使用；联网工具链先完成 DeepSeek 密钥测通。
6. **确认 AI 记账**：检查确认卡中的金额、分类和日期，确认后才写入；点击「过程」核对工具参数与回读结果。

## 主要依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| AndroidX AppCompat | 1.7.1 | 基础兼容组件 |
| AndroidX ConstraintLayout | 2.2.1 | 布局 |
| AndroidX RecyclerView | 1.3.2 | 账单、会话和排行列表 |
| AndroidX Room Runtime / Compiler | 2.6.1 | 本地数据库与注解处理 |
| OpenCSV | 5.9 | CSV 解析，支持 UTF-8 / GBK 检测 |
| PDFBox Android (`com.tom-roush`) | 2.0.27.0 | PDF 文本提取 |
| MPAndroidChart | v3.1.0 | 折线图、柱状图和饼图 |
| JUnit | 4.13.2 | 单元测试 |
| `org.json:json` | 20240303 | JVM 测试中的 JSON 支持 |

XLSX 由项目内的 ZIP + XML 解析器处理，不依赖 Apache POI。依赖仓库为 `google()`、`mavenCentral()` 和 JitPack。

## 项目结构

```text
app/
├── build.gradle                         # Android 配置、依赖与助手文档同步任务
└── src/
    ├── main/
    │   ├── AndroidManifest.xml          # 权限、Application 与 10 个 Activity
    │   ├── assets/
    │   │   ├── assistant/               # Prompt、工具 schema、意图与情绪协议
    │   │   └── emotion-ball/            # 本地 Emotion Ball WebView 资源与许可
    │   ├── java/com/campusbill/app/
    │   │   ├── MainActivity.java        # 首页、预算、图表、月份与情绪球
    │   │   ├── *Activity.java           # 记账、编辑、日历、历史、导入、明细、排行、对话与过程页
    │   │   ├── data/                    # Room 实体、DAO、数据库与账本访问
    │   │   ├── imports/                 # 粘贴、CSV、PDF、XLSX 解析
    │   │   ├── chart/                   # 图表渲染与自定义时间轴
    │   │   ├── chat/ai/                 # 本地意图、DeepSeek 配置与流式客户端
    │   │   ├── chat/ai/assistant/       # 助手控制、工具、确认、日记与过程日志
    │   │   ├── chat/session/            # 会话持久化、上下文用量与记忆交接
    │   │   └── ui/                      # 情绪球视线、布局和底部导航辅助
    │   └── res/                         # XML 布局、样式、图标与文案
    └── test/                            # JVM 单元测试
docs/
├── assistant/                           # 复现指南、演示、技术文档、协议与第三方许可
└── PROJECT-INVENTORY.md                 # 项目全量识别报告
prompt_evolution.md                      # Prompt 与需求演进记录
Promat历史/                              # 历史提示词归档
```

构建前，`syncAssistantDocs` 会把 `docs/assistant` 中的五份协议文件同步到 `app/src/main/assets/assistant`。

## 测试

当前测试源码包含 **8 个测试类、51 个 `@Test`**，覆盖本地回复、助手核心规则、预算状态、会话规则、姿态视线计算、情绪球尺寸与淡出、输入区布局等。

```powershell
.\gradlew.bat testDebugUnitTest
```

完整复现、真机检查和演示流程见 [10 分钟极速复现指南](docs/assistant/REPRODUCE.md)。

## 相关文档

- [项目全量识别报告](docs/PROJECT-INVENTORY.md)
- [中央学生助手复现指南](docs/assistant/REPRODUCE.md)
- [演示脚本](docs/assistant/DEMO-SCRIPT.md)
- [技术文档（Markdown）](docs/assistant/TECHNICAL-DOC.md) / [技术文档（PDF）](docs/assistant/TECHNICAL-DOC.pdf)
- [交赛材料清单](docs/assistant/SUBMISSION-CHECKLIST.md)
- [助手 Prompt 与协议说明](docs/assistant/README.md)
- [第三方许可与来源](docs/assistant/THIRD_PARTY.md)

## 第三方说明

Emotion Ball 来源于 [sam70361/aora-bot](https://github.com/sam70361/aora-bot) 的 `emotion-ball` 分支，当前 vendored 资源对应 commit `f793bb445ebad66a1a19bae75b4c27b306d2f95e`，位于 `app/src/main/assets/emotion-ball/`。

**该球形角色的造型、配色与特效仅用于学习和本比赛演示，不包含商业授权，也不提供商业授权。** 如需商用，必须移除该视觉或更换为自有/已获授权的形象。详细许可和其余依赖说明见 [THIRD_PARTY.md](docs/assistant/THIRD_PARTY.md)。
