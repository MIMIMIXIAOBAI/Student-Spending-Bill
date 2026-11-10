package com.campusbill.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.chat.ai.assistant.FileProcessLog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

/** 过程页：点开本机 JSONL 里的 Prompt、工具参数和确认记录。 */
public class AssistantProcessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_process);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView body = findViewById(R.id.tvProcess);
        body.setText(render());
    }

    private String render() {
        File[] files = FileProcessLog.directory(this).listFiles(
                (dir, name) -> name.endsWith(".jsonl"));
        if (files == null || files.length == 0) {
            return getString(R.string.assistant_process_empty);
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        StringBuilder text = new StringBuilder();
        for (File file : files) {
            text.append(file.getName()).append("\n\n");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    text.append(formatLine(line)).append("\n\n");
                }
            } catch (Exception e) {
                text.append("无法读取这条记录\n\n");
            }
        }
        return text.toString().trim();
    }

    private static String formatLine(String line) {
        try {
            JSONObject event = new JSONObject(line);
            String type = event.optString("type", "");
            switch (type) {
                case "prompt":
                    return "Prompt\n版本 " + event.optString("version")
                            + "\n摘要 " + event.optString("hash");
                case "tool_call":
                    return "工具\n" + event.optString("name")
                            + "\n参数 " + event.opt("arguments");
                case "tool_result":
                    return "工具结果\n" + event.optString("name")
                            + "\n" + event.optString("result_summary");
                case "confirm_record":
                    return "确认\n动作 " + event.optString("action")
                            + "\n同意 " + event.optBoolean("accepted")
                            + "\n卡片 " + event.opt("preview")
                            + "\n回读 " + event.opt("verified");
                case "status":
                    return "状态\n" + event.optString("value");
                case "think":
                    return "思考\n" + event.optString("text");
                case "decision":
                    return "决策\n" + event.optString("text")
                            + "\n" + event.opt("planned");
                default:
                    return type + "\n" + event;
            }
        } catch (Exception e) {
            return line;
        }
    }
}
