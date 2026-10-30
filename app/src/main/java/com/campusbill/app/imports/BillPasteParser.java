package com.campusbill.app.imports;

import com.campusbill.app.data.BillEntity;

public class BillPasteParser {
   public ImportResult parse(String text) {
      ImportResult result = new ImportResult();
      if (text == null) {
         return result;
      } else {
         int[] indexes = BillCsvParser.defaultIndexes();
         String[] lines = text.split("\r?\n");

         for(String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && (!trimmed.contains("金额") || !trimmed.contains("日期") && !trimmed.contains("交易时间"))) {
               String[] cells = trimmed.split("[,，\t]");
               if (cells.length < 5) {
                  cells = trimmed.split("\\s+");
               }

               BillEntity bill = BillCsvParser.parseFields(cells, indexes);
               if (bill == null) {
                  ++result.failed;
               } else {
                  result.bills.add(bill);
               }
            }
         }

         return result;
      }
   }
}
