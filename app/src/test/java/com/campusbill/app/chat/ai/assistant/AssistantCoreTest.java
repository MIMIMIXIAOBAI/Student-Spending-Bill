package com.campusbill.app.chat.ai.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 中央助手纯逻辑：四工具、四状态、确认门、流式解析与过程事件。不依赖设备。
 */
public class AssistantCoreTest {

    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 10);

    @Test
    public void status_只有四档且确认卡只在等待确认() throws Exception {
        assertEquals(4, AssistantStatus.values().length);
        assertEquals("思考", AssistantStatus.THINKING.label);
        assertEquals("调用工具", AssistantStatus.TOOL.label);
        assertEquals("等待确认", AssistantStatus.AWAITING_CONFIRM.label);
        assertEquals("出错", AssistantStatus.ERROR.label);

        AssistantStatusMachine machine = new AssistantStatusMachine();
        assertEquals("", machine.text());
        assertFalse(machine.confirmVisible());

        machine.set(AssistantStatus.THINKING);
        assertFalse(machine.confirmVisible());
        machine.set(AssistantStatus.TOOL);
        assertFalse(machine.confirmVisible());
        machine.set(AssistantStatus.ERROR);
        assertEquals("出错", machine.text());
        assertFalse(machine.confirmVisible());
        machine.set(AssistantStatus.AWAITING_CONFIRM);
        assertTrue(machine.confirmVisible());
        machine.clear();
        assertFalse(machine.confirmVisible());
    }

    @Test
    public void schema_正好四个工具且塔罗不是独立工具() throws Exception {
        String json = Files.readString(docs("tools.schema.v0.1.json"));
        List<String> names = ToolSchemaCatalog.toolNames(json);
        assertEquals(List.of(
                "query_spend",
                "propose_add_bill",
                "request_diary_context",
                "daily_fortune"), names);
        assertFalse(json.contains("\"name\": \"tarot"));
        assertTrue(json.contains("tarot_lite"));
        JSONArray tools = new JSONArray(ToolSchemaCatalog.toolsArray(json));
        assertEquals(4, tools.length());
        assertEquals("v0.1", ToolSchemaCatalog.version(json));
    }

    @Test
    public void systemPrompt_只取正文且不含给作者的说明() throws Exception {
        String file = Files.readString(docs("SYSTEM_PROMPT.v0.1.md"));
        String body = SystemPromptBody.extract(file);
        assertFalse(body.contains("不要发给模型"));
        assertFalse(body.startsWith("# SYSTEM_PROMPT"));
        assertTrue(body.contains("query_spend"));
        assertTrue(body.contains("propose_add_bill"));
        assertTrue(body.contains("daily_fortune"));
    }

    @Test
    public void week_本周是周一到周日() throws Exception {
        WeekRange range = WeekRange.of(TUESDAY);
        assertEquals("2026-11-09", range.start.toString());
        assertEquals("2026-11-15", range.end.toString());
        assertTrue(range.contains(LocalDate.of(2026, 11, 9)));
        assertTrue(range.contains(LocalDate.of(2026, 11, 15)));
        assertFalse(range.contains(LocalDate.of(2026, 11, 8)));
        assertFalse(range.contains(LocalDate.of(2026, 11, 16)));
    }

    @Test
    public void querySpend_食堂按分类或商户命中且只统计支出() throws Exception {
        List<LedgerBill> bills = new ArrayList<>();
        bills.add(bill(1, 0, 12.5, "食堂", "一食堂", "2026-11-09"));
        bills.add(bill(2, 0, 6, "餐饮", "第二食堂", "2026-11-10"));
        bills.add(bill(3, 0, 30, "购物", "超市", "2026-11-10"));
        bills.add(bill(4, 0, 9, "食堂", "食堂", "2026-11-08"));
        bills.add(bill(5, 1, 100, "食堂", "食堂", "2026-11-10"));

        JSONObject args = new JSONObject();
        args.put("period", "this_week");
        args.put("merchant_contains", "食堂");
        args.put("category", "食堂");
        SpendQuery.Result result = SpendQuery.run(bills, args, TUESDAY);

        assertEquals(18.5, result.total, 0.001);
        assertEquals(2, result.count);
        assertEquals("2026-11-09", result.periodStart);
        assertEquals("2026-11-15", result.periodEnd);
        assertTrue(result.json.contains("\"count\":2"));
    }

    @Test
    public void 当月跟随首页选中月_点名月份用那个月() throws Exception {
        YearMonth chart = YearMonth.of(2026, 9);
        List<ToolCall> selected = AssistantIntentPlanner.plan("当月花了多少", TUESDAY, chart);
        assertEquals("chart_month", new JSONObject(selected.get(0).arguments).getString("period"));
        List<ToolCall> words = AssistantIntentPlanner.plan("这个月消费", TUESDAY, chart);
        assertEquals("chart_month", new JSONObject(words.get(0).arguments).getString("period"));

        JSONObject named = new JSONObject(
                AssistantIntentPlanner.plan("9月花了多少", TUESDAY, chart).get(0).arguments);
        assertEquals("custom", named.getString("period"));
        assertEquals("2026-09-01", named.getString("start_date"));
        assertEquals("2026-09-30", named.getString("end_date"));

        JSONObject full = new JSONObject(
                AssistantIntentPlanner.plan("2026-09花了多少", TUESDAY, chart).get(0).arguments);
        assertEquals("2026-09-01", full.getString("start_date"));
        assertEquals("2026-09-30", full.getString("end_date"));

        JSONObject today = new JSONObject(
                AssistantIntentPlanner.plan("今天花了多少", TUESDAY, chart).get(0).arguments);
        assertEquals("today", today.getString("period"));
        assertTrue(AssistantIntentPlanner.plan("本月总支出", TUESDAY, chart).isEmpty());
    }

    @Test
    public void querySpend_thisMonth按选中月而不是本周() throws Exception {
        List<LedgerBill> bills = new ArrayList<>();
        bills.add(bill(1, 0, 20, "餐饮", "食堂", "2026-09-02"));
        bills.add(bill(2, 0, 8, "餐饮", "食堂", "2026-09-18"));
        bills.add(bill(3, 0, 15, "餐饮", "食堂", "2026-11-10"));
        JSONObject args = new JSONObject();
        args.put("period", "chart_month");
        SpendQuery.Result result = SpendQuery.run(bills, args, TUESDAY, YearMonth.of(2026, 9));
        assertEquals(28, result.total, 0.001);
        assertEquals(2, result.count);
        assertEquals("2026-09-01", result.periodStart);
        assertEquals("2026-09-30", result.periodEnd);
        args.put("period", "this_month");
        SpendQuery.Result alias = SpendQuery.run(bills, args, TUESDAY, YearMonth.of(2026, 9));
        assertEquals(28, alias.total, 0.001);
    }

    @Test
    public void querySpend_all含导入与手动且合计不因明细截断() throws Exception {
        List<LedgerBill> bills = new ArrayList<>();
        bills.add(bill(1, 0, 40, "购物", "导入店", "2025-03-01"));
        bills.add(bill(2, 0, 5, "餐饮", "食堂", "2026-11-10"));
        for (int i = 0; i < 19; i++) {
            bills.add(bill(10 + i, 0, 1, i % 2 == 0 ? "购物" : "餐饮", "导入店", "2025-06-01"));
        }
        JSONObject args = new JSONObject();
        args.put("period", "all");
        SpendQuery.Result result = SpendQuery.run(bills, args, TUESDAY);
        assertEquals(64, result.total, 0.001);
        assertEquals(21, result.count);
        assertEquals("2025-03-01", result.periodStart);
        assertEquals("2026-11-10", result.periodEnd);
        JSONObject root = new JSONObject(result.json);
        assertEquals(20, root.getJSONArray("items").length());
        JSONArray byCategory = root.getJSONArray("by_category");
        double categoryTotal = 0;
        int categoryCount = 0;
        for (int i = 0; i < byCategory.length(); i++) {
            categoryTotal += byCategory.getJSONObject(i).getDouble("total");
            categoryCount += byCategory.getJSONObject(i).getInt("count");
        }
        assertEquals(64, categoryTotal, 0.001);
        assertEquals(21, categoryCount);
        assertTrue(result.summary().contains("64.00"));

        args.put("period", "this_week");
        SpendQuery.Result week = SpendQuery.run(bills, args, TUESDAY);
        assertEquals(5, week.total, 0.001);
        assertEquals(1, week.count);

        SpendQuery.Result empty = SpendQuery.run(new ArrayList<>(), new JSONObject().put("period", "all"), TUESDAY);
        assertEquals(0, empty.count);
        assertEquals("", empty.periodStart);
        assertEquals("", empty.periodEnd);
        assertEquals("没有匹配的账单。", empty.summary());
    }

    @Test
    public void planner_未指定时间或历史导入用all且快捷句仍本地() throws Exception {
        assertEquals("all", periodOf("导入的账单怎么样"));
        assertEquals("all", periodOf("历史账单"));
        assertEquals("all", periodOf("一共花了多少"));
        assertEquals("all", periodOf("我是不是花多了"));
        assertEquals("all", periodOf("给我点消费建议"));
        assertEquals("all", periodOf("食堂花了多少"));
        assertEquals("this_week", periodOf("这周食堂花了多少"));
        assertTrue(AssistantIntentPlanner.plan("怎么导入", TUESDAY).isEmpty());
        assertTrue(AssistantIntentPlanner.plan("如何导入账单", TUESDAY).isEmpty());
        assertTrue(AssistantIntentPlanner.plan("省钱建议", TUESDAY).isEmpty());
        assertTrue(AssistantIntentPlanner.plan("消费分类", TUESDAY).isEmpty());
        assertTrue(AssistantIntentPlanner.wantsEntireLedger("导入的账单怎么样"));
        assertFalse(AssistantIntentPlanner.wantsEntireLedger("这周花了多少"));
    }

    @Test
    public void sse_拼出思考正文和分片工具参数() throws Exception {
        SseAccumulator acc = new SseAccumulator();
        acc.acceptLine("data: {\"choices\":[{\"delta\":{\"content\":\"<thi\"}}]}");
        acc.acceptLine("data: {\"choices\":[{\"delta\":{\"content\":\"nk>先查食堂</think>本周\"}}]}");
        acc.acceptLine("data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"function\":{\"name\":\"query_spend\",\"arguments\":\"{\\\"period\\\":\\\"this_\"}}]}}]}");
        acc.acceptLine("data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"week\\\"}\"}}]}}]}");
        acc.acceptLine("data: [DONE]");

        assertEquals("先查食堂", acc.think());
        assertEquals("本周", acc.content());
        assertEquals(1, acc.toolCalls().size());
        assertEquals("query_spend", acc.toolCalls().get(0).name);
        assertEquals("this_week", new JSONObject(acc.toolCalls().get(0).arguments).getString("period"));
    }

    @Test
    public void failure_只把密钥配额和网络标成出错() throws Exception {
        assertEquals(FailureKind.API_KEY, FailureClassifier.classifyHttp(401, ""));
        assertEquals(FailureKind.QUOTA, FailureClassifier.classifyHttp(402, "{\"error\":\"Insufficient Balance\"}"));
        assertEquals(FailureKind.QUOTA, FailureClassifier.classifyHttp(429, "余额不足"));
        assertEquals(FailureKind.NETWORK, FailureClassifier.classifyHttp(500, "bad gateway"));
        assertEquals("出错", AssistantStatus.ERROR.label);
        assertEquals("密钥失败", FailureClassifier.statusLine(FailureKind.API_KEY));
        assertEquals("余额不足", FailureClassifier.statusLine(FailureKind.QUOTA));
        assertEquals("网络错误", FailureClassifier.statusLine(FailureKind.NETWORK));
        assertFalse(FailureClassifier.statusLine(FailureKind.NETWORK).contains("重试"));
    }

    @Test
    public void planner_同句先查再记且运势不抢记账() throws Exception {
        List<ToolCall> both = AssistantIntentPlanner.plan(
                "这周食堂花了多少？再记一笔今天午餐 18 元", TUESDAY);
        assertEquals("query_spend", both.get(0).name);
        assertEquals("propose_add_bill", both.get(1).name);
        JSONObject query = new JSONObject(both.get(0).arguments);
        assertEquals("this_week", query.getString("period"));
        assertEquals("食堂", query.getString("merchant_contains"));
        JSONObject bill = new JSONObject(both.get(1).arguments);
        assertEquals(18, bill.getDouble("amount"), 0.001);
        assertEquals("2026-11-10", bill.getString("date"));
        assertTrue(bill.getString("remark").contains("午餐"));

        List<ToolCall> fortune = AssistantIntentPlanner.plan("今天运势怎么样，来一张塔罗", TUESDAY);
        assertEquals(1, fortune.size());
        assertEquals("daily_fortune", fortune.get(0).name);
        assertEquals("tarot_lite", new JSONObject(fortune.get(0).arguments).getString("style"));

        assertTrue(AssistantIntentPlanner.plan("本月总支出", TUESDAY).isEmpty());
    }

    @Test
    public void fortune_塔罗只是玩法且同日稳定() throws Exception {
        String a = FortuneComposer.compose(TUESDAY, "fortune");
        String b = FortuneComposer.compose(TUESDAY, "fortune");
        String tarot = FortuneComposer.compose(TUESDAY, "tarot_lite");
        String mood = FortuneComposer.compose(TUESDAY, "mood");
        assertEquals(a, b);
        assertTrue(tarot.contains("塔罗"));
        assertFalse(mood.equals(tarot));
    }

    @Test
    public void diary_未确认不得引用() throws Exception {
        DiaryGate gate = new DiaryGate();
        gate.request("greeting");
        assertEquals("", gate.citationOrEmpty("今天想吃食堂"));
        gate.confirm();
        assertEquals("今天想吃食堂", gate.citationOrEmpty("今天想吃食堂"));
        gate.request("tone_adjust");
        assertEquals("", gate.citationOrEmpty("今天想吃食堂"));
    }

    @Test
    public void demo_确认前不写库确认后回读一致() throws Exception {
        MemoryLedger ledger = new MemoryLedger();
        ledger.add(bill(1, 0, 12, "食堂", "一食堂", "2026-11-09"));
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.tools(
                tool("call_q", "query_spend",
                        "{\"period\":\"this_week\",\"merchant_contains\":\"食堂\",\"category\":\"食堂\"}"),
                tool("call_b", "propose_add_bill",
                        "{\"amount\":18,\"category\":\"食堂\",\"merchant\":\"午餐\",\"remark\":\"午餐\",\"date\":\"2026-11-10\"}")));
        streamer.enqueue(StreamResult.text("已按回读核对。"));
        RecordingUi ui = new RecordingUi();
        List<String> events = new ArrayList<>();
        AssistantController controller = controller(ledger, streamer, ui, events, true);

        controller.start("这周食堂花了多少？再记一笔今天午餐 18 元");

        assertEquals("等待确认", ui.status);
        assertTrue(ui.confirmVisible);
        assertNotNull(ui.cardBody);
        assertTrue(ui.cardBody.contains("18"));
        assertEquals(1, ledger.all().size());
        assertTrue(joined(events).contains("query_spend"));
        assertTrue(joined(events).contains("propose_add_bill"));

        controller.confirm();

        assertEquals(2, ledger.all().size());
        LedgerBill saved = ledger.all().get(1);
        assertEquals(18, saved.amount, 0.001);
        assertEquals("2026-11-10", saved.date);
        assertEquals("食堂", saved.category);
        assertEquals(saved.id, ledger.getById(saved.id).id);
        assertFalse(ui.confirmVisible);
        assertTrue(ui.transcript.contains("18"));
        assertTrue(joined(events).contains("confirm_record"));
        assertTrue(joined(events).contains("\"accepted\":true"));
        assertFalse(joined(events).contains("sk-"));
    }

    @Test
    public void demo_取消确认不写库() throws Exception {
        MemoryLedger ledger = new MemoryLedger();
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.tools(tool("call_b", "propose_add_bill",
                "{\"amount\":18,\"remark\":\"午餐\",\"date\":\"2026-11-10\"}")));
        RecordingUi ui = new RecordingUi();
        AssistantController controller = controller(ledger, streamer, ui, new ArrayList<>(), true);
        controller.start("记一笔今天午餐 18 元");
        assertEquals("等待确认", ui.status);
        controller.cancel();
        assertTrue(ledger.all().isEmpty());
        assertFalse(ui.confirmVisible);
    }

    @Test
    public void demo_模型不调工具时仍按计划查账并等待确认() throws Exception {
        MemoryLedger ledger = new MemoryLedger();
        ledger.add(bill(7, 0, 8, "食堂", "窗口", "2026-11-10"));
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.text("我猜大概八十。"));
        RecordingUi ui = new RecordingUi();
        List<String> events = new ArrayList<>();
        AssistantController controller = controller(ledger, streamer, ui, events, true);
        controller.start("这周食堂花了多少？再记一笔今天午餐 18 元");
        assertEquals("等待确认", ui.status);
        assertTrue(ui.transcript.contains("8"));
        assertFalse(ui.transcript.contains("八十"));
        assertEquals(1, ledger.all().size());
        assertTrue(joined(events).contains("decision"));
    }

    @Test
    public void 历史账单在查到之前不展示数字且包含导入记录() throws Exception {
        MemoryLedger ledger = new MemoryLedger();
        ledger.add(bill(1, 0, 40, "购物", "导入店", "2025-03-01"));
        ledger.add(bill(2, 0, 5, "餐饮", "食堂", "2026-11-10"));
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.text("一共花了80元"));
        RecordingUi ui = new RecordingUi();
        AssistantController controller = controller(ledger, streamer, ui, new ArrayList<>(), true);
        controller.start("导入的账单怎么样");
        assertFalse(ui.confirmVisible);
        assertFalse(ui.transcript.contains("80"));
        assertTrue(ui.transcript.contains("45.00"));
        assertTrue(ui.transcript.contains("2025-03-01"));
        assertTrue(ui.transcript.contains("2026-11-10"));
    }

    @Test
    public void 模型把历史问成这周时仍查全部账本() throws Exception {
        MemoryLedger ledger = new MemoryLedger();
        ledger.add(bill(1, 0, 40, "购物", "导入店", "2025-03-01"));
        ledger.add(bill(2, 0, 5, "餐饮", "食堂", "2026-11-10"));
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.tools(tool("call_q", "query_spend", "{\"period\":\"this_week\"}")));
        streamer.enqueue(StreamResult.text("查完了"));
        RecordingUi ui = new RecordingUi();
        AssistantController controller = controller(ledger, streamer, ui, new ArrayList<>(), true);
        controller.start("历史账单");
        assertFalse(ui.confirmVisible);
        assertTrue(ui.transcript.contains("45.00"));
        assertFalse(ui.transcript.contains("80"));
    }

    @Test
    public void replyGate_查账成功前拦住带数字的回复() {
        assertTrue(ReplyGate.withhold(true, false, "一共80元"));
        assertFalse(ReplyGate.withhold(true, true, "查询结果：¥45.00，共 2 笔。"));
        assertFalse(ReplyGate.withhold(false, false, "18元"));
        assertFalse(ReplyGate.withhold(true, false, "我先看一下账本"));
    }

    @Test
    public void fortune_调度第二个工具且不弹确认卡() throws Exception {
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.tools(tool("call_f", "daily_fortune", "{\"style\":\"mood\"}")));
        streamer.enqueue(StreamResult.text("今天适合慢慢吃午饭。"));
        RecordingUi ui = new RecordingUi();
        AssistantController controller = controller(new MemoryLedger(), streamer, ui, new ArrayList<>(), true);
        controller.start("看一下今天的情绪晴雨表");
        assertFalse(ui.confirmVisible);
        assertEquals("", ui.status);
        assertTrue(ui.transcript.contains("午饭") || ui.transcript.contains("晴雨"));
    }

    @Test
    public void network_出错时收起确认卡() throws Exception {
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.tools(tool("call_b", "propose_add_bill",
                "{\"amount\":18,\"remark\":\"午餐\"}")));
        streamer.enqueue(StreamResult.fail(FailureKind.NETWORK, "网络异常，这次没有连上 DeepSeek。"));
        MemoryLedger ledger = new MemoryLedger();
        RecordingUi ui = new RecordingUi();
        AssistantController controller = controller(ledger, streamer, ui, new ArrayList<>(), true);
        controller.start("记一笔午餐 18 元");
        assertTrue(ui.confirmVisible);
        controller.confirm();
        assertEquals(1, ledger.all().size());
        assertEquals("网络错误", ui.status);
        assertFalse(ui.status.contains("重试"));
        assertFalse(ui.confirmVisible);
        assertNull(ui.cardBody);
        assertTrue(ui.transcript.contains("网络"));
        assertTrue(ui.transcript.contains("18"));
    }

    @Test
    public void missingKey_是出错而不是新状态() throws Exception {
        RecordingUi ui = new RecordingUi();
        AssistantController controller = controller(new MemoryLedger(), new ScriptedStreamer(), ui,
                new ArrayList<>(), false);
        controller.start("这周食堂花了多少");
        assertEquals("密钥失败", ui.status);
        assertFalse(ui.status.contains("重试"));
        assertFalse(ui.confirmVisible);
        assertTrue(ui.transcript.contains("密钥"));
    }

    @Test
    public void retry_再发一次失败的流式请求() throws Exception {
        ScriptedStreamer streamer = new ScriptedStreamer();
        streamer.enqueue(StreamResult.fail(FailureKind.NETWORK, "网络异常，这次没有连上 DeepSeek。"));
        streamer.enqueue(StreamResult.text("回来了"));
        RecordingUi ui = new RecordingUi();
        AssistantController controller = controller(new MemoryLedger(), streamer, ui, new ArrayList<>(), true);
        controller.start("你好呀");
        assertEquals("网络错误", ui.status);
        assertFalse(ui.confirmVisible);
        controller.retry();
        assertEquals("", ui.status);
        assertTrue(ui.transcript.contains("回来了"));
        assertFalse(ui.transcript.contains("请重试"));
    }

    @Test
    public void process_事件不留下密钥() throws Exception {
        String line = ProcessEvents.toolCall("2026-11-10T13:00:03+08:00", "query_spend",
                "{\"period\":\"this_week\",\"note\":\"sk-secretkey123456\"}");
        assertFalse(line.contains("sk-secretkey123456"));
        assertTrue(line.contains("query_spend"));
        JSONObject obj = new JSONObject(line);
        assertEquals("tool_call", obj.getString("type"));
    }

    @Test
    public void emotion_四状态有球映射且可被协议钩子覆盖() throws Exception {
        EmotionHooks hooks = EmotionHooks.load("");
        assertEquals("02", hooks.idFor(null));
        assertEquals("30", hooks.idFor(AssistantStatus.THINKING));
        assertEquals("32", hooks.idFor(AssistantStatus.TOOL));
        assertEquals("03", hooks.idFor(AssistantStatus.AWAITING_CONFIRM));
        assertEquals("34", hooks.idFor(AssistantStatus.ERROR));
        assertEquals("35", hooks.idForScene("listening"));
        assertEquals("30", hooks.idForScene("thinking"));
        assertEquals("39", hooks.idForScene("streaming"));
        assertEquals("40", hooks.idForScene("query_tool"));
        assertEquals("33", hooks.idForScene("pleased"));
        assertEquals("19", hooks.idForScene("number"));
        assertEquals("02", hooks.idForScene("idle"));
        assertEquals("34", hooks.idForScene("concerned"));
        assertEquals("02", hooks.idForScene("not-an-id"));
        EmotionHooks updated = EmotionHooks.load("hook:出错=38\n");
        assertEquals("38", updated.idFor(AssistantStatus.ERROR));
        assertEquals("30", updated.idFor(AssistantStatus.THINKING));
    }

    private static AssistantController controller(MemoryLedger ledger, ScriptedStreamer streamer,
                                                  RecordingUi ui, List<String> events, boolean hasKey) {
        return new AssistantController(TUESDAY, () -> hasKey, ledger, "日记里写了想早睡",
                new AssistantController.Docs() {
                    @Override
                    public String systemPromptBody() {
                        return "你是中央学生助手";
                    }

                    @Override
                    public String toolsArray() {
                        return "[]";
                    }

                    @Override
                    public String version() {
                        return "v0.1";
                    }

                    @Override
                    public String emotionMap() {
                        return "";
                    }
                }, streamer, events::add, ui);
    }

    private static ToolCall tool(String id, String name, String args) {
        return new ToolCall(id, name, args);
    }

    private static String periodOf(String text) throws Exception {
        List<ToolCall> calls = AssistantIntentPlanner.plan(text, TUESDAY);
        assertEquals("query_spend", calls.get(0).name);
        return new JSONObject(calls.get(0).arguments).getString("period");
    }

    private static LedgerBill bill(long id, int type, double amount, String category,
                                   String merchant, String date) {
        LedgerBill bill = new LedgerBill();
        bill.id = id;
        bill.type = type;
        bill.amount = amount;
        bill.category = category;
        bill.merchant = merchant;
        bill.remark = "";
        bill.date = date;
        bill.time = "12:00:00";
        return bill;
    }

    private static String joined(List<String> events) {
        StringBuilder builder = new StringBuilder();
        for (String event : events) {
            builder.append(event).append('\n');
        }
        return builder.toString();
    }

    private static Path docs(String name) {
        Path module = Path.of("docs", "assistant", name);
        if (Files.exists(module)) {
            return module;
        }
        return Path.of("..", "docs", "assistant", name);
    }

    private static final class MemoryLedger implements BillLedger {
        private final List<LedgerBill> bills = new ArrayList<>();
        private long nextId = 1;

        void add(LedgerBill bill) {
            bills.add(bill);
            nextId = Math.max(nextId, bill.id + 1);
        }

        @Override
        public List<LedgerBill> all() {
            return new ArrayList<>(bills);
        }

        @Override
        public LedgerBill getById(long id) {
            for (LedgerBill bill : bills) {
                if (bill.id == id) {
                    return bill;
                }
            }
            return null;
        }

        @Override
        public LedgerBill insertAndRead(LedgerBill draft) {
            LedgerBill saved = draft.copy();
            saved.id = nextId++;
            bills.add(saved);
            return getById(saved.id);
        }
    }

    private static final class ScriptedStreamer implements AssistantController.Streamer {
        private final List<StreamResult> scripts = new ArrayList<>();
        private int index;

        void enqueue(StreamResult result) {
            scripts.add(result);
        }

        @Override
        public StreamResult stream(List<ChatTurn> messages, String toolsArray,
                                   AssistantController.Observer observer) {
            if (index >= scripts.size()) {
                return StreamResult.text("");
            }
            StreamResult result = scripts.get(index++);
            if (result.failure != null) {
                throw new AssistantCallException(result.failure, result.content);
            }
            if (!result.think.isEmpty()) {
                observer.onThink(result.think);
            }
            if (!result.content.isEmpty()) {
                observer.onContent(result.content);
            }
            return result;
        }
    }

    private static final class RecordingUi implements AssistantController.Ui {
        String status = "";
        boolean confirmVisible;
        String transcript = "";
        String cardBody;
        String emotion = "02";

        @Override
        public void onStatus(String statusText, boolean confirmVisible, String emotionId) {
            this.status = statusText;
            this.confirmVisible = confirmVisible;
            this.emotion = emotionId;
            if (!confirmVisible) {
                cardBody = null;
            }
        }

        @Override
        public void onTranscript(String text) {
            transcript = text;
        }

        @Override
        public void onConfirmCard(String title, String body) {
            cardBody = body;
        }
    }
}
