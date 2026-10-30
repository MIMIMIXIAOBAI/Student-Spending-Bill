package com.campusbill.app.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
   entities = {BillEntity.class, ImportBatchEntity.class},
   version = 2,
   exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
   private static final String DB_NAME = "student_bill.db";
   private static volatile AppDatabase instance;
   private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
      public void migrate(@NonNull SupportSQLiteDatabase db) {
         db.execSQL("ALTER TABLE bills ADD COLUMN batch_id INTEGER NOT NULL DEFAULT 0");
         db.execSQL("CREATE TABLE IF NOT EXISTS import_batches (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT, source TEXT, create_time INTEGER NOT NULL)");
      }
   };

   public abstract BillDao billDao();

   public static AppDatabase getInstance(Context context) {
      if (instance == null) {
         synchronized(AppDatabase.class) {
            if (instance == null) {
               instance = (AppDatabase)Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "student_bill.db").addMigrations(new Migration[]{MIGRATION_1_2}).build();
            }
         }
      }

      return instance;
   }
}
