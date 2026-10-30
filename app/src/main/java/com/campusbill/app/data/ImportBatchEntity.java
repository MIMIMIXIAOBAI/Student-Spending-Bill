package com.campusbill.app.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
   tableName = "import_batches"
)
public class ImportBatchEntity {
   @PrimaryKey(
      autoGenerate = true
   )
   @ColumnInfo(
      name = "id"
   )
   public long id;
   @ColumnInfo(
      name = "name"
   )
   public String name;
   @ColumnInfo(
      name = "source"
   )
   public String source;
   @ColumnInfo(
      name = "create_time"
   )
   public long createTime;
}
