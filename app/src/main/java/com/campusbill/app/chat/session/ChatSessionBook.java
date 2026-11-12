package com.campusbill.app.chat.session;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 本地会话本。切页、退后台、重进都回到当前 id，不会自己新开。 */
public final class ChatSessionBook {

    public static final int TYPE_AI = 0;
    public static final int TYPE_USER = 1;

    public static final class Message {
        public final int type;
        public final String content;

        public Message(int type, String content) {
            this.type = type;
            this.content = content == null ? "" : content;
        }
    }

    public static final class Session {
        public long id;
        public String title = "";
        public long createdAt;
        public long updatedAt;
        public int scrollPosition;
        public String injectedMemory = "";
        public boolean showMemoryTip;
        public final List<Message> messages = new ArrayList<>();
    }

    private final List<Session> sessions = new ArrayList<>();
    private long currentId;
    private long nextId = 1;
    private String storedMemory = "";
    private boolean memoryReady;
    private long memorySourceId;
    private long clock;

    public long currentId() {
        return currentId;
    }

    public boolean memoryReady() {
        return memoryReady && storedMemory != null && !storedMemory.trim().isEmpty();
    }

    public String storedMemory() {
        return storedMemory == null ? "" : storedMemory;
    }

    public Session current() {
        return find(currentId);
    }

    /** 恢复已保存的当前会话。没有这条 id 时也不新开，改回列表里最近的一条。 */
    public Session restore(long savedId) {
        Session found = find(savedId);
        if (found != null) {
            currentId = found.id;
            return found;
        }
        List<Session> history = historyNewestFirst();
        if (!history.isEmpty()) {
            currentId = history.get(0).id;
            return history.get(0);
        }
        return open(false);
    }

    public Session open(boolean injectMemory) {
        Session session = new Session();
        session.id = nextId++;
        session.createdAt = tick();
        session.updatedAt = session.createdAt;
        if (injectMemory && memoryReady()) {
            session.injectedMemory = storedMemory.trim();
            session.showMemoryTip = true;
        }
        sessions.add(session);
        currentId = session.id;
        return session;
    }

    public void append(int type, String content) {
        Session session = requireCurrent();
        session.messages.add(new Message(type, content));
        session.updatedAt = tick();
        if (type == TYPE_USER && (session.title == null || session.title.trim().isEmpty())) {
            session.title = content == null ? "" : content.trim();
        }
    }

    public void replaceLast(String content) {
        Session session = requireCurrent();
        if (session.messages.isEmpty()) {
            return;
        }
        Message last = session.messages.get(session.messages.size() - 1);
        session.messages.set(session.messages.size() - 1, new Message(last.type, content));
        session.updatedAt = tick();
    }

    public void rename(long id, String title) {
        Session session = require(id);
        if (title == null || title.trim().isEmpty()) {
            session.title = firstUserText(session);
        } else {
            session.title = title.trim();
        }
        session.updatedAt = tick();
    }

    public void delete(long id) {
        Session removed = find(id);
        if (removed == null) {
            return;
        }
        sessions.remove(removed);
        if (currentId != id) {
            return;
        }
        List<Session> history = historyNewestFirst();
        if (history.isEmpty()) {
            open(false);
        } else {
            currentId = history.get(0).id;
        }
    }

    public void select(long id) {
        if (find(id) == null) {
            return;
        }
        currentId = id;
    }

    public void remember(String points, long sourceSessionId) {
        storedMemory = points == null ? "" : points.trim();
        memoryReady = !storedMemory.isEmpty();
        memorySourceId = sourceSessionId;
    }

    public void dismissTip() {
        Session session = current();
        if (session != null) {
            session.showMemoryTip = false;
        }
    }

    public void setScroll(int position) {
        Session session = current();
        if (session != null && position >= 0) {
            session.scrollPosition = position;
        }
    }

    public List<Session> historyNewestFirst() {
        List<Session> copy = new ArrayList<>(sessions);
        copy.sort(Comparator.comparingLong((Session session) -> session.updatedAt).reversed());
        return copy;
    }

    public static String firstUserText(Session session) {
        if (session == null) {
            return "";
        }
        for (Message message : session.messages) {
            if (message.type == TYPE_USER && message.content != null && !message.content.trim().isEmpty()) {
                return message.content.trim();
            }
        }
        return "";
    }

    public Session find(long id) {
        for (Session session : sessions) {
            if (session.id == id) {
                return session;
            }
        }
        return null;
    }

    private Session require(long id) {
        Session session = find(id);
        if (session == null) {
            throw new IllegalArgumentException("missing session " + id);
        }
        return session;
    }

    private Session requireCurrent() {
        Session session = current();
        if (session == null) {
            return open(false);
        }
        return session;
    }

    private long tick() {
        clock += 1;
        return clock;
    }

    public String archive() {
        JSONObject root = new JSONObject();
        try {
            root.put("currentId", currentId);
            root.put("nextId", nextId);
            root.put("memory", storedMemory);
            root.put("memoryReady", memoryReady);
            root.put("memorySourceId", memorySourceId);
            JSONArray list = new JSONArray();
            for (Session session : sessions) {
                JSONObject item = new JSONObject();
                item.put("id", session.id);
                item.put("title", session.title);
                item.put("createdAt", session.createdAt);
                item.put("updatedAt", session.updatedAt);
                item.put("scrollPosition", session.scrollPosition);
                item.put("injectedMemory", session.injectedMemory);
                item.put("showMemoryTip", session.showMemoryTip);
                JSONArray messages = new JSONArray();
                for (Message message : session.messages) {
                    messages.put(new JSONObject()
                            .put("type", message.type)
                            .put("content", message.content));
                }
                item.put("messages", messages);
                list.put(item);
            }
            root.put("sessions", list);
            return root.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    public static ChatSessionBook fromArchive(String raw) {
        ChatSessionBook book = new ChatSessionBook();
        if (raw == null || raw.trim().isEmpty()) {
            return book;
        }
        try {
            JSONObject root = new JSONObject(raw);
            book.nextId = root.optLong("nextId", 1);
            book.storedMemory = root.optString("memory", "");
            book.memoryReady = root.optBoolean("memoryReady", false);
            book.memorySourceId = root.optLong("memorySourceId", 0);
            JSONArray list = root.optJSONArray("sessions");
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    Session session = new Session();
                    session.id = item.optLong("id");
                    session.title = item.optString("title", "");
                    session.createdAt = item.optLong("createdAt");
                    session.updatedAt = item.optLong("updatedAt");
                    session.scrollPosition = item.optInt("scrollPosition");
                    session.injectedMemory = item.optString("injectedMemory", "");
                    session.showMemoryTip = item.optBoolean("showMemoryTip", false);
                    JSONArray messages = item.optJSONArray("messages");
                    if (messages != null) {
                        for (int m = 0; m < messages.length(); m++) {
                            JSONObject message = messages.getJSONObject(m);
                            session.messages.add(new Message(
                                    message.optInt("type"), message.optString("content", "")));
                        }
                    }
                    book.sessions.add(session);
                }
            }
            book.currentId = root.optLong("currentId", 0);
            return book;
        } catch (Exception e) {
            return new ChatSessionBook();
        }
    }
}
