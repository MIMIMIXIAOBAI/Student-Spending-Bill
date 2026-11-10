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
| `emotion-map.v0.1.md` | 情绪/状态映射（文字状态为主体；官方 32 个 emotionId；首页大球、对话页球面板；语音输入已取消并冻结） |
| `session.v0.1.md` | 历史会话、上下文占用百分比与要点交接（已冻；不另开版本号） |
| `process/README.md` | 过程日志目录约定 |

修订时递增版本号（v0.2…），旧文件保留以便评委复现。

文字状态四档：思考、调用工具、等待确认、出错（失败态）。v0.1 草稿在合入前已按评估页补上「出错」，不另开版本号。

按 2026-11-10 再更正：球必须使用 Emotion Ball 官方 32 个 `emotionId`：空闲 `02`、思考 `30`、流式出字 `39`、成功 `33`、查询报数 `19`、工具 `32`（查账可用 `40`）、等待确认 `03`、出错 `34`。禁止传自造情绪名。详见 `emotion-map.v0.1.md`。问「当月」「本月」时跟首页折线图当前选中月份走，见 `SYSTEM_PROMPT.v0.1.md` 与 `tools.schema.v0.1.json` 的 `chart_month`。对话页球约占屏高三分之一，键盘或确认卡时仍压到 160dp。用户真机认为语音效果不好，主管已冻结：**语音输入整段取消**（红圆麦克风、Vosk、按住识别均不再作为产品能力，倾听 `35` 不再使用）。本包只做历史对话，见 `session.v0.1.md`：切页或退出不新开会话，会话存本地 Room；右下角按实际 token / 模型窗口显示占用百分比；约 80% 且本轮回复结束后只在后台预存要点，不自动切换、不再走「思考」；用户新建对话时再带入；约 95% 或发送前会超窗时才自动开下一场，预存未成功再走思考。左侧用抽屉新建/切换/重命名/删除历史。球、陀螺仪、月份同步本包不动。工程已交 `C:\Users\ZXM\Desktop\测试v0.9.apk`（37,770,505 字节，SHA256 `1ecd54253872f05f4d1a9ab6e325b1968962de4250dbc8b0f9a2dd555ceeacbf`，打包时间 2026-11-11 00:43:09 Asia/Shanghai）。安装包已包含重复 `@Override` 修正，对应 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3) 当前头 [`447423a`](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/commit/447423a9a6f6072f86775e61d45dad716c8a7315)，不用另下包。本机单元测试 34 项已通过；真机效果待确认。不合入 `master`。【序号】33 已由主管冻进本轮：涉及账单或消费必须先 `query_spend` 再答；金额等只能来自本轮结果；范围含导入和手动记录，与记账页同一本账；未指定时间或问历史/习惯/消费建议时用 `period=all`，合计和分类汇总要完整，明细可截断。过程只走「调用工具」和表情 `32`（查账也可用 `40`），不弹确认卡；查不到就在回复里说明。应用侧：本轮还没成功查到账，不要把带数字的回复给用户。不新增工具，不改四档状态。工程已另交 `C:\Users\ZXM\Desktop\测试v0.10.apk`（37,770,505 字节，SHA256 `2619f50fdaeb281a17984b4005813ff5c302b49d3ad7b6f9b9b9ff858acad70d`，打包时间 2026-11-11 01:33 Asia/Shanghai），没有覆盖 `测试v0.9.apk`。代码在 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3)，当时头 [`d691d85`](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/commit/d691d852f8ed54c3eddbdd8ed6366be7eb1328a6)。本机单元测试 39 项已通过；未指定时间会用 `period=all` 查全部导入和手动记录。仍不合入 `master`，真机效果待确认。测通交包后的下一件按主管安排：对话页球约占屏高三分之一。
