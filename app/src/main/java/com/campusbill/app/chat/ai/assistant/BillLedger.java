package com.campusbill.app.chat.ai.assistant;

import java.util.List;

/** 读现有账本、确认后插入并回读。实现方负责落到 Room。 */
public interface BillLedger {
    List<LedgerBill> all();

    LedgerBill getById(long id);

    LedgerBill insertAndRead(LedgerBill draft);
}
