# 一键复现（评委用）

目标：从空环境拉到可演示的「中央学生助手」主线，并在过程页点开每一步留痕。

仓库：[MIMIMIXIAOBAI/Student-Spending-Bill](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill)  
包名：`com.campusbill.app` · 纯 Java + Room · 本地路径约定：`E:\AI\Student Spending Bill`

## 1. 环境（与现有 README 一致）

| 项 | 要求 |
|---|---|
| Android Studio | Narwhal 或更新（内含 JDK 17） |
| SDK | Platform API 37 + Build-Tools；模拟器建议 API 29+、Graphics = Hardware |
| Gradle | 使用仓库自带 Wrapper（无需单独安装） |
| 本机文件 | 根目录创建 `local.properties`，写入 `sdk.dir=...`（该文件已 gitignore） |

```powershell
git clone https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill.git
cd Student-Spending-Bill
# 写入 local.properties 后：
.\gradlew.bat assembleDebug installDebug
```

或用 Android Studio 打开仓库根目录，Sync 后 Run。不要把 DeepSeek Key 写进任何仓库文件或 `gradle.properties`。

## 2. 首次启动：DeepSeek Key 向导

1. 打开应用，进入中央助手对话页（现有 `AiChatActivity` 扩展，仍为竖屏）。
2. 按向导粘贴 **DeepSeek API Key**。密钥写入本机 Keystore，仅本机有效。
3. 向导应给出「测通 / 失败」结果后再进入对话。失败时检查网络与 Key，不要改仓库配置。

默认接口与模型以向导内预填为准（账本阶段曾默认 `https://api.deepseek.com/chat/completions`、`deepseek-chat`，以当前向导文案为准）。

## 3. 演示主线（请按顺序说）

建议先确保本机 Room 里已有本周食堂相关账单（可用「记一笔」或导入；评测机可用仓库 `模拟账单/`）。

1. **查询**：对助手说：「这周食堂花了多少？」
   - 界面文字状态依次可见：思考 → 调用工具 →（如需）等待确认。失败时同一行改为「出错」（仅密钥无效、余额/配额不足、网络错误），不新增页面或工具。确认卡只在「等待确认」出现。
   - 应调用查询工具，只读已有 Room（`BillDao` / `BillEntity`），不另建账本。
   - 回复为流式输出；思考过程可可视化。
2. **记账并回读**：再说：「再记一笔今天午餐 18 元」。
   - 弹出确认卡片（未确认不得写入）。
   - 确认后工具写入 Room，并回读核对金额/分类/日期。
3. **过程页**：打开过程页，应能点开本轮的：
   - 系统/用户 Prompt（版本号对应 `docs/assistant/` 中已入库文件）
   - 工具名称与参数
   - 确认记录（确认/取消、时间、写入前后回读）
4. **调度证明（主线之后）**：问一句今日运势或情绪晴雨表（第一版仅这一个娱乐工具）。应再次出现「调用工具」状态，证明会调度而非只聊天。

按住说话为第二优先，本脚本不强制。日记仅在用户确认后可作为打招呼/语气上下文，不是独立日记 App。

## 4. 留痕如何被点开

| 位置 | 内容 |
|---|---|
| 应用内过程页 | 当场会话的 Prompt / 工具参数 / 确认记录 |
| [`process/`](process/) | 目录约定 + 模板；演示会话可导出或按模板归档 |
| [`prompt_evolution.md`](../../prompt_evolution.md) | 历史任务与 Prompt 迭代 |
| `Promat历史/`、`制作过程.docx` | 更早的制作过程 |

运行时原始日志若含密钥或完整账单明细，只留在本机，目录建议 `docs/assistant/process/runtime/`（应 gitignore）。入库的是脱敏后的决策与模板化记录。

## 5. 范围红线（复现时不要扩展）

- 不新开独立 App；不单独做横屏套 UI、八个侧边栏。
- 不把塔罗/日记/随手记做成独立入口；塔罗最多作为运势玩法。
- 不接多厂商 Key 向导（只要 DeepSeek）。
- Emotion Ball 可关、不占中屏；许可见 [THIRD_PARTY.md](THIRD_PARTY.md)。

## 6. 自检清单

- [ ] `assembleDebug` / `installDebug` 通过
- [ ] Key 向导测通，仓库内搜不到 Key
- [ ] 「本周食堂」查询走工具，流式 + 文字状态可见
- [ ] 「午餐 18 元」先确认再写入，回读一致
- [ ] 过程页三项留痕可点开
- [ ] 运势类工具可被调度一次
