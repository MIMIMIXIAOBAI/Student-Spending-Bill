package com.campusbill.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据库操作工具类：所有数据库读写均在子线程串行执行，
 * 结果回调切回主线程，避免主线程访问数据库。
 */
public class DBHelper {

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(Exception e);
    }

    private static volatile DBHelper instance;

    private final BillDao billDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DBHelper(Context context) {
        this.billDao = AppDatabase.getInstance(context).billDao();
    }

    public static DBHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DBHelper.class) {
                if (instance == null) {
                    instance = new DBHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ---------------- 新增 ----------------

    public void insert(BillEntity bill, DataCallback<Long> callback) {
        execute(() -> billDao.insert(bill), callback);
    }

    public void insertAll(List<BillEntity> bills, DataCallback<Integer> callback) {
        execute(() -> billDao.insertAll(bills).size(), callback);
    }

    // ---------------- 修改 / 删除 ----------------

    public void update(BillEntity bill, DataCallback<Integer> callback) {
        execute(() -> billDao.update(bill), callback);
    }

    public void delete(BillEntity bill, DataCallback<Integer> callback) {
        execute(() -> billDao.delete(bill), callback);
    }

    public void deleteById(long id, DataCallback<Integer> callback) {
        execute(() -> billDao.deleteById(id), callback);
    }

    // ---------------- 查询 ----------------

    public void getById(long id, DataCallback<BillEntity> callback) {
        execute(() -> billDao.getById(id), callback);
    }

    public void getBillsByDate(String date, DataCallback<List<BillEntity>> callback) {
        execute(() -> billDao.getBillsByDate(date), callback);
    }

    public void getBillsByMonth(String month, DataCallback<List<BillEntity>> callback) {
        execute(() -> billDao.getBillsByMonth(month), callback);
    }

    public void sumAmountByCategory(String month, int type, DataCallback<List<CategoryAmount>> callback) {
        execute(() -> billDao.sumAmountByCategory(month, type), callback);
    }

    public void sumAmountByMerchant(String month, int type, DataCallback<List<MerchantAmount>> callback) {
        execute(() -> billDao.sumAmountByMerchant(month, type), callback);
    }

    public void sumAmountByMonthAndType(String month, int type, DataCallback<Double> callback) {
        execute(() -> billDao.sumAmountByMonthAndType(month, type), callback);
    }

    // ---------------- 线程调度 ----------------

    private <T> void execute(Callable<T> task, DataCallback<T> callback) {
        executor.execute(() -> {
            try {
                T result = task.call();
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }
}
