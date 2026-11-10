# 思考可视化与过程留痕协议 v0.1

评审硬指标：流式输出 + 思考可视化；过程可复现。文字状态是主体。进行中只有下面三档；失败时用同一行文字状态显示 **出错**（密钥失败、余额不足、网络错误），不加新页面、不加第四种进行中文案。空闲/完成可由 UI 自行处理，不要再发明其它进行中文案：

- **思考**
- **调用工具**
- **等待确认**
- **出错**（失败态：密钥失败、余额不足、网络错误）

## 模型侧（流式）

建议在流式正文中使用可解析标记（应用解析后对用户折叠或单独展示「思考」区）：

```text
<think>用户先问本周食堂花销，再要记午餐 18 元；应先查询再发起确认。</think>
```

随后是标准 tool call（DeepSeek function calling），不要把工具参数只写在散文里。对用户最终可见回复避免出现 `<think>` 或原始参数 JSON。

## 应用侧状态机

| 阶段 | 文字状态 | 说明 |
|---|---|---|
| 开始生成 / 读 `<think>` | 思考 | 流式展示思考摘要 |
| 发出或执行 tool call（查询、运势、申请日记） | 调用工具 | 过程页记录工具名与参数 |
| `propose_add_bill` 已返回或日记申请待同意 | 等待确认 | 弹出确认卡，等待用户 |
| 确认后回读 / 最终作答 | 思考或结束态 | 用回读结果生成核对语 |
| 密钥失败、余额不足、网络错误 | 出错 | 同一行文字状态说明原因；不加新页面 |

## 过程页事件（JSONL）

每条一行，写入 `docs/assistant/process/` 或本机同等目录（演示可导出）。**禁止写入密钥。**

```json
{"ts":"2026-11-10T13:00:00+08:00","type":"status","value":"思考"}
{"ts":"2026-11-10T13:00:01+08:00","type":"prompt","version":"v0.1","hash":"<sha256 of system prompt>"}
{"ts":"2026-11-10T13:00:02+08:00","type":"think","text":"..."}
{"ts":"2026-11-10T13:00:03+08:00","type":"tool_call","name":"query_spend","arguments":{}}
{"ts":"2026-11-10T13:00:04+08:00","type":"tool_result","name":"query_spend","result_summary":"total=... count=..."}
{"ts":"2026-11-10T13:00:05+08:00","type":"status","value":"等待确认"}
{"ts":"2026-11-10T13:00:20+08:00","type":"confirm_record","action":"add_bill","accepted":true,"preview":{},"verified":{}}
```

出错示例：

```json
{"ts":"2026-11-10T13:20:00+08:00","type":"status","value":"出错","reason":"network_error","message":"网络错误，请稍后重试"}
```

`reason` 建议枚举：`key_failure`（密钥失败）| `insufficient_balance`（余额不足）| `network_error`（网络错误）。不要在日志里写入密钥本身。

`type` 枚举：`status` | `prompt` | `think` | `tool_call` | `tool_result` | `confirm_record` | `decision` | `error`。

`confirm_record` 必须能看出：展示给用户的字段、是否同意、回读核对值。

## 与仓库文档的衔接

- Prompt 版本以本目录文件名为准（如 `SYSTEM_PROMPT.v0.1.md`）。
- 重大决策同时记入根目录 `prompt_evolution.md` 和 `Promat历史/`。
- `制作过程.docx` 由文档侧按需摘录，不在协议里改该文件。
