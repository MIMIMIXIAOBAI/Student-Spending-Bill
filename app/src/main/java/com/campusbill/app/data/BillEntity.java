package com.campusbill.app.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
   tableName = "bills"
)
public class BillEntity {
   public static final int TYPE_EXPENSE = 0;
   public static final int TYPE_INCOME = 1;
   @PrimaryKey(
      autoGenerate = true
   )
   @ColumnInfo(
      name = "id"
   )
   public long id;
   @ColumnInfo(
      name = "type"
   )
   public int type;
   @ColumnInfo(
      name = "category"
   )
   public String category;
   @ColumnInfo(
      name = "amount"
   )
   public double amount;
   @ColumnInfo(
      name = "merchant"
   )
   public String merchant;
   @ColumnInfo(
      name = "remark"
   )
   public String remark;
   @ColumnInfo(
      name = "date"
   )
   public String date;
   @ColumnInfo(
      name = "time"
   )
   public String time;
   @ColumnInfo(
      name = "create_time"
   )
   public long createTime;
   @ColumnInfo(
      name = "batch_id",
      defaultValue = "0"
   )
   public long batchId;

   public boolean isIncome() {
      return this.type == 1;
   }
}
