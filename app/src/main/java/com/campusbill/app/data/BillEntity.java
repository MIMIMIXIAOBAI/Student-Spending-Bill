package com.campusbill.app.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 账单实体：一条收入或支出记录，仅存本机数据库。
 */
@Entity(tableName = "bills")
public class BillEntity {

    /** 支出 */
    public static final int TYPE_EXPENSE = 0;
    /** 收入 */
    public static final int TYPE_INCOME = 1;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    /** 类型：TYPE_EXPENSE / TYPE_INCOME */
    @ColumnInfo(name = "type")
    public int type;

    /** 分类名称，如「餐饮」「生活费」 */
    @ColumnInfo(name = "category")
    public String category;

    /** 金额，单位：元 */
    @ColumnInfo(name = "amount")
    public double amount;

    /** 商户名称 */
    @ColumnInfo(name = "merchant")
    public String merchant;

    /** 备注 */
    @ColumnInfo(name = "remark")
    public String remark;

    /** 日期，格式 yyyy-MM-dd */
    @ColumnInfo(name = "date")
    public String date;

    /** 时间，格式 HH:mm:ss */
    @ColumnInfo(name = "time")
    public String time;

    /** 创建时间戳（毫秒） */
    @ColumnInfo(name = "create_time")
    public long createTime;

    public boolean isIncome() {
        return type == TYPE_INCOME;
    }
}
