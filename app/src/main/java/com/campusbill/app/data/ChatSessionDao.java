package com.campusbill.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions")
    List<ChatSessionEntity> sessions();

    @Query("SELECT * FROM chat_messages ORDER BY sessionId ASC, position ASC, id ASC")
    List<ChatMessageEntity> messages();

    @Query("DELETE FROM chat_sessions")
    void clearSessions();

    @Query("DELETE FROM chat_messages")
    void clearMessages();

    @Insert
    void insertSessions(List<ChatSessionEntity> rows);

    @Insert
    void insertMessages(List<ChatMessageEntity> rows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveMemory(ChatMemoryEntity memory);

    @Query("SELECT * FROM chat_memory WHERE id = 1")
    ChatMemoryEntity memory();
}
