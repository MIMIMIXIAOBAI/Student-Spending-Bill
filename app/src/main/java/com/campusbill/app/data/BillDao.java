package com.campusbill.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface BillDao {
   @Insert(
      onConflict = 1
   )
   long insert(BillEntity bill);

   @Insert(
      onConflict = 1
   )
   List<Long> insertAll(List<BillEntity> bills);

   @Update
   int update(BillEntity bill);

   @Delete
   int delete(BillEntity bill);

   @Query("DELETE FROM bills WHERE id = :id")
   int deleteById(long id);

   @Query("SELECT * FROM bills WHERE id = :id")
   BillEntity getById(long id);

   @Query("SELECT * FROM bills WHERE date = :date ORDER BY time DESC, id DESC")
   List<BillEntity> getBillsByDate(String date);

   @Query("SELECT * FROM bills WHERE substr(date, 1, 7) = :month ORDER BY date ASC, time ASC, id ASC")
   List<BillEntity> getBillsByMonth(String month);

   @Query("SELECT category, SUM(amount) AS total FROM bills WHERE substr(date, 1, 7) = :month AND type = :type GROUP BY category ORDER BY total DESC")
   List<CategoryAmount> sumAmountByCategory(String month, int type);

   @Query("SELECT merchant, SUM(amount) AS total FROM bills WHERE substr(date, 1, 7) = :month AND type = :type GROUP BY merchant ORDER BY total DESC")
   List<MerchantAmount> sumAmountByMerchant(String month, int type);

   @Query("SELECT IFNULL(SUM(amount), 0) FROM bills WHERE substr(date, 1, 7) = :month AND type = :type")
   double sumAmountByMonthAndType(String month, int type);

   @Query("SELECT COUNT(*) FROM bills")
   int countAll();

   @Query("SELECT DISTINCT substr(date, 1, 7) AS month FROM bills ORDER BY month DESC")
   List<String> getBillMonths();

   @Insert
   long insertBatch(ImportBatchEntity batch);

   @Query("SELECT * FROM import_batches ORDER BY create_time DESC, id DESC")
   List<ImportBatchEntity> getBatches();

   @Query("DELETE FROM import_batches WHERE id = :batchId")
   int deleteBatch(long batchId);

   @Query("DELETE FROM import_batches")
   int deleteAllBatches();

   @Query("SELECT * FROM bills ORDER BY date DESC, time DESC, id DESC")
   List<BillEntity> getAllBills();

   @Query("DELETE FROM bills WHERE batch_id = :batchId")
   int deleteByBatch(long batchId);

   @Query("DELETE FROM bills")
   int deleteAllBills();
}
