package com.campusbill.app.chat.ai.assistant;

import android.content.Context;

import com.campusbill.app.data.AppDatabase;
import com.campusbill.app.data.BillDao;
import com.campusbill.app.data.BillEntity;

import java.util.ArrayList;
import java.util.List;

/** 直接使用现有 BillDao / BillEntity，不复制账本。调用方需在后台线程执行。 */
public final class RoomBillLedger implements BillLedger {

    private final BillDao dao;

    public RoomBillLedger(Context context) {
        dao = AppDatabase.getInstance(context).billDao();
    }

    @Override
    public List<LedgerBill> all() {
        List<LedgerBill> bills = new ArrayList<>();
        // getAllBills 没有 batch_id 条件，历史导入和手动记下的行都在这本账里。
        for (BillEntity entity : dao.getAllBills()) {
            bills.add(from(entity));
        }
        return bills;
    }

    @Override
    public LedgerBill getById(long id) {
        return from(dao.getById(id));
    }

    @Override
    public LedgerBill insertAndRead(LedgerBill draft) {
        BillEntity entity = new BillEntity();
        entity.type = draft.type;
        entity.amount = draft.amount;
        entity.category = draft.category == null ? "" : draft.category;
        entity.merchant = draft.merchant == null ? "" : draft.merchant;
        entity.remark = draft.remark == null ? "" : draft.remark;
        entity.date = draft.date;
        entity.time = draft.time == null || draft.time.isEmpty() ? "12:00:00" : draft.time;
        entity.createTime = System.currentTimeMillis();
        entity.batchId = 0;
        long id = dao.insert(entity);
        return from(dao.getById(id));
    }

    private static LedgerBill from(BillEntity entity) {
        if (entity == null) {
            return null;
        }
        LedgerBill bill = new LedgerBill();
        bill.id = entity.id;
        bill.type = entity.type;
        bill.amount = entity.amount;
        bill.category = entity.category == null ? "" : entity.category;
        bill.merchant = entity.merchant == null ? "" : entity.merchant;
        bill.remark = entity.remark == null ? "" : entity.remark;
        bill.date = entity.date == null ? "" : entity.date;
        bill.time = entity.time == null ? "" : entity.time;
        return bill;
    }
}
