package com.campusbill.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.campusbill.app.chat.ChatAdapter;
import com.campusbill.app.chat.ChatMessage;
import com.campusbill.app.chat.ai.AiConfig;
import com.campusbill.app.chat.ai.AiRemoteClient;
import com.campusbill.app.chat.ai.DeepSeekKeyStore;
import com.campusbill.app.chat.ai.assistant.AssistantCallException;
import com.campusbill.app.chat.ai.assistant.AssistantHost;
import com.campusbill.app.chat.ai.assistant.AssistantIntentPlanner;
import com.campusbill.app.chat.ai.BillAnalysisUtils;
import com.campusbill.app.chat.ai.BillContextBuilder;
import com.campusbill.app.chat.ai.IntentMatcher;
import com.campusbill.app.chat.ai.ReplyGenerator;
import com.campusbill.app.data.CategoryAmount;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.data.MerchantAmount;
import com.campusbill.app.databinding.ActivityAiChatBinding;
import com.campusbill.app.ui.BottomTabHelper;
import com.campusbill.app.ui.PressScaleHelper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PressScaleHelper.attach(binding.btnSend);
        BottomTabHelper.bind(this, BottomTabHelper.Tab.AI);

        String fromMain = getIntent().getStringExtra("extra_month");
        if (fromMain != null && !fromMain.trim().isEmpty()) {
            queryMonth = fromMain.trim();
        }

        adapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnApiKey.setOnClickListener(v -> showDeepSeekWizard());
        binding.btnProcess.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, AssistantProcessActivity.class)));
        binding.btnConfirmOk.setOnClickListener(v -> assistantHost.confirm());
        binding.btnConfirmCancel.setOnClickListener(v -> assistantHost.cancel());
        assistantHost = new AssistantHost(this, new AssistantHost.Surface() {
            @Override
            public void onStatus(String statusText, boolean confirmVisible, String emotionId) {
                binding.tvAssistantStatus.setText(statusText);
                boolean showCard = confirmVisible && confirmBody != null;
                binding.confirmCard.setVisibility(showCard ? View.VISIBLE : View.GONE);
                if (!confirmVisible) {
                    confirmBody = null;
                }
            }

            @Override
            public void onTranscript(String text) {
                if (streamIndex < 0 || streamIndex >= messages.size()) {
                    streamIndex = addMessage(ChatMessage.TYPE_AI, text);
                } else {
                    replaceMessage(streamIndex, text);
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
                    binding.confirmCard.setVisibility(View.VISIBLE);
                }
            }
        });
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

        boolean assistantUtterance = !AssistantIntentPlanner.plan(query, LocalDate.now()).isEmpty();
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
        BillContextBuilder.build(this, billContext ->
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
                            tvResult.setText(R.string.deepseek_ok);
                            Toast.makeText(this, R.string.deepseek_ok, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } catch (AssistantCallException e) {
                        runOnUiThread(() -> {
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
