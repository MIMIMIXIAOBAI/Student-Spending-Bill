package com.campusbill.app.imports;

import com.campusbill.app.data.BillEntity;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class BillCsvParser {
   public ImportResult parse(InputStream in) throws IOException {
      ImportResult result = new ImportResult();
      byte[] bytes = readAll(in);
      if (bytes.length == 0) {
         return result;
      } else {
         try {
            CSVReader reader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(bytes), detectCharset(bytes)));

            try {
               List<String[]> rows = reader.readAll();
               int headerRow = findHeaderRow(rows);
               int start = headerRow >= 0 ? headerRow + 1 : 0;
               int[] indexes = headerRow >= 0 ? resolveIndexes((String[])rows.get(headerRow)) : defaultIndexes();

               for(int i = start; i < rows.size(); ++i) {
                  String[] row = (String[])rows.get(i);
                  if (!isEmptyRow(row)) {
                     BillEntity bill = parseFields(row, indexes);
                     if (bill == null) {
                        ++result.failed;
                     } else {
                        result.bills.add(bill);
                     }
                  }
               }
            } catch (Throwable var13) {
               try {
                  reader.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }

               throw var13;
            }

            reader.close();
            return result;
         } catch (CsvException e) {
            throw new IOException(e);
         }
      }
   }

   public static int[] defaultIndexes() {
      return new int[]{0, 1, 2, 3, 4, 5, 6};
   }

   private static byte[] readAll(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];

      int count;
      while((count = in.read(buffer)) != -1) {
         out.write(buffer, 0, count);
      }

      return out.toByteArray();
   }

   private static Charset detectCharset(byte[] bytes) {
      try {
         StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
         return StandardCharsets.UTF_8;
      } catch (CharacterCodingException var2) {
         return Charset.forName("GBK");
      }
   }

   private static int findHeaderRow(List<String[]> rows) {
      for(int i = 0; i < rows.size(); ++i) {
         String[] row = (String[])rows.get(i);
         String line = join(row);
         if (line.contains("金额") && (line.contains("日期") || line.contains("交易时间"))) {
            return i;
         }
      }

      return -1;
   }

   private static String join(String[] row) {
      StringBuilder sb = new StringBuilder();

      for(String cell : row) {
         if (cell != null) {
            sb.append(cell).append(' ');
         }
      }

      return sb.toString();
   }

   private static int[] resolveIndexes(String[] header) {
      int dateIndex = findColumn(header, "交易时间", "日期");
      int timeIndex = findColumn(header, "时间");
      if (timeIndex >= 0 && timeIndex == dateIndex) {
         timeIndex = -1;
      }

      int typeIndex = findColumn(header, "收/支", "类型", "收支");
      int categoryIndex = findColumn(header, "分类");
      int amountIndex = findColumn(header, "金额");
      int merchantIndex = findColumn(header, "交易对方", "商户");
      int remarkIndex = findColumn(header, "商品说明", "备注");
      return new int[]{dateIndex, timeIndex, typeIndex, categoryIndex, amountIndex, merchantIndex, remarkIndex};
   }

   private static int findColumn(String[] header, String... keywords) {
      for(String keyword : keywords) {
         for(int col = 0; col < header.length; ++col) {
            String cell = header[col] == null ? "" : header[col].trim();
            if (cell.contains(keyword)) {
               return col;
            }
         }
      }

      return -1;
   }

   private static boolean isEmptyRow(String[] row) {
      if (row == null) {
         return true;
      } else {
         for(String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   public static BillEntity parseFields(String[] row, int[] indexes) {
      if (row != null && indexes != null && indexes.length >= 7) {
         String rawDate = cell(row, indexes[0]).trim();
         String rawTime = cell(row, indexes[1]).trim();
         if (rawDate.contains(" ") && rawTime.isEmpty()) {
            String[] parts = rawDate.split("\\s+", 2);
            rawDate = parts[0];
            rawTime = parts[1];
         }

         LocalDate date = parseDate(normalizeDate(rawDate));
         if (date == null) {
            return null;
         } else {
            int type = parseType(cell(row, indexes[2]));
            if (type < 0) {
               return null;
            } else {
               double amount = parseAmount(cell(row, indexes[4]));
               if (!Double.isNaN(amount) && !(amount <= (double)0.0F)) {
                  BillEntity bill = new BillEntity();
                  bill.type = type;
                  bill.category = fallbackCategory(cell(row, indexes[3]));
                  bill.amount = amount;
                  bill.merchant = cell(row, indexes[5]).trim();
                  bill.remark = cell(row, indexes[6]).trim();
                  bill.date = date.toString();
                  bill.time = normalizeTime(rawTime);
                  bill.createTime = System.currentTimeMillis();
                  return bill;
               } else {
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   private static String cell(String[] row, int index) {
      return row != null && index >= 0 && index < row.length && row[index] != null ? row[index].replace("\ufeff", "") : "";
   }

   private static String normalizeDate(String raw) {
      String text;
      for(text = raw.trim().replace('/', '-').replace('.', '-').replace("年", "-").replace("月", "-").replace("日", ""); text.contains("--"); text = text.replace("--", "-")) {
      }

      return text;
   }

   private static LocalDate parseDate(String text) {
      try {
         return LocalDate.parse(text);
      } catch (Exception var2) {
         return null;
      }
   }

   private static String normalizeTime(String raw) {
      String text = raw.trim().replace('：', ':');
      if (text.isEmpty()) {
         return "00:00:00";
      } else if (text.length() == 5) {
         return text + ":00";
      } else {
         return text.length() == 8 ? text : "00:00:00";
      }
   }

   private static int parseType(String raw) {
      String text = raw.trim();
      if (text.contains("收")) {
         return 1;
      } else {
         return text.contains("支") ? 0 : -1;
      }
   }

   private static double parseAmount(String raw) {
      String text = raw.trim().replace("¥", "").replace("￥", "").replace(",", "").replace("元", "").replace(" ", "");

      try {
         return Double.parseDouble(text);
      } catch (NumberFormatException var3) {
         return Double.NaN;
      }
   }

   private static String fallbackCategory(String raw) {
      String text = raw.trim();
      return text.isEmpty() ? "其他" : text;
   }
}
