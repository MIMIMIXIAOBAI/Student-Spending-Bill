# 10分钟极速复现指南（评委用）

目标：在已安装 Android Studio / SDK 的前提下，约 10 分钟从空环境跑到可演示的「中央学生助手」主线，并在过程页点开留痕。首次安装 SDK 或创建模拟器不计入这 10 分钟。

仓库：[MIMIMIXIAOBAI/Student-Spending-Bill](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill)（须保持 Public 或向评委开放访问）  
包名：`com.campusbill.app` · 纯 Java + Room · 本地路径约定：`E:\AI\Student Spending Bill`

交赛材料总清单见 [SUBMISSION-CHECKLIST.md](SUBMISSION-CHECKLIST.md)。环境变量模板见 [env.example](env.example)。

## 1. 环境配置

| 项 | 要求 |
|---|---|
| Android Studio | Narwhal 或更新（内含 JDK 17） |
| SDK | Platform API 37 + Build-Tools；模拟器建议 API 29+、Graphics = Hardware |
| Gradle | 使用仓库自带 Wrapper（无需单独安装） |
| 本机文件 | 根目录创建 `local.properties`，写入 `sdk.dir=...`（该文件已 gitignore） |

```powershell
git clone https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill.git
cd "Student-Spending-Bill"
# 按 docs/assistant/env.example 写入 local.properties 后：
.\gradlew.bat assembleDebug installDebug
```

或用 Android Studio 打开仓库根目录，Sync 后 Run。不要把 DeepSeek Key 写进任何仓库文件或 `gradle.properties`。

## 2. 依赖清单

以 `app/build.gradle` 为准，核心依赖如下（版本若有更新以构建文件为准）：

| 组件 | 版本（账本工程现有 README） | 用途 |
|---|---|---|
| AndroidX AppCompat / ConstraintLayout / RecyclerView | 见根 README | UI |
| AndroidX Room | 2.6.1 | 本机账本 |
| OpenCSV | 5.9 | CSV 导入 |
| PDFBox Android (`com.tom-roush`) | 2.0.27.0 | PDF 导入 |
| MPAndroidChart | v3.1.0 | 图表 |
| JUnit | 4.13.2 | 单元测试 |

助手层不另引入云端 SDK 密钥；DeepSeek 为用户自备服务。Emotion Ball 若已接入，许可见 [THIRD_PARTY.md](THIRD_PARTY.md)。

## 3. 环境变量模板

见 [env.example](env.example)。摘要：

- `local.properties` 只写 `sdk.dir`，已 gitignore。
- DeepSeek Key 经应用内向导写入本机 Keystore，不入库。
- 若使用临时 `secrets.properties`，必须保持 gitignore，禁止提交真实值。

## 4. 首次启动：DeepSeek Key 向导

1. 打开应用，进入中央助手对话页（现有 `AiChatActivity` 扩展，仍为竖屏）。
2. 按向导粘贴 **DeepSeek API Key**。密钥写入本机 Keystore，仅本机有效。
3. 向导应给出「测通 / 失败」结果后再进入对话。失败时文字状态为「出错」（密钥失败、余额不足、网络错误），检查网络与 Key，不要改仓库配置。

默认接口与模型以向导内预填为准（账本阶段曾默认 `https://api.deepseek.com/chat/completions`、`deepseek-chat`，以当前向导文案为准）。

## 5. 演示主线（请按顺序说）

建议先确保本机 Room 里已有本周食堂相关账单（可用「记一笔」或导入；评测机可用仓库 `模拟账单/`）。本周按周一至周日，日期 `YYYY-MM-DD`。

文字状态共四档，独立可见：**思考 / 调用工具 / 等待确认 / 出错**。确认卡只出现在「等待确认」。回复为流式字符输出（硬指标）；思考过程可可视化。Emotion Ball 不能替代文字状态。按已拍板约定：AI 页主屏常驻大球（替换原「AI消费建议」面板），不再用顶栏开关，先不放到记账首页；滑动或点击时视线跟随。同一页里键盘弹出或出现确认卡时，球面板压缩到最低 160dp，不能挡住状态行和确认卡，也不另在对话页放一颗小球。工程落地前，评测机上可能仍是旧的顶栏开关，不以已实现论。

1. **查询**（工具 `query_spend`，只读）：对助手说：「这周食堂花了多少？」
   - 文字状态可见：思考 → 调用工具。
   - 只读已有 Room（`BillDao` / `BillEntity`），不另建账本。
2. **记账并回读**（工具 `propose_add_bill`）：再说：「再记一笔今天午餐 18 元」。
   - 状态为「等待确认」，弹出确认卡片（未确认不得写入）。
   - 确认后由应用写入 Room 并回读，核对金额/分类/日期。
3. **过程页**：打开过程页，应能点开本轮的：
   - 系统/用户 Prompt（版本号对应 `docs/assistant/` 中已入库文件）
   - 工具名称与参数
   - 确认记录（确认/取消、时间、写入前后回读）
4. **调度证明（主线之后）**：问一句今日运势（工具 `daily_fortune`，第一版唯一娱乐入口）。应再次出现「调用工具」，证明会调度而非只聊天。

按住说话为第二优先，本脚本不强制。日记仅在用户确认后可作为上下文（工具 `request_diary_context`），不是独立日记 App。

## 6. 留痕如何被点开

| 位置 | 内容 |
|---|---|
| 应用内过程页 | 当场会话的 Prompt / 工具参数 / 确认记录 |
| [`process/`](process/) | 目录约定 + 模板；演示会话可导出或按模板归档 |
| [`prompt_evolution.md`](../../prompt_evolution.md) | 历史任务与 Prompt 迭代（现阶段视为原始材料，不润色） |
| `Promat历史/`、`制作过程.docx` | 更早的制作过程（原始材料，交赛前再统一收拾） |

运行时原始日志若含密钥或完整账单明细，只留在本机，目录建议 `docs/assistant/process/runtime/`（应 gitignore）。入库的是脱敏后的决策与模板化记录。

## 7. 范围红线（复现时不要扩展）

- 不新开独立 App；不单独做横屏套 UI、八个侧边栏。
- 不把塔罗/日记/随手记做成独立入口；塔罗最多作为运势玩法。
- 不接多厂商 Key 向导（只要 DeepSeek）。
- 工具冻结为上述四个；文字状态冻结为上述四档。
- Emotion Ball 在 AI 页主屏常驻并替换「AI消费建议」面板，去掉顶栏开关；先不放到记账首页。不另在对话页放小球；键盘或确认卡出现时球面板压缩到最低 160dp，不能挡住状态行和确认卡，也不能替代文字状态。许可见 [THIRD_PARTY.md](THIRD_PARTY.md)。

## 8. 自检清单

- [ ] `assembleDebug` / `installDebug` 通过
- [ ] Key 向导测通，仓库内搜不到 Key
- [ ] 「本周食堂」查询走工具，流式输出 + 文字状态独立可见
- [ ] 「午餐 18 元」先确认再写入，回读一致
- [ ] 过程页三项留痕可点开
- [ ] 运势类工具可被调度一次
- [ ] 密钥/网络失败时出现「出错」，无新页面
