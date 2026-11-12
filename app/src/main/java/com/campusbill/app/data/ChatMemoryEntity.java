package com.campusbill.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_memory")
public class ChatMemoryEntity {
    @PrimaryKey
    public long id;
    public String points;
    public long sourceSessionId;
    public boolean ready;
}
