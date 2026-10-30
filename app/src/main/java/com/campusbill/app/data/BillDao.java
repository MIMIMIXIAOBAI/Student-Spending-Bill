package com.campusbill.app.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 账单数据访问接口，全部为本地数据库操作。
 */
@Dao
public interface BillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BillEntity bill);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<BillEntity> bills);

    @Update
    int update(BillEntity bill);

    @Delete
    int delete(BillEntity bill);

    @Query("DELETE FROM bills WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT * FROM bills WHERE id = :id")
    BillEntity getById(long id);

    /** 按日期查询（yyyy-MM-dd） */
    @Query("SELECT * FROM bills WHERE date = :date ORDER BY time DESC, id DESC")
    List<BillEntity> getBillsByDate(String date);

    /** 按月份查询（yyyy-MM） */
    @Query("SELECT * FROM bills WHERE substr(date, 1, 7) = :month ORDER BY date ASC, time ASC, id ASC")
    List<BillEntity> getBillsByMonth(String month);

    /** 按分类统计金额（阶段：月份 + 类型） */
    @Query("SELECT category, SUM(amount) AS total FROM bills "
            + "WHERE substr(date, 1, 7) = :month AND type = :type "
            + "GROUP BY category ORDER BY total DESC")
    List<CategoryAmount> sumAmountByCategory(String month, int type);

    /** 按商户统计金额（阶段：月份 + 类型） */
    @Query("SELECT merchant, SUM(amount) AS total FROM bills "
            + "WHERE substr(date, 1, 7) = :month AND type = :type "
            + "GROUP BY merchant ORDER BY total DESC")
    List<MerchantAmount> sumAmountByMerchant(String month, int type);

    /** 某月某类型的金额合计 */
    @Query("SELECT IFNULL(SUM(amount), 0) FROM bills "
            + "WHERE substr(date, 1, 7) = :month AND type = :type")
    double sumAmountByMonthAndType(String month, int type);
}
