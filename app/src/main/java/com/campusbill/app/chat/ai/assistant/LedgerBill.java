package com.campusbill.app.chat.ai.assistant;

/** 助手看见的账单行，字段与 BillEntity 对齐，本身不是第二套账本。 */
public final class LedgerBill {
    public long id;
    public int type;
    public double amount;
    public String category = "";
    public String merchant = "";
    public String remark = "";
    public String date = "";
    public String time = "";

    public LedgerBill copy() {
        LedgerBill copy = new LedgerBill();
        copy.id = id;
        copy.type = type;
        copy.amount = amount;
        copy.category = category;
        copy.merchant = merchant;
        copy.remark = remark;
        copy.date = date;
        copy.time = time;
        return copy;
    }
}
