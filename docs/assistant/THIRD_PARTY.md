# 第三方许可与来源标注

本项目是**比赛/学习作品，不是商用产品**。使用开源组件必须标注来源。本页随助手层更新；合并依赖时由工程侧同步版本号。

## 1. Emotion Ball / Aora Bot（必须标注）

| 项 | 说明 |
|---|---|
| 来源 | [sam70361/aora-bot](https://github.com/sam70361/aora-bot)，**经典球形**在 `emotion-ball` 分支 |
| 接入方式 | 应用内 WebView。记账首页用常驻大球替换「AI消费建议」卡片，轻点进入对话页；对话页留下球面板（键盘或确认卡时压到 160dp）。须保留署名 |
| 许可要点 | `emotion-ball` 分支沿用 Emotion Ball 社区许可（非商业免费）。**球形角色视觉形象（造型、配色、特效等）仅供学习研究，禁止任何商业用途，且不提供、永不提供商业授权。** 表情引擎与表情数据的商业授权不包含该造型。条款以仓库内 `LICENSE`、`NOTICE.md`、[LICENSE-COMMERCIAL.md](https://github.com/sam70361/aora-bot/blob/main/LICENSE-COMMERCIAL.md) 及 [授权说明](https://aora-bot.vercel.app/guide/license) 为准。 |
| 本项目用法 | 用于比赛演示。记账首页常驻大球，轻点进入对话页；对话页留球面板，键盘或确认卡时压缩。按住说话时球依次为倾听 / 思考 / 成功，失败走「出错」，不能替代文字状态。须在关于页保留署名。 |

应用「关于 / 许可」页与 README 均须保留上述来源与「造型不可商用」声明。不得删除署名。若未来商用，必须去掉该球形视觉，或改用自有/已获授权的形象（本比赛范围不做商用）。

接入后请在下表补充分支、commit 与本地路径：

| 字段 | 值（落地后填写） |
|---|---|
| 使用的 git ref / commit | _待 Trae 接入后填写_ |
| 资源在本仓库中的路径 | _待填写_ |

## 2. Vosk 离线中文识别

| 项 | 说明 |
|---|---|
| 来源 | [alphacep/vosk-api](https://github.com/alphacep/vosk-api)，Android 库 `com.alphacephei:vosk-android` |
| 模型 | `vosk-model-small-cn-0.22`，Apache-2.0。构建时从 [官方模型页](https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip) 下载进 APK，不提交压缩包 |
| 用法 | 系统 `RecognitionService` 缺失或全部失败后，按住说话改走本机 Vosk。对话页打开时在后台解压模型 |
| 许可 | Apache-2.0。过程页保留这段署名 |

## 3. 账本工程已有依赖（沿用根 README）

| 组件 | 版本（以 `app/build.gradle` 为准） | 用途 | 来源/许可证（请以官方仓库为准） |
|---|---|---|---|
| AndroidX AppCompat / ConstraintLayout / RecyclerView | 见根 README | UI | Apache-2.0 |
| AndroidX Room | 2.6.1 | 本机数据库 | Apache-2.0 |
| OpenCSV | 5.9 | CSV 导入 | Apache-2.0 |
| PDFBox Android (`com.tom-roush`) | 2.0.27.0 | PDF 导入 | Apache-2.0 |
| MPAndroidChart | v3.1.0（JitPack） | 图表 | Apache-2.0 |
| JUnit | 4.13.2 | 单元测试 | EPL-1.0 |

DeepSeek 为用户自备的云端模型服务，本仓库不包含其 SDK 密钥或专有模型权重。

## 4. 新增开源时的检查单

- [ ] 记录仓库 URL、许可全文链接、使用的 commit
- [ ] 比赛可用但须标注；禁止扒取付费/闭源项目
- [ ] 不把密钥、证书、Keystore 打进包或提交进 git
