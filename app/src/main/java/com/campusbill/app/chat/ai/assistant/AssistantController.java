package com.campusbill.app.chat.ai.assistant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 一轮中央助手：流式思考、四个工具、确认后才写 Room 并回读。
 * 调用方要保证 {@link #start}、{@link #confirm}、{@link #cancel} 在同一条线程上。
 */
public final class AssistantController {

    public interface Docs {
        String systemPromptBody();

        String toolsArray();

        String version();

        String emotionMap();
    }

    public interface Observer {
        void onThink(String chunk);

        void onContent(String chunk);
    }

    public interface Streamer {
        StreamResult stream(List<ChatTurn> messages, String toolsArray, Observer observer);
    }

    public interface Ui {
        void onStatus(String statusText, boolean confirmVisible, String emotionId);

        void onTranscript(String text);

        void onConfirmCard(String title, String body);

        default void onUsage(int promptTokens, int completionTokens) {
        }
    }

    private static final int MAX_ROUNDS = 4;
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final LocalDate today;
    private volatile YearMonth chartMonth;
    private volatile String injectedMemory = "";
    private final BooleanSupplier hasKey;
    private final BillLedger ledger;
    private final String diaryText;
    private final Docs docs;
    private final Streamer streamer;
    private final Consumer<String> log;
    private final Ui ui;
    private final AssistantStatusMachine machine = new AssistantStatusMachine();
    private final DiaryGate diaryGate = new DiaryGate();
    private final List<ChatTurn> messages = new ArrayList<>();
    private final EmotionHooks emotions;

    private int rounds;
    private boolean plannerAllowed = true;
    private String transcript = "";
    private String turnText = "";
    private boolean needsLedger;
    private boolean ledgerQueried;
    private ToolCall pending;
    private BillDraft pendingBill;
    private String pendingDiaryPurpose;

    public AssistantController(LocalDate today, BooleanSupplier hasKey, BillLedger ledger, String diaryText,
                               Docs docs, Streamer streamer, Consumer<String> log, Ui ui) {
        this.today = today;
        this.chartMonth = YearMonth.from(today);
        this.hasKey = hasKey;
        this.ledger = ledger;
        this.diaryText = diaryText == null ? "" : diaryText;
        this.docs = docs;
        this.streamer = streamer;
        this.log = log == null ? line -> { } : log;
        this.ui = ui;
        this.emotions = EmotionHooks.load(docs.emotionMap());
    }

    /** 首页图表选中的月份。「今天」「这周」仍用构造时的日历日。 */
    public void setInjectedMemory(String memory) {
        injectedMemory = memory == null ? "" : memory.trim();
    }

    public void restoreTurns(List<ChatTurn> turns) {
        messages.clear();
        if (turns != null) {
            messages.addAll(turns);
        }
    }

    public void setChartMonth(YearMonth month) {
        if (month != null) {
            chartMonth = month;
        }
    }

    public void start(String userText) {
        try {
            doStart(userText);
        } catch (JSONException e) {
            show("这次回复没有组好。");
            setIdle();
        }
    }

    private void doStart(String userText) throws JSONException {
        rounds = 0;
        plannerAllowed = true;
        pending = null;
        pendingBill = null;
        pendingDiaryPurpose = null;
        transcript = "";
        turnText = userText == null ? "" : userText;
        needsLedger = AssistantIntentPlanner.needsSpendQuery(turnText);
        ledgerQueried = false;
        messages.add(ChatTurn.user(userText));
        log.accept(ProcessEvents.prompt(now(), docs.version(),
                SystemPromptBody.sha256(docs.systemPromptBody())));
        setStatus(AssistantStatus.THINKING);
        if (hasKey == null || !hasKey.getAsBoolean()) {
            fail(FailureKind.API_KEY, "请先在 DeepSeek 密钥向导里填写并测通密钥。");
            return;
        }
        runModelRound(userText);
    }

    /** 再发一次失败的 DeepSeek 请求，不新增用户气泡。 */
    public void retry() {
        if (machine.current() != AssistantStatus.ERROR) {
            return;
        }
        try {
            if (rounds > 0) {
                rounds--;
            }
            if (hasKey == null || !hasKey.getAsBoolean()) {
                fail(FailureKind.API_KEY, "请先在 DeepSeek 密钥向导里填写并测通密钥。");
                return;
            }
            runModelRound(lastUserText());
        } catch (JSONException e) {
            show("这次回复没有组好。");
            setIdle();
        }
    }

    public void confirm() {
        try {
            doConfirm();
        } catch (JSONException e) {
            show("确认结果没有组好，请再看一眼过程记录。");
            setIdle();
        }
    }

    private void doConfirm() throws JSONException {
        if (machine.current() != AssistantStatus.AWAITING_CONFIRM || pending == null) {
            return;
        }
        ToolCall call = pending;
        pending = null;
        if ("request_diary_context".equals(call.name)) {
            diaryGate.confirm();
            String text = diaryGate.citationOrEmpty(diaryText);
            JSONObject verified = new JSONObject().put("purpose", pendingDiaryPurpose).put("text", text);
            log.accept(ProcessEvents.confirmRecord(now(), "diary", true,
                    new JSONObject().put("purpose", pendingDiaryPurpose), verified));
            messages.add(ChatTurn.tool(call.id, new JSONObject()
                    .put("status", "confirmed")
                    .put("purpose", pendingDiaryPurpose == null ? "" : pendingDiaryPurpose)
                    .put("text", text)
                    .toString()));
            pendingDiaryPurpose = null;
            show(text.isEmpty() ? "你同意了，但这台机器上还没有日记，我不会编一段。"
                    : "我会按你确认过的日记调整语气。");
        } else {
            BillDraft draft = pendingBill;
            pendingBill = null;
            LedgerBill saved = ledger.insertAndRead(draft.toBill());
            boolean matched = draft.matches(saved);
            JSONObject verified = billJson(saved).put("matched", matched);
            log.accept(ProcessEvents.confirmRecord(now(), "add_bill", true, draft.preview(), verified));
            messages.add(ChatTurn.tool(call.id, new JSONObject()
                    .put("status", matched ? "confirmed_and_verified" : "mismatch")
                    .put("bill", billJson(saved))
                    .toString()));
            show(verifySentence(saved, matched));
        }
        runModelRound(lastUserText());
    }

    public void cancel() {
        try {
            doCancel();
        } catch (JSONException e) {
            pending = null;
            pendingBill = null;
            pendingDiaryPurpose = null;
            setIdle();
        }
    }

    private void doCancel() throws JSONException {
        if (machine.current() != AssistantStatus.AWAITING_CONFIRM || pending == null) {
            return;
        }
        ToolCall call = pending;
        pending = null;
        boolean diary = pendingDiaryPurpose != null;
        log.accept(ProcessEvents.confirmRecord(now(), diary ? "diary" : "add_bill", false,
                diary ? new JSONObject().put("purpose", pendingDiaryPurpose)
                        : (pendingBill == null ? new JSONObject() : pendingBill.preview()),
                new JSONObject().put("status", "cancelled")));
        messages.add(ChatTurn.tool(call.id, new JSONObject().put("status", "cancelled").toString()));
        pendingBill = null;
        pendingDiaryPurpose = null;
        diaryGate.cancel();
        plannerAllowed = false;
        show(diary
                ? "日记要先经过你同意我才能用来打招呼，这一轮我先不引用。"
                : "记账要先请你在卡片上确认，我还没有写入。");
        setIdle();
    }

    private void runModelRound(String userText) throws JSONException {
        if (rounds >= MAX_ROUNDS) {
            setIdle();
            return;
        }
        rounds++;
        setStatus(AssistantStatus.THINKING);
        try {
            StringBuilder live = new StringBuilder();
            StreamResult result = streamer.stream(withSystem(), docs.toolsArray(), new Observer() {
                @Override
                public void onThink(String chunk) {
                    // 思考摘要留在过程日志，状态行保持「思考」。
                }

                @Override
                public void onContent(String chunk) {
                    live.append(chunk);
                    String visible = transcript.isEmpty() ? live.toString() : transcript + "\n" + live;
                    if (ReplyGate.withhold(needsLedger, ledgerQueried, visible)) {
                        return;
                    }
                    ui.onTranscript(visible);
                }
            });
            if (result.promptTokens > 0 || result.completionTokens > 0) {
                ui.onUsage(result.promptTokens, result.completionTokens);
            }
            if (!result.think.isEmpty()) {
                log.accept(ProcessEvents.think(now(), result.think));
            }
            if (!result.calls.isEmpty()) {
                plannerAllowed = false;
                handleCalls(order(result.calls), false, result.content.isEmpty() ? live.toString() : result.content);
                return;
            }
            if (plannerAllowed) {
                plannerAllowed = false;
                List<ToolCall> planned = AssistantIntentPlanner.plan(userText, today, chartMonth);
                if (!planned.isEmpty()) {
                    JSONArray names = new JSONArray();
                    for (ToolCall call : planned) {
                        names.put(call.name);
                    }
                    log.accept(ProcessEvents.decision(now(), "model_omitted_tool_call", names));
                    handleCalls(planned, true, "");
                    return;
                }
            }
            if (!result.content.isEmpty()) {
                show(result.content);
            }
            setIdle();
        } catch (AssistantCallException e) {
            fail(e.kind, e.getMessage().isEmpty() ? FailureClassifier.message(e.kind) : e.getMessage());
        }
    }

    private void handleCalls(List<ToolCall> calls, boolean replaceVisible, String modelContent)
            throws JSONException {
        messages.add(ChatTurn.assistantTools(calls));
        if (!replaceVisible && modelContent != null && !modelContent.isEmpty()) {
            show(modelContent);
        }
        for (int i = 0; i < calls.size(); i++) {
            ToolCall call = calls.get(i);
            log.accept(ProcessEvents.toolCall(now(), call.name, call.arguments));
            if ("propose_add_bill".equals(call.name) || "request_diary_context".equals(call.name)) {
                if (beginConfirm(call)) {
                    return;
                }
                continue;
            }
            setStatus(AssistantStatus.TOOL);
            String result = execute(call);
            messages.add(ChatTurn.tool(call.id, result));
            if ("query_spend".equals(call.name)) {
                if (querySucceeded(result)) {
                    ledgerQueried = true;
                    SpendQuery.Result query = SpendQuery.run(
                            ledger.all(), spendArgs(call.arguments), today, chartMonth);
                    log.accept(ProcessEvents.toolResult(now(), call.name,
                            "total=" + query.total + " count=" + query.count));
                    if (replaceVisible || transcript.isEmpty()) {
                        showOnly(query.summary());
                    } else {
                        show(query.summary());
                    }
                } else {
                    log.accept(ProcessEvents.toolResult(now(), call.name, "error"));
                    show("这次没有查成账本。");
                }
            } else if ("daily_fortune".equals(call.name)) {
                log.accept(ProcessEvents.toolResult(now(), call.name, "style"));
                show(new JSONObject(result).optString("text", ""));
            } else {
                log.accept(ProcessEvents.toolResult(now(), call.name, "done"));
            }
        }
        runModelRound(lastUserText());
    }

    private boolean beginConfirm(ToolCall call) throws JSONException {
        if ("request_diary_context".equals(call.name)) {
            String purpose = parseArgs(call.arguments).optString("purpose", "greeting");
            pending = call;
            pendingDiaryPurpose = purpose;
            pendingBill = null;
            diaryGate.request(purpose);
            setStatus(AssistantStatus.AWAITING_CONFIRM);
            ui.onConfirmCard("引用日记？", "允许把本机日记用于"
                    + ("greeting".equals(purpose) ? "打招呼" : "调整语气")
                    + "？确认前不会引用原文。");
            return true;
        }
        BillDraft draft = BillDraft.parse(call.arguments, today);
        if (!draft.valid()) {
            messages.add(ChatTurn.tool(call.id, new JSONObject()
                    .put("status", "cancelled")
                    .put("error", "missing amount")
                    .toString()));
            log.accept(ProcessEvents.toolResult(now(), call.name, "missing amount"));
            show("还缺金额，我没有写入。");
            return false;
        }
        pending = call;
        pendingBill = draft;
        pendingDiaryPurpose = null;
        setStatus(AssistantStatus.AWAITING_CONFIRM);
        ui.onConfirmCard("确认记下这一笔？", draft.cardBody());
        return true;
    }

    private String execute(ToolCall call) throws JSONException {
        if ("query_spend".equals(call.name)) {
            return SpendQuery.run(ledger.all(), spendArgs(call.arguments), today, chartMonth).json;
        }
        if ("daily_fortune".equals(call.name)) {
            String style = parseArgs(call.arguments).optString("style", "fortune");
            if (!"mood".equals(style) && !"tarot_lite".equals(style)) {
                style = "fortune";
            }
            return new JSONObject()
                    .put("style", style)
                    .put("text", FortuneComposer.compose(today, style))
                    .toString();
        }
        return new JSONObject().put("error", "unknown tool").put("name", call.name).toString();
    }

    private void fail(FailureKind kind, String message) {
        pending = null;
        pendingBill = null;
        pendingDiaryPurpose = null;
        machine.set(AssistantStatus.ERROR);
        ui.onStatus(FailureClassifier.statusLine(kind), false,
                emotions.idFor(AssistantStatus.ERROR));
        log.accept(ProcessEvents.status(now(), AssistantStatus.ERROR.label));
        show(message);
    }

    private void setStatus(AssistantStatus status) {
        if (machine.current() == status) {
            ui.onStatus(machine.text(), machine.confirmVisible(), emotions.idFor(status));
            return;
        }
        machine.set(status);
        ui.onStatus(machine.text(), machine.confirmVisible(), emotions.idFor(status));
        log.accept(ProcessEvents.status(now(), machine.text()));
    }

    private void setIdle() {
        machine.clear();
        ui.onStatus("", false, emotions.idFor(null));
    }

    private void show(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (ReplyGate.withhold(needsLedger, ledgerQueried, text)) {
            return;
        }
        if (!transcript.isEmpty() && transcript.contains(text)) {
            return;
        }
        transcript = transcript.isEmpty() ? text : transcript + "\n" + text;
        ui.onTranscript(transcript);
    }

    private void showOnly(String text) {
        if (ReplyGate.withhold(needsLedger, ledgerQueried, text)) {
            return;
        }
        transcript = text == null ? "" : text;
        ui.onTranscript(transcript);
    }

    /** 用户没点名时间时，把模型写成 this_week 的查账改成全部账本。 */
    private JSONObject spendArgs(String arguments) throws JSONException {
        JSONObject args = parseArgs(arguments);
        if (AssistantIntentPlanner.wantsEntireLedger(turnText)) {
            args.put("period", "all");
            args.remove("start_date");
            args.remove("end_date");
        }
        return args;
    }

    private static boolean querySucceeded(String json) {
        try {
            return !new JSONObject(json).has("error");
        } catch (JSONException e) {
            return false;
        }
    }

    private List<ChatTurn> withSystem() {
        List<ChatTurn> all = new ArrayList<>();
        all.add(ChatTurn.system(docs.systemPromptBody()));
        if (injectedMemory != null && !injectedMemory.isEmpty()) {
            all.add(ChatTurn.system("上一会话要点：\n" + injectedMemory
                    + "\n只使用其中已经写出的事实，不要假装记得没有写入的内容。日记仍须先请求确认。"));
        }
        all.addAll(messages);
        return all;
    }

    private String lastUserText() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role)) {
                return messages.get(i).content;
            }
        }
        return "";
    }

    private static List<ToolCall> order(List<ToolCall> calls) {
        List<ToolCall> copy = new ArrayList<>(calls);
        copy.sort(Comparator.comparingInt(call -> rank(call.name)));
        return copy;
    }

    private static int rank(String name) {
        if ("query_spend".equals(name)) {
            return 0;
        }
        if ("daily_fortune".equals(name)) {
            return 1;
        }
        if ("propose_add_bill".equals(name)) {
            return 2;
        }
        if ("request_diary_context".equals(name)) {
            return 3;
        }
        return 9;
    }

    private static JSONObject parseArgs(String arguments) {
        try {
            return new JSONObject(arguments);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static JSONObject billJson(LedgerBill bill) throws JSONException {
        if (bill == null) {
            return new JSONObject();
        }
        return new JSONObject()
                .put("id", bill.id)
                .put("date", bill.date)
                .put("amount", bill.amount)
                .put("category", bill.category)
                .put("merchant", bill.merchant)
                .put("remark", bill.remark);
    }

    private static String verifySentence(LedgerBill saved, boolean matched) {
        if (saved == null) {
            return "确认后没有读回这条记录。";
        }
        String prefix = matched ? "已写入并回读：" : "已写入，但回读和确认卡不一致：";
        return prefix + saved.date + " " + saved.category + " " + saved.merchant
                + String.format(java.util.Locale.US, " ¥%.2f", saved.amount)
                + "（记录 " + saved.id + "）。";
    }

    private static String now() {
        return OffsetDateTime.now(SHANGHAI).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
