package com.campusbill.app;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private boolean voiceHolding;
    private boolean voiceResultSent;
    private boolean recognizerReady;
    private boolean pendingStop;
    private boolean replyActive;
    private boolean sawSpendTool;
    private int pendingVoiceError = -1;
    private String latestPartial = "";
    private String finalTranscript = "";
    private SpeechRecognizer speechRecognizer;
    private final EmotionHooks ballScenes = EmotionHooks.load("");
    private final Handler gazeHandler = new Handler(Looper.getMainLooper());
    private final Runnable gazeHome = () -> pushGaze(0f, 0f);
    private final Runnable pleasedDone = () -> {
        if (!voiceHolding && !confirmCardVisible) {
            pushScene("idle");
        }
    };
    private final Runnable voiceFail = () -> finishVoiceAfterRelease();
    private final Runnable readyTimeout = () -> {
        if (!voiceResultSent && pendingStop) {
            pendingStop = false;
            finishVoiceAfterRelease();
        }
    };
    private static final int REQUEST_MIC = 41;
    private boolean ballGesture;
    private final ArrayList<ListenEngine> listenEngines = new ArrayList<>();
    private int listenEngineIndex = -1;
    private int listenGeneration;
    private boolean recognizersExhausted;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PressScaleHelper.attach(binding.btnSend);
        BottomTabHelper.bind(this, BottomTabHelper.Tab.AI);
        refreshQueryMonth();

        adapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
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
                binding.tvAssistantStatus.setText(statusText);
                boolean errorLine = isErrorLine(statusText);
                binding.btnStatusRetry.setVisibility(errorLine ? View.VISIBLE : View.GONE);
                retryKeyProbe = false;
                if (!confirmVisible) {
                    confirmBody = null;
                }
                applyConfirmCard(confirmVisible && confirmBody != null);
                if (voiceHolding) {
                    pushScene("listening");
                    return;
                }
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
                if (!voiceHolding && AssistantStatus.THINKING.label.contentEquals(
                        binding.tvAssistantStatus.getText()) && text != null && !text.isEmpty()) {
                    pushScene("streaming");
                }
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
        binding.btnHoldTalk.setOnTouchListener(this::onHoldTalk);
        if (!AiConfig.hasApiKey(this)) {
            binding.getRoot().post(this::showDeepSeekWizard);
        }
        binding.btnSend.setOnClickListener(v -> sendMessage(binding.etMessage.getText().toString()));
        binding.chipTotal.setOnClickListener(v -> sendMessage(getString(R.string.quick_total)));
        binding.chipBudget.setOnClickListener(v -> sendMessage(getString(R.string.quick_budget)));
        binding.chipCategory.setOnClickListener(v -> sendMessage(getString(R.string.quick_category)));
        binding.chipAdvice.setOnClickListener(v -> sendMessage(getString(R.string.quick_advice)));

        addMessage(ChatMessage.TYPE_AI, getString(R.string.chat_welcome));
    }

    /** 发送一条问题：用户气泡 + 「正在整理账单」占位，随后按意图填充回复 */
    private void sendMessage(String rawText) {
        if (TextUtils.isEmpty(rawText) || rawText.trim().isEmpty()) {
            return;
        }
        String query = rawText.trim();

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
    }

    /** 对话页球面板。键盘或确认卡出现时压到 160dp，不盖住状态行和确认卡。 */
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
        int room = windowPx - root.getPaddingBottom() - above - inputHeight
                - confirmHeight - tabHeight - dp(12);
        boolean compress = keyboardVisible || confirmCardVisible;
        int preferred = compress ? dp(160) : Math.round(windowPx / 3f);
        int target = Math.min(preferred, Math.max(0, room));
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

    private boolean onHoldTalk(View view, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && view.getParent() != null) {
            view.getParent().requestDisallowInterceptTouchEvent(true);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setMicPressed(true);
                beginVoiceHold();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setMicPressed(false);
                endVoiceHold();
                return true;
            default:
                return true;
        }
    }

    /** 按下缩到 0.92、换成深色并亮出 3dp 光晕。不把焦点交给输入框，避免弹出键盘。 */
    private void setMicPressed(boolean pressed) {
        binding.micHalo.setVisibility(pressed ? View.VISIBLE : View.INVISIBLE);
        binding.micFace.setBackgroundResource(pressed
                ? R.drawable.bg_mic_pressed : R.drawable.bg_mic_rest);
        binding.micFace.animate().cancel();
        binding.micFace.animate()
                .scaleX(pressed ? 0.92f : 1f)
                .scaleY(pressed ? 0.92f : 1f)
                .setDuration(120)
                .start();
        binding.btnHoldTalk.setContentDescription(getString(pressed
                ? R.string.assistant_hold_release : R.string.assistant_hold_talk));
    }

    private void beginVoiceHold() {
        voiceHolding = true;
        voiceResultSent = false;
        recognizerReady = false;
        pendingStop = false;
        pendingVoiceError = -1;
        latestPartial = "";
        finalTranscript = "";
        gazeHandler.removeCallbacks(voiceFail);
        gazeHandler.removeCallbacks(readyTimeout);
        gazeHandler.removeCallbacks(pleasedDone);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            voiceHolding = false;
            pushScene("idle");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        listenEngines.clear();
        listenEngineIndex = -1;
        recognizersExhausted = false;
        buildListenEngines();
        if (!startNextListenEngine()) {
            voiceHolding = false;
            showVoiceError(getString(R.string.assistant_voice_none));
            return;
        }
        pushScene("listening");
    }

    private void endVoiceHold() {
        if (!voiceHolding && !pendingStop) {
            return;
        }
        voiceHolding = false;
        String readyText = firstNonEmpty(finalTranscript, latestPartial);
        if (pendingVoiceError >= 0 && readyText.isEmpty()) {
            pendingStop = false;
            finishVoiceAfterRelease();
            return;
        }
        if (!readyText.isEmpty()) {
            pendingStop = false;
            deliverRecognized(readyText);
            cancelListening();
            return;
        }
        if (!recognizerReady) {
            pendingStop = true;
            gazeHandler.removeCallbacks(readyTimeout);
            gazeHandler.postDelayed(readyTimeout, 4000);
            return;
        }
        stopListeningWhenReady();
    }

    private void stopListeningWhenReady() {
        if (speechRecognizer == null) {
            finishVoiceAfterRelease();
            return;
        }
        try {
            speechRecognizer.stopListening();
        } catch (RuntimeException e) {
            finishVoiceAfterRelease();
        }
    }

    private void buildListenEngines() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            listenEngines.add(new ListenEngine(null, false));
        }
        List<ResolveInfo> services = getPackageManager().queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), 0);
        if (services != null) {
            List<ResolveInfo> ranked = new ArrayList<>(services);
            ranked.sort((left, right) -> Integer.compare(scoreRecognizer(right), scoreRecognizer(left)));
            for (ResolveInfo info : ranked) {
                if (info.serviceInfo == null || info.serviceInfo.packageName == null
                        || info.serviceInfo.name == null) {
                    continue;
                }
                listenEngines.add(new ListenEngine(
                        new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name),
                        false));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listenEngines.add(new ListenEngine(null, true));
        }
    }

    private static int scoreRecognizer(ResolveInfo info) {
        if (info == null || info.serviceInfo == null || info.serviceInfo.packageName == null) {
            return 0;
        }
        String pkg = info.serviceInfo.packageName.toLowerCase(Locale.US);
        if (pkg.contains("huawei") || pkg.contains("hihonor") || pkg.contains("honor")) {
            return 50;
        }
        if (pkg.contains("iflytek") || pkg.contains("xfyun")) {
            return 40;
        }
        if (pkg.contains("baidu")) {
            return 30;
        }
        if (pkg.contains("google")) {
            return 20;
        }
        return 1;
    }

    private boolean startNextListenEngine() {
        while (listenEngineIndex + 1 < listenEngines.size()) {
            listenEngineIndex++;
            if (startListenEngine(listenEngines.get(listenEngineIndex))) {
                return true;
            }
        }
        return false;
    }

    private boolean startListenEngine(ListenEngine engine) {
        destroyRecognizer();
        final int generation = ++listenGeneration;
        try {
            if (engine.onDevice) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    return false;
                }
                speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            } else if (engine.component != null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, engine.component);
            } else {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            }
            recognizerReady = false;
            speechRecognizer.setRecognitionListener(recognitionListener(generation));
            speechRecognizer.startListening(listenIntent());
            return true;
        } catch (RuntimeException e) {
            destroyRecognizer();
            return false;
        }
    }

    private Intent listenIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        return intent;
    }

    private RecognitionListener recognitionListener(int generation) {
        return new RecognitionListener() {
            private boolean current() {
                return generation == listenGeneration && !voiceResultSent;
            }

            @Override
            public void onReadyForSpeech(Bundle params) {
                if (!current()) {
                    return;
                }
                recognizerReady = true;
                gazeHandler.removeCallbacks(readyTimeout);
                if (pendingStop) {
                    pendingStop = false;
                    String early = firstNonEmpty(finalTranscript, latestPartial);
                    if (!early.isEmpty()) {
                        deliverRecognized(early);
                        cancelListening();
                    } else {
                        stopListeningWhenReady();
                    }
                }
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
                if (!current()) {
                    return;
                }
                pendingVoiceError = error;
                String heard = firstNonEmpty(finalTranscript, latestPartial);
                if (!heard.isEmpty()) {
                    if (!voiceHolding) {
                        deliverRecognized(heard);
                    }
                    return;
                }
                if (shouldTryNextRecognizer(error)) {
                    gazeHandler.post(() -> tryNextRecognizerOrFail(generation));
                    return;
                }
                if (voiceHolding) {
                    return;
                }
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    showVoiceError(getString(R.string.assistant_voice_mic));
                    return;
                }
                gazeHandler.removeCallbacks(voiceFail);
                gazeHandler.postDelayed(voiceFail, 350);
            }

            @Override
            public void onResults(Bundle results) {
                if (!current()) {
                    return;
                }
                rememberTranscript(results);
                if (voiceHolding) {
                    return;
                }
                finishVoiceAfterRelease();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (!current()) {
                    return;
                }
                rememberTranscript(partialResults);
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        };
    }

    private void tryNextRecognizerOrFail(int generation) {
        if (generation != listenGeneration || voiceResultSent) {
            return;
        }
        if (!firstNonEmpty(finalTranscript, latestPartial).isEmpty()) {
            return;
        }
        if (startNextListenEngine()) {
            pendingVoiceError = -1;
            if (voiceHolding) {
                pushScene("listening");
            }
            return;
        }
        recognizersExhausted = true;
        if (voiceHolding) {
            return;
        }
        finishVoiceAfterRelease();
    }

    private static boolean shouldTryNextRecognizer(int error) {
        return error == SpeechRecognizer.ERROR_CLIENT
                || error == SpeechRecognizer.ERROR_SERVER
                || error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED
                || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                || error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE;
    }

    private static final class ListenEngine {
        final ComponentName component;
        final boolean onDevice;

        ListenEngine(ComponentName component, boolean onDevice) {
            this.component = component;
            this.onDevice = onDevice;
        }
    }

    private void rememberTranscript(Bundle bundle) {
        String text = firstMatch(bundle);
        if (!text.isEmpty()) {
            if (bundle != null && bundle.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)
                    && !voiceHolding) {
                finalTranscript = text;
            }
            latestPartial = text;
            if (finalTranscript.isEmpty()) {
                finalTranscript = text;
            }
        }
    }

    private static String firstMatch(Bundle bundle) {
        if (bundle == null) {
            return "";
        }
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            for (String match : matches) {
                if (match != null && !match.trim().isEmpty()) {
                    return match.trim();
                }
            }
        }
        return "";
    }

    private void finishVoiceAfterRelease() {
        if (voiceResultSent || voiceHolding) {
            return;
        }
        String text = firstNonEmpty(finalTranscript, latestPartial);
        if (!text.isEmpty()) {
            deliverRecognized(text);
            return;
        }
        String reason = voiceFailureReason();
        showVoiceError(reason);
    }

    private void deliverRecognized(String text) {
        if (voiceResultSent || text == null || text.trim().isEmpty()) {
            return;
        }
        voiceResultSent = true;
        pendingStop = false;
        voiceHolding = false;
        gazeHandler.removeCallbacks(voiceFail);
        gazeHandler.removeCallbacks(readyTimeout);
        replyActive = true;
        pushScene("thinking");
        sendMessage(text.trim());
    }

    private void cancelListening() {
        if (speechRecognizer == null) {
            return;
        }
        try {
            speechRecognizer.cancel();
        } catch (RuntimeException ignored) {
            destroyRecognizer();
        }
    }

    private void destroyRecognizer() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (RuntimeException ignored) {
                // already gone
            }
            speechRecognizer = null;
        }
        recognizerReady = false;
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return "";
    }

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

    private String voiceFailureReason() {
        if (pendingVoiceError == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            return getString(R.string.assistant_voice_mic);
        }
        if (recognizersExhausted || shouldTryNextRecognizer(pendingVoiceError)) {
            return getString(R.string.assistant_voice_none);
        }
        return getString(R.string.assistant_voice_fail);
    }

    private void showVoiceError(String reason) {
        voiceHolding = false;
        pendingStop = false;
        replyActive = false;
        gazeHandler.removeCallbacks(pleasedDone);
        gazeHandler.removeCallbacks(readyTimeout);
        binding.tvAssistantStatus.setText(reason);
        binding.btnStatusRetry.setVisibility(View.GONE);
        pushScene("concerned");
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_MIC) {
            return;
        }
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showVoiceError(getString(R.string.assistant_voice_mic));
        } else {
            pushScene("idle");
        }
    }

    @Override
    protected void onDestroy() {
        attitudeGaze.stop();
        gazeHandler.removeCallbacks(voiceFail);
        gazeHandler.removeCallbacks(readyTimeout);
        gazeHandler.removeCallbacks(pleasedDone);
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
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
        attitudeGaze.setPageVisible(this, false);
        gazeHandler.removeCallbacks(gazeHome);
        pushBallActive(false);
        super.onPause();
    }

    /** 出错行右侧的「重试」：再测密钥，或再发一次失败的对话请求。 */
    private void retryFailedRequest() {
        if (retryKeyProbe) {
            probeDeepSeekKey(retryKey, true);
            return;
        }
        assistantHost.retry(() ->
                Toast.makeText(this, R.string.assistant_busy, Toast.LENGTH_SHORT).show());
    }

    private void probeDeepSeekKey(String apiKey, boolean fromStatusLine) {
        if (fromStatusLine) {
            binding.tvAssistantStatus.setText(AssistantStatus.THINKING.label);
            binding.btnStatusRetry.setVisibility(View.GONE);
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
                        binding.tvAssistantStatus.setText("");
                        binding.btnStatusRetry.setVisibility(View.GONE);
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

    private void showErrorLine(String line) {
        binding.tvAssistantStatus.setText(line);
        binding.btnStatusRetry.setVisibility(View.VISIBLE);
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
                                binding.tvAssistantStatus.setText("");
                                binding.btnStatusRetry.setVisibility(View.GONE);
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
                binding.rvMessages.scrollToPosition(messages.size() - 1));
    }
}
