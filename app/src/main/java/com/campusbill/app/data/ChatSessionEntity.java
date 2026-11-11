package com.campusbill.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_sessions")
public class ChatSessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public long createdAt;
    public long updatedAt;
    public int scrollPosition;
    public String injectedMemory;
    public boolean showMemoryTip;
}
