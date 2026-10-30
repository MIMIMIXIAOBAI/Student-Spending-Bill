package com.campusbill.app.data;

import androidx.room.ColumnInfo;

/**
 * 分类金额统计结果。
 */
public class CategoryAmount {

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "total")
    public double total;
}
