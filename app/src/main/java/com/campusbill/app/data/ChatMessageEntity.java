package com.campusbill.app.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages", indices = {@Index("sessionId")})
public class ChatMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public int type;
    public String content;
    public int position;
}
