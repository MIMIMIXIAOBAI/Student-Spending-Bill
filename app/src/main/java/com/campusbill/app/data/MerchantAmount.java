package com.campusbill.app.data;

import androidx.room.ColumnInfo;

/**
 * 商户金额统计结果。
 */
public class MerchantAmount {

    @ColumnInfo(name = "merchant")
    public String merchant;

    @ColumnInfo(name = "total")
    public double total;
}
