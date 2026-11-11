package com.campusbill.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.campusbill.app.chat.ai.assistant.EmotionHooks;

import com.campusbill.app.chat.ChatAdapter;
import com.campusbill.app.chat.ChatMessage;
import com.campusbill.app.chat.ai.AiConfig;
import com.campusbill.app.chat.ai.AiRemoteClient;
import com.campusbill.app.chat.ai.DeepSeekKeyStore;
import com.campusbill.app.chat.ai.assistant.AssistantCallException;
import com.campusbill.app.chat.ai.assistant.AssistantHost;
import com.campusbill.app.chat.ai.assistant.ChatTurn;
import com.campusbill.app.chat.ai.DeepSeekStreamClient;
import com.campusbill.app.chat.session.ChatSessionBook;
import com.campusbill.app.chat.session.ChatSessionRoom;
import com.campusbill.app.chat.session.ContextUsage;
import com.campusbill.app.chat.session.MemoryHandoff;
import com.campusbill.app.chat.session.ModelWindows;
import com.campusbill.app.chat.session.TokenEstimate;
import com.campusbill.app.ui.ChatBallMetrics;
import com.campusbill.app.ui.ChatInputChrome;
import com.campusbill.app.chat.ai.assistant.AssistantIntentPlanner;
import com.campusbill.app.chat.ai.assistant.AssistantStatus;
import com.campusbill.app.chat.ai.assistant.FailureClassifier;
import com.campusbill.app.chat.ai.BillAnalysisUtils;
import com.campusbill.app.chat.ai.BillContextBuilder;
import com.campusbill.app.chat.ai.IntentMatcher;
import com.campusbill.app.chat.ai.ReplyGenerator;
import com.campusbill.app.data.CategoryAmount;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.data.MerchantAmount;
import com.campusbill.app.data.SelectedMonthStore;
import com.campusbill.app.databinding.ActivityAiChatBinding;
import com.campusbill.app.ui.AttitudeGazeMath;
import com.campusbill.app.ui.BallAttitudeGaze;
import com.campusbill.app.ui.BottomTabHelper;
import com.campusbill.app.ui.PressScaleHelper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 消费助手对话页：本地意图识别 + 本机账单分析，全程不联网、不收集隐私数据。
 */
public class AiChatActivity extends AppCompatActivity {

    private ActivityAiChatBinding binding;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    /** 主页选中的月份（yyyy-MM），预设问题按该月查询账单；缺省为当前月 */
    private String queryMonth = YearMonth.now().toString();
    private AssistantHost assistantHost;
    private int streamIndex = -1;
    private String confirmBody;
    private String lastEmotionId = "02";
    private boolean emotionReady;
    private boolean ballShouldRun;
    private boolean keyboardVisible;
    private boolean confirmCardVisible;
    private boolean replyActive;
    private boolean sawSpendTool;
    private final EmotionHooks ballScenes = EmotionHooks.load("");
    private final Handler gazeHandler = new Handler(Looper.getMainLooper());
    private final Runnable gazeHome = () -> pushGaze(0f, 0f);
    private final Runnable pleasedDone = () -> {
        if (!confirmCardVisible) {
            pushScene("idle");
        }
    };
    private boolean ballGesture;
    private final BallAttitudeGaze attitudeGaze = new BallAttitudeGaze(new BallAttitudeGaze.Host() {
        @Override
        public void onAttitudeGaze(float nx, float ny) {
            gazeHandler.removeCallbacks(gazeHome);
            pushGaze(nx, ny);
        }

        @Override
        public void onAttitudeReturn() {
            gazeHandler.removeCallbacks(gazeHome);
            gazeHandler.postDelayed(gazeHome, AttitudeGazeMath.RETURN_MS);
        }

        @Override
        public boolean fingerOwnsBall() {
            return ballGesture;
        }

        @Override
        public boolean attitudeAllowed() {
            return ballShouldRun
                    && "02".equals(lastEmotionId)
                    && binding != null
                    && binding.emotionBallPanel.getVisibility() == View.VISIBLE
                    && binding.emotionBallPanel.getHeight() > 0;
        }
    });
    /** 向导测通失败时，状态行「重试」再测这一次粘贴的密钥。 */
    private boolean retryKeyProbe;
    private String retryKey = "";
    private ChatSessionBook sessionBook = new ChatSessionBook();
    private boolean sessionReady;
    private boolean restoringSession;
    private int lastPromptTokens;
    private int lastCompletionTokens;
    private boolean handoffRetry;
    private String pendingAfterHandoff = "";
    private float edgeDownX;
    private float edgeDownY;
    private boolean edgeTracking;
    private boolean drawerOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PressScaleHelper.attach(binding.btnSend);
        BottomTabHelper.bind(this, BottomTabHelper.Tab.AI);
        refreshQueryMonth();

