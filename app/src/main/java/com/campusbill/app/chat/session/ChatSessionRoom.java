package com.campusbill.app.chat.session;

import android.content.Context;

import com.campusbill.app.data.AppDatabase;
import com.campusbill.app.data.ChatMemoryEntity;
import com.campusbill.app.data.ChatMessageEntity;
import com.campusbill.app.data.ChatSessionDao;
import com.campusbill.app.data.ChatSessionEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 把会话本写进本机 Room，当前 id 另外记在偏好里。密钥不进这些表。 */
public final class ChatSessionRoom {

    private static final String PREF = "chat_session_pointer";
    private static final String KEY = "current_id";

    private ChatSessionRoom() {
    }

    public static long savedId(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getLong(KEY, 0L);
    }

    public static ChatSessionBook load(Context context) {
        ChatSessionDao dao = AppDatabase.getInstance(context).chatSessionDao();
        try {
            JSONObject root = new JSONObject();
            root.put("currentId", savedId(context));
            JSONArray sessions = new JSONArray();
            long nextId = 1;
            for (ChatSessionEntity row : dao.sessions()) {
                JSONObject item = new JSONObject();
                item.put("id", row.id);
                item.put("title", row.title == null ? "" : row.title);
                item.put("createdAt", row.createdAt);
                item.put("updatedAt", row.updatedAt);
                item.put("scrollPosition", row.scrollPosition);
                item.put("injectedMemory", row.injectedMemory == null ? "" : row.injectedMemory);
                item.put("showMemoryTip", row.showMemoryTip);
                JSONArray messages = new JSONArray();
                item.put("messages", messages);
                sessions.put(item);
                if (row.id >= nextId) {
                    nextId = row.id + 1;
                }
            }
            for (ChatMessageEntity row : dao.messages()) {
                for (int i = 0; i < sessions.length(); i++) {
                    JSONObject item = sessions.getJSONObject(i);
                    if (item.getLong("id") != row.sessionId) {
                        continue;
                    }
                    item.getJSONArray("messages").put(new JSONObject()
                            .put("type", row.type)
                            .put("content", row.content == null ? "" : row.content));
                }
            }
            ChatMemoryEntity memory = dao.memory();
            root.put("sessions", sessions);
            root.put("nextId", nextId);
            root.put("memory", memory == null || memory.points == null ? "" : memory.points);
            root.put("memoryReady", memory != null && memory.ready);
            root.put("memorySourceId", memory == null ? 0 : memory.sourceSessionId);
            return ChatSessionBook.fromArchive(root.toString());
        } catch (Exception e) {
            return new ChatSessionBook();
        }
    }

    public static void save(Context context, ChatSessionBook book) {
        if (book == null) {
            return;
        }
        context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY, book.currentId())
                .apply();
        AppDatabase database = AppDatabase.getInstance(context);
        database.runInTransaction(() -> write(database.chatSessionDao(), book));
    }

    private static void write(ChatSessionDao dao, ChatSessionBook book) {
        try {
            JSONObject root = new JSONObject(book.archive());
            dao.clearMessages();
            dao.clearSessions();
            List<ChatSessionEntity> sessions = new ArrayList<>();
            List<ChatMessageEntity> messages = new ArrayList<>();
            JSONArray list = root.optJSONArray("sessions");
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    ChatSessionEntity row = new ChatSessionEntity();
                    row.id = item.getLong("id");
                    row.title = item.optString("title", "");
                    row.createdAt = item.optLong("createdAt");
                    row.updatedAt = item.optLong("updatedAt");
                    row.scrollPosition = item.optInt("scrollPosition");
                    row.injectedMemory = item.optString("injectedMemory", "");
                    row.showMemoryTip = item.optBoolean("showMemoryTip");
                    sessions.add(row);
                    JSONArray stored = item.optJSONArray("messages");
                    if (stored == null) {
                        continue;
                    }
                    for (int m = 0; m < stored.length(); m++) {
                        JSONObject message = stored.getJSONObject(m);
                        ChatMessageEntity line = new ChatMessageEntity();
                        line.sessionId = row.id;
                        line.type = message.optInt("type");
                        line.content = message.optString("content", "");
                        line.position = m;
                        messages.add(line);
                    }
                }
            }
            if (!sessions.isEmpty()) {
                dao.insertSessions(sessions);
            }
            if (!messages.isEmpty()) {
                dao.insertMessages(messages);
            }
            ChatMemoryEntity memory = new ChatMemoryEntity();
            memory.id = 1;
            memory.points = root.optString("memory", "");
            memory.ready = root.optBoolean("memoryReady");
            memory.sourceSessionId = root.optLong("memorySourceId");
            dao.saveMemory(memory);
        } catch (Exception ignored) {
            // 写失败时保留上一份表，不把密钥写进来
        }
    }
}
