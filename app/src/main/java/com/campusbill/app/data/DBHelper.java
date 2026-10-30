package com.campusbill.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DBHelper {
   private static volatile DBHelper instance;
   private final AppDatabase database;
   private final BillDao billDao;
   private final Context appContext;
   private final ExecutorService executor = Executors.newSingleThreadExecutor();
   private final Handler mainHandler = new Handler(Looper.getMainLooper());

   private DBHelper(Context context) {
      this.database = AppDatabase.getInstance(context);
      this.billDao = this.database.billDao();
      this.appContext = context;
      this.executor.execute(this::refineCategoriesOnce);
   }

   public static DBHelper getInstance(Context context) {
      if (instance == null) {
         synchronized(DBHelper.class) {
            if (instance == null) {
               instance = new DBHelper(context.getApplicationContext());
            }
         }
      }

      return instance;
   }

   public void insert(BillEntity bill, DataCallback<Long> callback) {
      this.execute(() -> this.billDao.insert(bill), callback);
   }

   public void insertAll(List<BillEntity> bills, DataCallback<Integer> callback) {
      this.execute(() -> this.billDao.insertAll(bills).size(), callback);
   }

   public void insertBatch(String name, String source, List<BillEntity> bills, DataCallback<Integer> callback) {
      this.execute(() -> (Integer)this.database.runInTransaction(() -> {
            ImportBatchEntity batch = new ImportBatchEntity();
            batch.name = name;
            batch.source = source;
            batch.createTime = System.currentTimeMillis();
            long batchId = this.billDao.insertBatch(batch);

            for(BillEntity bill : bills) {
               bill.batchId = batchId;
            }

            return this.billDao.insertAll(bills).size();
         }), callback);
   }

   public void update(BillEntity bill, DataCallback<Integer> callback) {
      this.execute(() -> this.billDao.update(bill), callback);
   }

   public void delete(BillEntity bill, DataCallback<Integer> callback) {
      this.execute(() -> this.billDao.delete(bill), callback);
   }

   public void deleteById(long id, DataCallback<Integer> callback) {
      this.execute(() -> this.billDao.deleteById(id), callback);
   }

   public void getById(long id, DataCallback<BillEntity> callback) {
      this.execute(() -> this.billDao.getById(id), callback);
   }

   public void getBillsByDate(String date, DataCallback<List<BillEntity>> callback) {
      this.execute(() -> this.billDao.getBillsByDate(date), callback);
   }

   public void getBillsByMonth(String month, DataCallback<List<BillEntity>> callback) {
      this.execute(() -> this.billDao.getBillsByMonth(month), callback);
   }

   public void sumAmountByCategory(String month, int type, DataCallback<List<CategoryAmount>> callback) {
      this.execute(() -> this.billDao.sumAmountByCategory(month, type), callback);
   }

   public void sumAmountByMerchant(String month, int type, DataCallback<List<MerchantAmount>> callback) {
      this.execute(() -> this.billDao.sumAmountByMerchant(month, type), callback);
   }

   public void sumAmountByMonthAndType(String month, int type, DataCallback<Double> callback) {
      this.execute(() -> this.billDao.sumAmountByMonthAndType(month, type), callback);
   }

   public void countAll(DataCallback<Integer> callback) {
      this.execute(() -> this.billDao.countAll(), callback);
   }

   public void getBillMonths(DataCallback<List<String>> callback) {
      this.execute(() -> this.billDao.getBillMonths(), callback);
   }

   public void getBatches(DataCallback<List<ImportBatchEntity>> callback) {
      this.execute(() -> this.billDao.getBatches(), callback);
   }

   public void getAllBills(DataCallback<List<BillEntity>> callback) {
      this.execute(() -> this.billDao.getAllBills(), callback);
   }

   public void deleteBatchWithBills(long batchId, DataCallback<Integer> callback) {
      this.execute(() -> (Integer)this.database.runInTransaction(() -> {
            int rows = this.billDao.deleteByBatch(batchId);
            this.billDao.deleteBatch(batchId);
            return rows;
         }), callback);
   }

   public void clearHistory(DataCallback<Integer> callback) {
      this.execute(() -> (Integer)this.database.runInTransaction(() -> {
            int rows = this.billDao.deleteAllBills();
            this.billDao.deleteAllBatches();
            return rows;
         }), callback);
   }

   /** 存量账单一次性分类细化：仅在首次执行，之后由分类规则在导入时生效。 */
   private void refineCategoriesOnce() {
      SharedPreferences prefs = this.appContext.getSharedPreferences("category_refine", 0);
      if (prefs.getBoolean("done_v2", false)) {
         return;
      }

      try {
         this.database.runInTransaction(() -> {
            List<BillEntity> bills = this.billDao.getAllBills();

            for(BillEntity bill : bills) {
               String refined = CategoryClassifier.classify(bill.type, bill.category, bill.merchant, bill.remark);
               if (!refined.equals(bill.category)) {
                  bill.category = refined;
                  this.billDao.update(bill);
               }
            }

            return null;
         });
         prefs.edit().putBoolean("done_v2", true).apply();
      } catch (Exception e) {
         // 失败时不写标志，下次启动重试
      }
   }

   private <T> void execute(Callable<T> task, DataCallback<T> callback) {
      this.executor.execute(() -> {
         try {
            T result = (T)task.call();
            if (callback != null) {
               this.mainHandler.post(() -> callback.onSuccess(result));
            }
         } catch (Exception e) {
            if (callback != null) {
               this.mainHandler.post(() -> callback.onError(e));
            }
         }

      });
   }

   public interface DataCallback<T> {
      void onSuccess(T data);

      void onError(Exception e);
   }
}