        adapter = new ChatAdapter(messages);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnApiKey.setOnClickListener(v -> showDeepSeekWizard());
        binding.btnProcess.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, AssistantProcessActivity.class)));
        setupEmotionBall();
        binding.btnConfirmOk.setOnClickListener(v -> assistantHost.confirm());
        binding.btnConfirmCancel.setOnClickListener(v -> assistantHost.cancel());
        assistantHost = new AssistantHost(this, YearMonth.parse(queryMonth), new AssistantHost.Surface() {
            @Override
            public void onStatus(String statusText, boolean confirmVisible, String emotionId) {
                boolean errorLine = isErrorLine(statusText);
                applyStatusRow(statusText, errorLine);
                retryKeyProbe = false;
                if (!confirmVisible) {
                    confirmBody = null;
                }
                applyConfirmCard(confirmVisible && confirmBody != null);
                if (isErrorLine(statusText)) {
                    replyActive = false;
                    sawSpendTool = false;
                    gazeHandler.removeCallbacks(pleasedDone);
                    pushScene("concerned");
                    return;
                }
                if (statusText == null || statusText.isEmpty()) {
                    if (replyActive) {
                        replyActive = false;
                        pushScene(sawSpendTool && transcriptHasNumber() ? "number" : "pleased");
                        sawSpendTool = false;
                        gazeHandler.postDelayed(pleasedDone, 1500);
                        considerHandoff();
                    } else {
                        pushScene("idle");
                    }
                    return;
                }
                gazeHandler.removeCallbacks(pleasedDone);
                if (AssistantStatus.THINKING.label.equals(statusText)) {
                    replyActive = true;
                    pushScene("thinking");
                } else if (AssistantStatus.TOOL.label.equals(statusText)) {
                    replyActive = true;
                    boolean spend = lastUserAsksSpend();
                    sawSpendTool = sawSpendTool || spend;
                    pushScene(spend ? "query_tool" : "tool");
                } else if (confirmVisible) {
                    replyActive = false;
                    if (emotionId != null && !emotionId.isEmpty()) {
                        lastEmotionId = emotionId;
                    }
                    pushEmotion();
                } else {
                    if (emotionId != null && !emotionId.isEmpty()) {
                        lastEmotionId = emotionId;
                    }
                    pushEmotion();
                }
            }

            @Override
            public void onTranscript(String text) {
                if (streamIndex < 0 || streamIndex >= messages.size()) {
                    streamIndex = addMessage(ChatMessage.TYPE_AI, text);
                } else {
                    replaceMessage(streamIndex, text);
                }
                if (AssistantStatus.THINKING.label.contentEquals(
                        binding.tvAssistantStatus.getText()) && text != null && !text.isEmpty()) {
                    pushScene("streaming");
                }
            }

            @Override
            public void onUsage(int promptTokens, int completionTokens) {
                lastPromptTokens = promptTokens;
                lastCompletionTokens = completionTokens;
                showContextPercent();
            }

            @Override
            public void onConfirmCard(String title, String body) {
                confirmBody = body;
                binding.tvConfirmTitle.setText(title);
                binding.tvConfirmBody.setText(body);
                binding.btnConfirmOk.setText(title != null && title.contains("日记")
                        ? R.string.assistant_confirm_diary : R.string.assistant_confirm_ok);
                if ("等待确认".equals(binding.tvAssistantStatus.getText().toString())) {
                    applyConfirmCard(true);
                }
            }
        });
        binding.btnStatusRetry.setOnClickListener(v -> retryFailedRequest());
        if (!AiConfig.hasApiKey(this)) {
            binding.getRoot().post(this::showDeepSeekWizard);
        }
        binding.btnSend.setOnClickListener(v -> sendMessage(binding.etMessage.getText().toString()));
        binding.btnSessions.setOnClickListener(v -> openDrawer());
        binding.btnNewChat.setOnClickListener(v -> {
            closeDrawer();
            openFreshSession(true);
        });
        binding.drawerScrim.setOnClickListener(v -> closeDrawer());
        binding.memoryTip.setOnClickListener(v -> {
            sessionBook.dismissTip();
            binding.memoryTip.setVisibility(View.GONE);
            persistSession();
        });
        loadSessions();
        binding.chipTotal.setOnClickListener(v -> sendMessage(getString(R.string.quick_total)));
        binding.chipBudget.setOnClickListener(v -> sendMessage(getString(R.string.quick_budget)));
        binding.chipCategory.setOnClickListener(v -> sendMessage(getString(R.string.quick_category)));
        binding.chipAdvice.setOnClickListener(v -> sendMessage(getString(R.string.quick_advice)));
    }

    /** 发送一条问题：用户气泡 + 「正在整理账单」占位，随后按意图填充回复 */
    private void sendMessage(String rawText) {
        if (TextUtils.isEmpty(rawText) || rawText.trim().isEmpty() || !sessionReady) {
            return;
        }
        String query = rawText.trim();
        if (sessionReady && maybeHoldForWindow(query)) {
            return;
        }

        boolean assistantUtterance = !AssistantIntentPlanner.plan(
                query, LocalDate.now(), YearMonth.parse(queryMonth)).isEmpty();
        boolean streamedChat = IntentMatcher.match(query) == IntentMatcher.Intent.FALLBACK
                && AiConfig.hasApiKey(this);
        if (assistantUtterance || streamedChat) {
            if (!assistantHost.startOrIgnore(query, () ->
                    Toast.makeText(this, R.string.assistant_busy, Toast.LENGTH_SHORT).show())) {
                return;
            }
            binding.etMessage.setText("");
            addMessage(ChatMessage.TYPE_USER, query);
            streamIndex = -1;
            return;
        }
        binding.etMessage.setText("");

        addMessage(ChatMessage.TYPE_USER, query);
        boolean online = false;
        int placeholderIndex = addMessage(ChatMessage.TYPE_AI, getString(online
                ? R.string.chat_thinking_online : R.string.chat_thinking));
        dispatch(query, placeholderIndex);
    }

    /** 意图分发：按优先级匹配后查询本机账单，生成对应回复（按主页选中月份） */
    private void dispatch(String text, int index) {
        IntentMatcher.Intent intent = IntentMatcher.match(text);
        String month = queryMonth;
        String today = LocalDate.now().toString();

        switch (intent) {
            case BUDGET:
                BillAnalysisUtils.getMonthTotalExpense(this, month,
                        new DBHelper.DataCallback<Double>() {
                            @Override
                            public void onSuccess(Double expense) {
                                replaceMessage(index, ReplyGenerator.budget(
                                        BillAnalysisUtils.getBudget(AiChatActivity.this),
                                        expense == null ? 0 : expense));
                            }

                            @Override
                            public void onError(Exception e) {
                                showError(index);
                            }
                        });
                break;
            case TOTAL:
                if (IntentMatcher.isTodayQuery(text)) {
                    BillAnalysisUtils.getDayTotalExpense(this, today,
                            new DBHelper.DataCallback<Double>() {
                                @Override
                                public void onSuccess(Double expense) {
                                    replaceMessage(index, ReplyGenerator.totalDay(
                                            today, expense == null ? 0 : expense));
                                }

                                @Override
                                public void onError(Exception e) {
                                    showError(index);
                                }
                            });
                } else {
                    BillAnalysisUtils.getMonthTotalExpense(this, month,
                            new DBHelper.DataCallback<Double>() {
                                @Override
                                public void onSuccess(Double expense) {
                                    replaceMessage(index, ReplyGenerator.totalMonth(
                                            month, expense == null ? 0 : expense));
                                }

                                @Override
                                public void onError(Exception e) {
                                    showError(index);
                                }
                            });
                }
                break;
            case CATEGORY:
                BillAnalysisUtils.getCategoryRank(this, month,
                        new DBHelper.DataCallback<List<CategoryAmount>>() {
                            @Override
                            public void onSuccess(List<CategoryAmount> list) {
                                replaceMessage(index, ReplyGenerator.category(month, list));
                            }

                            @Override
                            public void onError(Exception e) {
                                showError(index);
                            }
                        });
                break;
            case RANK:
                BillAnalysisUtils.getMerchantTop(this, month, 3,
                        new DBHelper.DataCallback<List<MerchantAmount>>() {
                            @Override
                            public void onSuccess(List<MerchantAmount> list) {
                                replaceMessage(index, ReplyGenerator.merchantTop(month, list));
                            }

                            @Override
                            public void onError(Exception e) {
                                showError(index);
                            }
                        });
                break;
            case ADVICE:
                BillAnalysisUtils.getMonthTotalExpense(this, month,
                        new DBHelper.DataCallback<Double>() {
                            @Override
                            public void onSuccess(Double expense) {
                                double total = expense == null ? 0 : expense;
                                BillAnalysisUtils.getCategoryRank(AiChatActivity.this, month,
                                        new DBHelper.DataCallback<List<CategoryAmount>>() {
                                            @Override
                                            public void onSuccess(List<CategoryAmount> list) {
                                                replaceMessage(index,
                                                        ReplyGenerator.advice(month, total, list));
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                showError(index);
                                            }
                                        });
                            }

                            @Override
                            public void onError(Exception e) {
                                showError(index);
                            }
                        });
                break;
            case HELP:
                replaceMessage(index, ReplyGenerator.help());
                break;
            default:
                replyOnlineOrFallback(text, index);
                break;
        }
    }

    /** 账单外的问题：已配置 API KEY 时联网获取 AI 回复，失败则退回本地兜底文案 */
    private void replyOnlineOrFallback(String text, int index) {
        if (!AiConfig.hasApiKey(this)) {
            replaceMessage(index, ReplyGenerator.fallback());
            return;
        }
        // 联网前先从本机数据库构建账单快照，注入 system prompt，
        // 让远程 AI 基于用户真实账单作答，而不是凭空编造。
        BillContextBuilder.build(this, queryMonth, billContext ->
                AiRemoteClient.requestReply(
                        AiConfig.getApiKey(this),
                        AiConfig.getBaseUrl(this),
                        AiConfig.getModel(this),
                        buildHistory(index),
                        text,
                        billContext,
                        new AiRemoteClient.Callback() {
                            @Override
                            public void onReply(String reply) {
                                replaceMessage(index, reply);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                replaceMessage(index, ReplyGenerator.fallback() + "\n"
                                        + getString(R.string.ai_online_failed));
                            }
                        }));
    }

    /** 组装最近几轮对话作为联网上下文（跳过欢迎语与占位消息） */
    private List<String[]> buildHistory(int placeholderIndex) {
        List<String[]> history = new ArrayList<>();
        String welcome = getString(R.string.chat_welcome);
        for (int i = 0; i < messages.size(); i++) {
            if (i == placeholderIndex || i == placeholderIndex - 1) {
                continue;
            }
            ChatMessage message = messages.get(i);
            if (message.type == ChatMessage.TYPE_AI && welcome.equals(message.content)) {
                continue;
            }
            history.add(new String[]{
                    message.type == ChatMessage.TYPE_USER ? "user" : "assistant",
                    message.content});
        }
        return history;
    }

    private void showError(int index) {
        replaceMessage(index, getString(R.string.chat_db_error));
    }

    /** 追加一条消息并滚动到最新 */
    private int addMessage(int type, String content) {
        messages.add(new ChatMessage(type, content));
        int index = messages.size() - 1;
        adapter.notifyItemInserted(index);
        scrollToLatest();
        if (sessionReady && !restoringSession && sessionBook.current() != null) {
            sessionBook.append(type, content);
            persistSession();
        }
        return index;
    }

    /** 用最终回复替换占位消息 */
    private void replaceMessage(int index, String content) {
        if (index < 0 || index >= messages.size()) {
            return;
        }
        messages.set(index, new ChatMessage(ChatMessage.TYPE_AI, content));
        adapter.notifyItemChanged(index);
        scrollToLatest();
        if (sessionReady && !restoringSession && sessionBook.current() != null
                && index == sessionBook.current().messages.size() - 1) {
            sessionBook.replaceLast(content);
            persistSession();
        }
    }

    /** 对话页球面板。键盘或确认卡时压到 160dp，高度不再低于 160dp。 */
    private void setupEmotionBall() {
        WebSettings settings = binding.emotionBall.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        binding.emotionBall.setBackgroundColor(Color.TRANSPARENT);
        binding.emotionBall.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                emotionReady = true;
                pushEmotion();
                pushBallActive(ballShouldRun);
            }
        });
        binding.emotionBall.loadUrl("file:///android_asset/emotion-ball/host.html");
        binding.getRoot().addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (bottom - top != oldBottom - oldTop) {
                        applyBallPanelHeight();
                    }
                });
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            boolean ime = insets.isVisible(WindowInsetsCompat.Type.ime());
            int bottom = ime ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom : 0;
            keyboardVisible = ime;
            if (v.getPaddingBottom() != bottom) {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            }
            applyBallPanelHeight();
            if (ime) {
                binding.etMessage.post(() -> {
                    Rect shown = new Rect(0, 0, binding.etMessage.getWidth(),
                            binding.etMessage.getHeight());
                    binding.etMessage.requestRectangleOnScreen(shown, true);
                });
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
        applyBallPanelHeight();
    }

    private void applyConfirmCard(boolean show) {
        confirmCardVisible = show;
        binding.confirmCard.setVisibility(show ? View.VISIBLE : View.GONE);
        applyBallPanelHeight();
    }

    private void applyBallPanelHeight() {
        View root = binding.getRoot();
        int windowPx = root.getHeight();
        if (windowPx <= 0) {
            windowPx = getResources().getDisplayMetrics().heightPixels;
        }
        int above = binding.emotionBallPanel.getTop();
        if (above <= 0) {
            above = dp(96);
        }
        int inputHeight = binding.inputColumn.getHeight();
        if (inputHeight <= 0) {
            inputHeight = dp(168);
        }
        int confirmHeight = 0;
        if (confirmCardVisible) {
            confirmHeight = binding.confirmCard.getHeight();
            if (confirmHeight <= 0) {
                confirmHeight = dp(120);
            }
        }
        int tabHeight = 0;
        if (!keyboardVisible) {
            View tabs = findViewById(R.id.bottomTabs);
            tabHeight = tabs == null ? 0 : tabs.getHeight();
            if (tabHeight <= 0) {
                tabHeight = dp(56);
            }
        }
        int target = ChatBallMetrics.heightPx(
                windowPx,
                root.getPaddingBottom(),
                above,
                inputHeight,
                confirmHeight,
                tabHeight,
                keyboardVisible || confirmCardVisible,
                getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams params = binding.emotionBallPanel.getLayoutParams();
        if (params.height != target) {
            params.height = target;
            binding.emotionBallPanel.setLayoutParams(params);
        }
        attitudeGaze.onConditionsChanged();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** 按下缩到 0.92、换成深色并亮出 3dp 光晕。不把焦点交给输入框，避免弹出键盘。 */
    /** 手指还按着，这句话说完了就接着听，不换引擎，也不把球从 35 挪走。 */
    /** 按住时只把已识别的字放进输入栏，不改四档状态，也不发送。 */
    private boolean lastUserAsksSpend() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.type != ChatMessage.TYPE_USER || message.content == null) {
                continue;
            }
            String text = message.content;
            return text.contains("花") || text.contains("多少") || text.contains("食堂")
                    || text.contains("支出");
        }
        return false;
    }

    private boolean transcriptHasNumber() {
        if (streamIndex < 0 || streamIndex >= messages.size()) {
            return false;
        }
        String text = messages.get(streamIndex).content;
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void pushScene(String scene) {
        String id = ballScenes.idForScene(scene);
        if (id == null || !id.matches("\\d{2}")) {
            id = "02";
        }
        lastEmotionId = id;
        pushEmotion();
    }

    @Override
    protected void onDestroy() {
        attitudeGaze.stop();
        gazeHandler.removeCallbacks(pleasedDone);
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (trackDrawerEdge(event)) {
            return true;
        }
        trackGaze(event);
        return super.dispatchTouchEvent(event);
    }

    private void trackGaze(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (touchHitsBall(event)) {
                ballGesture = true;
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            ballGesture = false;
            if (!attitudeGaze.isDriving()) {
                gazeHandler.removeCallbacks(gazeHome);
                gazeHandler.postDelayed(gazeHome, AttitudeGazeMath.RETURN_MS);
            }
            return;
        }
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE
                && action != MotionEvent.ACTION_POINTER_DOWN) {
            return;
        }
        if (!ballGesture && attitudeGaze.isDriving()) {
            return;
        }
        if (binding.emotionBall.getWidth() <= 0 || binding.emotionBall.getHeight() <= 0) {
            return;
        }
        gazeHandler.removeCallbacks(gazeHome);
        int[] location = new int[2];
        binding.emotionBall.getLocationOnScreen(location);
        float centerX = location[0] + binding.emotionBall.getWidth() / 2f;
        float centerY = location[1] + binding.emotionBall.getHeight() / 2f;
        float nx = clampUnit((event.getRawX() - centerX) / (binding.emotionBall.getWidth() / 2f));
        float ny = clampUnit((event.getRawY() - centerY) / (binding.emotionBall.getHeight() / 2f));
        pushGaze(nx, ny);
    }

    private boolean touchHitsBall(MotionEvent event) {
        if (binding.emotionBallPanel.getWidth() <= 0 || binding.emotionBallPanel.getHeight() <= 0) {
            return false;
        }
        int[] location = new int[2];
        binding.emotionBallPanel.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        return x >= location[0] && x < location[0] + binding.emotionBallPanel.getWidth()
                && y >= location[1] && y < location[1] + binding.emotionBallPanel.getHeight();
    }

    private static float clampUnit(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }

    private void pushEmotion() {
        if (!emotionReady) {
            return;
        }
        String id = lastEmotionId == null ? "02" : lastEmotionId.replaceAll("[^0-9]", "");
        if (id.isEmpty()) {
            id = "02";
        }
        binding.emotionBall.evaluateJavascript(
                "window.setAssistantEmotion&&window.setAssistantEmotion('" + id + "')", null);
        attitudeGaze.onConditionsChanged();
    }

    private void pushGaze(float nx, float ny) {
        if (!emotionReady || !ballShouldRun) {
            return;
        }
        String script = String.format(java.util.Locale.US,
                "window.setGaze&&window.setGaze(%f,%f)", nx, ny);
        binding.emotionBall.evaluateJavascript(script, null);
    }

    private void pushBallActive(boolean active) {
        ballShouldRun = active;
        if (!emotionReady) {
            return;
        }
        binding.emotionBall.evaluateJavascript(
                "window.setBallActive&&window.setBallActive(" + active + ")", null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refreshQueryMonth();
    }

    private void refreshQueryMonth() {
        queryMonth = SelectedMonthStore.get(this).toString();
        if (assistantHost != null) {
            assistantHost.setChartMonth(YearMonth.parse(queryMonth));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshQueryMonth();
        pushBallActive(true);
        attitudeGaze.setPageVisible(this, true);
        attitudeGaze.onConditionsChanged();
    }

    @Override
    protected void onPause() {
        saveScroll();
        attitudeGaze.setPageVisible(this, false);
        gazeHandler.removeCallbacks(gazeHome);
        pushBallActive(false);
        super.onPause();
    }

    /** 出错行右侧的「重试」：再测密钥，或再发一次失败的对话请求。 */
    private void retryFailedRequest() {
        if (handoffRetry) {
            extractMemory(true, pendingAfterHandoff);
            return;
        }
        if (retryKeyProbe) {
            probeDeepSeekKey(retryKey, true);
            return;
        }
        assistantHost.retry(() ->
                Toast.makeText(this, R.string.assistant_busy, Toast.LENGTH_SHORT).show());
    }

    private void probeDeepSeekKey(String apiKey, boolean fromStatusLine) {
        if (fromStatusLine) {
            applyStatusRow(AssistantStatus.THINKING.label, false);
        }
        new Thread(() -> {
            try {
                com.campusbill.app.chat.ai.DeepSeekKeyProbe.check(apiKey);
                AiConfig.saveSettings(this, apiKey,
                        AiConfig.DEFAULT_BASE_URL, AiConfig.DEFAULT_MODEL);
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    retryKeyProbe = false;
                    if (isErrorLine(binding.tvAssistantStatus.getText().toString())
                            || AssistantStatus.THINKING.label.contentEquals(
                            binding.tvAssistantStatus.getText())) {
                        applyStatusRow("", false);
                    }
                    Toast.makeText(this, R.string.deepseek_ok, Toast.LENGTH_SHORT).show();
                });
            } catch (AssistantCallException e) {
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    retryKey = apiKey == null ? "" : apiKey;
                    retryKeyProbe = true;
                    showErrorLine(FailureClassifier.statusLine(e.kind));
                });
            }
        }, "deepseek-key-probe").start();
    }

    private void applyStatusRow(CharSequence text, boolean retry) {
        boolean show = ChatInputChrome.reserveStatus(text == null ? "" : text.toString());
        binding.tvAssistantStatus.setText(show ? text : "");
        binding.btnStatusRetry.setVisibility(show && retry ? View.VISIBLE : View.GONE);
        binding.statusRow.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showErrorLine(String line) {
        applyStatusRow(line, true);
        confirmBody = null;
        applyConfirmCard(false);
    }

    private static boolean isErrorLine(String text) {
        return "出错".equals(text) || "密钥失败".equals(text)
                || "余额不足".equals(text) || "网络错误".equals(text);
    }

    /** DeepSeek 专用向导：测通后才写入 Keystore。不提供其他厂商。 */
    private void showDeepSeekWizard() {
        if (isFinishing()) {
            return;
        }
        View content = getLayoutInflater().inflate(R.layout.dialog_deepseek_key, null);
        EditText etKey = content.findViewById(R.id.etDeepSeekKey);
        TextView tvResult = content.findViewById(R.id.tvDeepSeekResult);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.deepseek_wizard_title)
                .setView(content)
                .setPositiveButton(R.string.deepseek_test_save, null)
                .setNeutralButton(R.string.deepseek_clear, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
        dialog.setOnShowListener(shown -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String apiKey = etKey.getText().toString().trim();
                tvResult.setText(R.string.deepseek_testing);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                new Thread(() -> {
                    try {
                        com.campusbill.app.chat.ai.DeepSeekKeyProbe.check(apiKey);
                        AiConfig.saveSettings(this, apiKey,
                                AiConfig.DEFAULT_BASE_URL, AiConfig.DEFAULT_MODEL);
                        runOnUiThread(() -> {
                            if (isFinishing()) {
                                return;
                            }
                            retryKeyProbe = false;
                            tvResult.setText(R.string.deepseek_ok);
                            if (isErrorLine(binding.tvAssistantStatus.getText().toString())) {
                                applyStatusRow("", false);
                            }
                            Toast.makeText(this, R.string.deepseek_ok, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } catch (AssistantCallException e) {
                        runOnUiThread(() -> {
                            if (isFinishing()) {
                                return;
                            }
                            retryKey = apiKey;
                            retryKeyProbe = true;
                            showErrorLine(FailureClassifier.statusLine(e.kind));
                            tvResult.setText(e.getMessage());
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        });
                    }
                }, "deepseek-key-probe").start();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                DeepSeekKeyStore.clear(this);
                etKey.setText("");
                tvResult.setText(R.string.deepseek_cleared);
            });
        });
        dialog.show();
    }

    /** 滚动到最新一条消息 */
    private void scrollToLatest() {
        binding.rvMessages.post(() ->
                binding.rvMessages.scrollToPosition(Math.max(0, messages.size() - 1)));
    }

    private boolean welcomeOnly(ChatSessionBook.Session session) {
        if (session == null || session.messages.size() != 1) {
            return false;
        }
        ChatSessionBook.Message message = session.messages.get(0);
        return message.type == ChatSessionBook.TYPE_AI
                && getString(R.string.chat_welcome).equals(message.content);
    }

    private void loadSessions() {
        new Thread(() -> {
            ChatSessionBook loaded = ChatSessionRoom.load(this);
            long saved = ChatSessionRoom.savedId(this);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                sessionBook = loaded;
                ChatSessionBook.Session session = sessionBook.restore(saved);
                sessionReady = true;
                if (session.messages.isEmpty() || welcomeOnly(session)) {
                    if (welcomeOnly(session)) {
                        session.messages.clear();
                    }
                    messages.clear();
                    adapter.notifyDataSetChanged();
                    pushScene("idle");
                } else {
                    showStoredSession(session, false);
                }
                persistSession();
            });
        }, "chat-session-load").start();
    }

    private void showStoredSession(ChatSessionBook.Session session, boolean stickToEnd) {
        restoringSession = true;
        messages.clear();
        adapter.notifyDataSetChanged();
        for (ChatSessionBook.Message message : session.messages) {
            messages.add(new ChatMessage(message.type, message.content));
        }
        adapter.notifyDataSetChanged();
        restoringSession = false;
        java.util.ArrayList<ChatTurn> turns = new java.util.ArrayList<>();
        for (ChatSessionBook.Message message : session.messages) {
            if (message.type == ChatSessionBook.TYPE_USER) {
                turns.add(ChatTurn.user(message.content));
            } else if (message.content != null && !message.content.equals(getString(R.string.chat_welcome))) {
                turns.add(ChatTurn.assistant(message.content));
            }
        }
        assistantHost.setInjectedMemory(session.injectedMemory);
        assistantHost.restoreTurns(turns);
        binding.memoryTip.setVisibility(session.showMemoryTip ? View.VISIBLE : View.GONE);
        if (stickToEnd || session.scrollPosition <= 0) {
            scrollToLatest();
        } else {
            binding.rvMessages.scrollToPosition(session.scrollPosition);
        }
        pushScene("idle");
    }

    private void openFreshSession(boolean fromUser) {
        sessionBook.open(fromUser);
        showStoredSession(sessionBook.current(), true);
        lastPromptTokens = 0;
        lastCompletionTokens = 0;
        showContextPercent();
        persistSession();
    }

    private void persistSession() {
        if (!sessionReady) {
            return;
        }
        ChatSessionBook snapshot = sessionBook;
        new Thread(() -> ChatSessionRoom.save(this, snapshot), "chat-session-save").start();
    }

    private void saveScroll() {
        if (!sessionReady || sessionBook.current() == null) {
            return;
        }
        if (binding.rvMessages.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager manager = (LinearLayoutManager) binding.rvMessages.getLayoutManager();
            int position = manager.findFirstVisibleItemPosition();
            if (position >= 0) {
                sessionBook.setScroll(position);
                persistSession();
            }
        }
    }

    private int contextWindow() {
        return ModelWindows.tokensFor(AiConfig.getModel(this));
    }

    private void showContextPercent() {
        int occupied = lastPromptTokens + lastCompletionTokens;
        int percent = ContextUsage.percent(occupied, contextWindow());
        String label = occupied <= 0 ? "" : getString(R.string.chat_context_percent, percent);
        if (!ChatInputChrome.reserveContext(label)) {
            binding.tvContextUsage.setText("");
            binding.tvContextUsage.setVisibility(View.GONE);
            return;
        }
        binding.tvContextUsage.setVisibility(View.VISIBLE);
        binding.tvContextUsage.setText(label);
        int color = R.color.text_secondary;
        if (ContextUsage.level(percent) == ContextUsage.Level.FULL) {
            color = R.color.color_warning;
        } else if (ContextUsage.level(percent) == ContextUsage.Level.HINT) {
            color = R.color.color_context_hint;
        }
        binding.tvContextUsage.setTextColor(getColor(color));
    }

    private boolean maybeHoldForWindow(String query) {
        MemoryHandoff.Step step = MemoryHandoff.beforeSend(
                lastPromptTokens, TokenEstimate.ofText(query), contextWindow(), sessionBook.memoryReady());
        if (step == MemoryHandoff.Step.NONE) {
            return false;
        }
        if (step == MemoryHandoff.Step.AUTO_SWITCH) {
            openFreshSession(true);
            return false;
        }
        pendingAfterHandoff = query;
        extractMemory(true, query);
        return true;
    }

    private void considerHandoff() {
        if (!sessionReady) {
            return;
        }
        int percent = ContextUsage.percent(lastPromptTokens + lastCompletionTokens, contextWindow());
        showContextPercent();
        MemoryHandoff.Step step = MemoryHandoff.afterReply(percent, sessionBook.memoryReady());
        if (step == MemoryHandoff.Step.PRESTORE) {
            extractMemory(false, "");
        } else if (step == MemoryHandoff.Step.AUTO_SWITCH) {
            openFreshSession(true);
        } else if (step == MemoryHandoff.Step.EXTRACT_THEN_SWITCH) {
            extractMemory(true, "");
        }
    }

    private void extractMemory(boolean switchAfter, String sendAfter) {
        StringBuilder transcript = new StringBuilder();
        for (ChatMessage message : messages) {
            transcript.append(message.type == ChatMessage.TYPE_USER ? "用户：" : "助手：")
                    .append(message.content == null ? "" : message.content)
                    .append('\n');
        }
        long source = sessionBook.currentId();
        if (switchAfter) {
            handoffRetry = false;
            applyStatusRow(AssistantStatus.THINKING.label, false);
            pushScene("thinking");
        }
        DeepSeekStreamClient client = new DeepSeekStreamClient(this);
        new Thread(() -> {
            try {
                java.util.List<ChatTurn> turns = java.util.Arrays.asList(
                        ChatTurn.system("只输出需要长期记住的要点，每条一行。包括消费习惯、已确认账单、偏好和仍有效的约束。不要复述整段原文，不要写密钥。"),
                        ChatTurn.user(transcript.toString()));
                com.campusbill.app.chat.ai.assistant.StreamResult result =
                        client.stream(turns, "[]", null);
                String points = result.content == null ? "" : result.content.trim();
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    if (points.isEmpty()) {
                        if (switchAfter) {
                            failHandoff(sendAfter);
                        }
                        return;
                    }
                    sessionBook.remember(points, source);
                    handoffRetry = false;
                    if (switchAfter) {
                        applyStatusRow("", false);
                        openFreshSession(true);
                        if (sendAfter != null && !sendAfter.isEmpty()) {
                            sendMessage(sendAfter);
                        }
                    } else {
                        persistSession();
                    }
                });
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && switchAfter) {
                        failHandoff(sendAfter);
                    }
                });
            }
        }, "chat-memory").start();
    }

    private void failHandoff(String sendAfter) {
        pendingAfterHandoff = sendAfter == null ? "" : sendAfter;
        handoffRetry = true;
        showErrorLine(AssistantStatus.ERROR.label);
        pushScene("concerned");
    }

    private void openDrawer() {
        int screen = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(Math.round(screen * 0.8f), dp(320));
        ViewGroup.LayoutParams params = binding.historyDrawer.getLayoutParams();
        params.width = width;
        binding.historyDrawer.setLayoutParams(params);
        renderSessionList();
        binding.drawerScrim.setVisibility(View.VISIBLE);
        binding.historyDrawer.setVisibility(View.VISIBLE);
        binding.historyDrawer.setTranslationX(-width);
        binding.historyDrawer.animate().translationX(0f).setDuration(220).start();
        drawerOpen = true;
    }

    private void closeDrawer() {
        if (!drawerOpen) {
            binding.historyDrawer.setVisibility(View.GONE);
            binding.drawerScrim.setVisibility(View.GONE);
            return;
        }
        float width = binding.historyDrawer.getWidth();
        binding.historyDrawer.animate().translationX(-width).setDuration(220).withEndAction(() -> {
            binding.historyDrawer.setVisibility(View.GONE);
            binding.drawerScrim.setVisibility(View.GONE);
        }).start();
        drawerOpen = false;
    }

    private void renderSessionList() {
        binding.sessionList.removeAllViews();
        for (ChatSessionBook.Session session : sessionBook.historyNewestFirst()) {
            TextView row = new TextView(this);
            String title = session.title == null || session.title.isEmpty()
                    ? getString(R.string.chat_untitled) : session.title;
            row.setText(title);
            row.setTextSize(15f);
            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setBackgroundResource(R.drawable.bg_card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            row.setLayoutParams(lp);
            boolean current = session.id == sessionBook.currentId();
            row.setTextColor(getColor(current ? R.color.color_primary : R.color.text_primary));
            row.setTypeface(null, current ? Typeface.BOLD : Typeface.NORMAL);
            row.setOnClickListener(v -> {
                sessionBook.select(session.id);
                showStoredSession(sessionBook.current(), false);
                persistSession();
                closeDrawer();
            });
            row.setOnLongClickListener(v -> {
                askRename(session.id);
                return true;
            });
            row.setOnTouchListener((v, event) -> trackRowSwipe(v, event, session.id));
            binding.sessionList.addView(row);
        }
    }

    private void askRename(long id) {
        ChatSessionBook.Session session = sessionBook.find(id);
        if (session == null) {
            return;
        }
        EditText input = new EditText(this);
        input.setText(session.title);
        new AlertDialog.Builder(this)
                .setTitle(R.string.chat_rename_title)
                .setView(input)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    sessionBook.rename(id, input.getText().toString());
                    persistSession();
                    renderSessionList();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .setNeutralButton(R.string.chat_delete, (dialog, which) -> confirmDelete(id))
                .show();
    }

    private void confirmDelete(long id) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.chat_delete_confirm)
                .setPositiveButton(R.string.chat_delete, (dialog, which) -> {
                    boolean current = id == sessionBook.currentId();
                    sessionBook.delete(id);
                    if (current) {
                        showStoredSession(sessionBook.current(), true);
                    }
                    persistSession();
                    renderSessionList();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private boolean trackRowSwipe(View view, MotionEvent event, long id) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            view.setTag(event.getRawX());
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP && view.getTag() instanceof Float) {
            if (event.getRawX() - (Float) view.getTag() < -dp(72)) {
                confirmDelete(id);
                return true;
            }
        }
        return false;
    }

    private boolean trackDrawerEdge(MotionEvent event) {
        if (drawerOpen) {
            return false;
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            edgeTracking = event.getX() < dp(24);
            edgeDownX = event.getRawX();
            edgeDownY = event.getRawY();
            return false;
        }
        if (!edgeTracking) {
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = event.getRawX() - edgeDownX;
            float dy = event.getRawY() - edgeDownY;
            if (dx > dp(48) && Math.abs(dx) > Math.abs(dy)) {
                edgeTracking = false;
                openDrawer();
                return true;
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            edgeTracking = false;
        }
        return false;
    }
}
