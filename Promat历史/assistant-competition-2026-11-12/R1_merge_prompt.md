# R1 助手主线合并与交付（2026-11-12，已执行）

目标：把「中央学生助手」主线并入 `master`，让评委从仓库首页直接看到助手代码、文档与 PDF；同时修复首页预算的误报超支问题，并产出交赛用演示视频与技术文档。

约束：
- 账单只存本机 Room，联网仅限用户主动提问；`allowBackup=false`。
- 不重写 `master` 已冻结的历史，不合入后丢弃分支；合并前必须通过干净 clone 复现构建与 51 项单测。
- 文档、脚本、README、制作过程必须与最终提交、成片、PDF 的实际数值一致，不能出现「文档说 26 页、复跑 25 页」这类口径漂移。

改动：
1. 预算四态：`BillAnalysisUtils.budgetState / budgetRemaining` + `BUDGET_UNSET/OK/OVER`，`MainActivity.updateBudgetPanel` 改为「未设预算 → 中性；0 账单 + 已设预算 → 剩余全额；正常 → 蓝白；超支 → 红白」；`BillAnalysisUtilsTest` 增加 4 条断言。
2. 助手链路：悬浮球对话页、4 个离线芯片、API KEY 向导（Keystore AES/GCM）、在线工具调用 `query_spend`/`add_bill`（记账确认卡）、过程页。
3. EOL 可复现：`.gitattributes` 只钉 `syncAssistantDocs` 的 5 个源文件为 LF，`app/src/main/assets/** -text`，避免 PDF 被误改。
4. 交付：DEMO-SCRIPT 记录实际 5:49.89 成片与 13 段时间轴；TECHNICAL-DOC 25 页 PDF；PR #4 合并入 master（merge commit `f48816b`），默认分支改回 `master`。

验收（实测）：
- 干净 clone（JDK 21.0.12.1）`clean assembleDebug + testDebugUnitTest`：BUILD SUCCESSFUL，51 tests / 0 失败，APK 26,702,585 B / SHA256 `B5A4B94991897FCCFB08C6FCF652F5FC0651615909DACE4C9AAD053E29B6CFC6`。
- 真机四态：0 账单 + 预算 1500 → 剩余 ¥1500.00；1 笔 210 → ¥1290.00；预算 100 → 红色「超出 ¥110.00」；清空预算 → 「未设置预算」。
- 在线 Key：向导测通（Toast「测通了，密钥已写入本机 Keystore。」），发 `hello` 得真实模型回复，`query_spend` 工具链完整。
- 成片 5:49.89 / 1080×1920 / 16,422,369 B / SHA256 `B7C39F4F61F1241505339982C708551E810F0245E93B12397263335289854A1A`，AI 段 155 s。

备注：本文件是赛后补记的提示词与验收记录，对应 PR #4 的合并动作；原 `Promat历史/ui-refresh-2026-11-04/P1_trae_prompt.md` 保持不变。
