# 第三方许可与来源标注

本项目是**比赛/学习作品，不是商用产品**。使用开源组件必须标注来源。本页随助手层更新；合并依赖时由工程侧同步版本号。

## 1. Emotion Ball / Aora Bot（必须标注）

| 项 | 说明 |
|---|---|
| 来源 | [sam70361/aora-bot](https://github.com/sam70361/aora-bot)，**经典球形**在 `emotion-ball` 分支 |
| 接入方式 | 应用内 WebView。记账首页用常驻大球替换「AI消费建议」卡片，轻点进入对话页；对话页留下球面板（键盘或确认卡时压到 160dp）。须保留署名 |
| 许可要点 | `emotion-ball` 分支沿用 Emotion Ball 社区许可（非商业免费）。**球形角色视觉形象（造型、配色、特效等）仅供学习研究，禁止任何商业用途，且不提供、永不提供商业授权。** 表情引擎与表情数据的商业授权不包含该造型。条款以仓库内 `LICENSE`、`NOTICE.md`、[LICENSE-COMMERCIAL.md](https://github.com/sam70361/aora-bot/blob/main/LICENSE-COMMERCIAL.md) 及 [授权说明](https://aora-bot.vercel.app/guide/license) 为准。 |
| 本项目用法 | 用于比赛演示。记账首页常驻大球（约 260dp、上限 300dp），轻点进入对话页；对话页球面板默认约占屏高三分之一，键盘或确认卡时压到 160dp。空闲 `02` 且页面可见时，可用姿态驱动 `setGaze`（不足 8° 忽略，达到 12° 看向倾斜方向，约 30° 看满）；表情占用时不激活，进后台 `setActive(false)`。表情只用官方两位 `emotionId` 调用 `setEmotion('35')` 这类字符串（见 `emotion-ball` 分支 `emotion-ball/js/emotions.js` 与 [表情馆](https://sam70361.github.io/aora-bot/)），不要用 `listening` / `pleased` 等自造名；只使用已有 32 个编号（`00`–`07`、`10`–`21`、`30`–`41`）。常用对照：空闲 `02`、思考 `30`、流式出字 `39`、成功 `33`（约 1.5 秒回 `02`）、报数 `19`、调用工具 `32`/`40`、等待确认 `03`、出错 `34`。倾听 `35` 不再用于已取消的语音输入。不能替代文字状态。须在关于页保留署名。本包只做对话历史时，先不改球与陀螺仪实现。 |

应用「关于 / 许可」页与 README 均须保留上述来源与「造型不可商用」声明。不得删除署名。若未来商用，必须去掉该球形视觉，或改用自有/已获授权的形象（本比赛范围不做商用）。

接入后请在下表补充分支、commit 与本地路径：

| 字段 | 值（落地后填写） |
|---|---|
| 使用的 git ref / commit | _待 Trae 接入后填写_ |
| 资源在本仓库中的路径 | _待填写_ |

## 2. Vosk（已取消的产品能力）

| 项 | 说明 |
|---|---|
| 来源 | [alphacep/vosk-api](https://github.com/alphacep/vosk-api)，Android 库曾用 `com.alphacephei:vosk-android:0.3.75`（以当时 `app/build.gradle` 为准） |
| 模型 | `vosk-model-small-cn-0.22`，Apache-2.0。构建时曾从 [官方模型页](https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip) 下载，不提交压缩包 |
| 现状 | 真机语音效果记为未通过，语音输入已从产品取消（含按住说话、红色麦克风、系统识别和 Vosk 离线回退）。不再作为演示路径。已核 [PR #3](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/pull/3) 当前头 [`447423a`](https://github.com/MIMIMIXIAOBAI/Student-Spending-Bill/commit/447423a9a6f6072f86775e61d45dad716c8a7315) 的 `app/build.gradle` 已去掉 `vosk-android` 与模型下载；Trae 称 `测试v0.9.apk` 不再打包 Vosk。关于页不再署 Vosk。本页保留历史来源与 Apache-2.0 说明。 |
| 许可 | Apache-2.0。依赖已从当前构建去掉后，关于页不必再署 Vosk；本页保留历史来源与许可说明。 |

不要接讯飞等闭源 SDK，也不要让用户去装 Google。不要把 Vosk 重新做成产品能力。

## 3. 账本工程已有依赖（沿用根 README）

| 组件 | 版本（以 `app/build.gradle` 为准） | 用途 | 来源/许可证（请以官方仓库为准） |
|---|---|---|---|
| AndroidX AppCompat / ConstraintLayout / RecyclerView | 见根 README | UI | Apache-2.0 |
| AndroidX Room | 2.6.1 | 本机账本与助手会话/消息 | Apache-2.0 |
| OpenCSV | 5.9 | CSV 导入 | Apache-2.0 |
| PDFBox Android (`com.tom-roush`) | 2.0.27.0 | PDF 导入 | Apache-2.0 |
| MPAndroidChart | v3.1.0（JitPack） | 图表 | Apache-2.0 |
| JUnit | 4.13.2 | 单元测试 | EPL-1.0 |

DeepSeek 为用户自备的云端模型服务，本仓库不包含其 SDK 密钥或专有模型权重。

## 4. 新增开源时的检查单

- [ ] 记录仓库 URL、许可全文链接、使用的 commit
- [ ] 比赛可用但须标注；禁止扒取付费/闭源项目
- [ ] 不把密钥、证书、Keystore 打进包或提交进 git
