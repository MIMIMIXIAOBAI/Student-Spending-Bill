package com.campusbill.app.imports;

import android.util.Xml;
import com.campusbill.app.data.BillEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.xmlpull.v1.XmlPullParser;

public class BillXlsxParser {
   private static final String SHARED = "sharedStrings";

   public ImportResult parse(InputStream in) throws IOException {
      ImportResult result = new ImportResult();
      Map<String, byte[]> entries = this.readEntries(in);
      byte[] sheet = this.findSheet(entries);
      if (sheet == null) {
         return result;
      } else {
         byte[] sharedBytes = this.findEntry(entries, "sharedStrings.xml");
         List<String> shared = (List<String>)(sharedBytes == null ? new ArrayList() : this.parseSharedStrings(sharedBytes));
         List<Map<Integer, String>> rows = this.parseSheet(sheet, shared);
         int headerRow = this.findHeaderRow(rows);
         if (headerRow < 0) {
            return result;
         } else {
            int[] indexes = this.resolveIndexes((Map)rows.get(headerRow));
            if (indexes[0] >= 0 && indexes[1] >= 0 && indexes[3] >= 0) {
               for(int i = headerRow + 1; i < rows.size(); ++i) {
                  Map<Integer, String> row = (Map)rows.get(i);
                  String typeRaw = cell(row, indexes[1]);
                  if (!isNeutral(typeRaw)) {
                     BillEntity bill = this.parseRow(row, indexes);
                     if (bill == null) {
                        if (!row.isEmpty()) {
                           ++result.failed;
                        }
                     } else {
                        result.bills.add(bill);
                     }
                  }
               }

               return result;
            } else {
               return result;
            }
         }
      }
   }

   private Map<String, byte[]> readEntries(InputStream in) throws IOException {
      Map<String, byte[]> entries = new HashMap();
      ZipInputStream zip = new ZipInputStream(in);

      ZipEntry entry;
      try {
         while((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();
            if (name.endsWith("sharedStrings.xml") || name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
               entries.put(name, this.readEntry(zip));
            }
         }
      } catch (Throwable var7) {
         try {
            zip.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      zip.close();
      return entries;
   }

   private byte[] readEntry(InputStream in) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];

      int count;
      while((count = in.read(buffer)) != -1) {
         out.write(buffer, 0, count);
      }

      return out.toByteArray();
   }

   private byte[] findSheet(Map<String, byte[]> entries) {
      byte[] sheet = this.findEntry(entries, "xl/worksheets/sheet1.xml");
      if (sheet != null) {
         return sheet;
      } else {
         for(Map.Entry<String, byte[]> e : entries.entrySet()) {
            if (((String)e.getKey()).startsWith("xl/worksheets/")) {
               return (byte[])e.getValue();
            }
         }

         return null;
      }
   }

   private byte[] findEntry(Map<String, byte[]> entries, String suffix) {
      for(Map.Entry<String, byte[]> e : entries.entrySet()) {
         if (((String)e.getKey()).endsWith(suffix)) {
            return (byte[])e.getValue();
         }
      }

      return null;
   }

   private List<String> parseSharedStrings(byte[] bytes) throws IOException {
      List<String> list = new ArrayList();
      StringBuilder current = null;
      StringBuilder text = null;

      try {
         XmlPullParser parser = Xml.newPullParser();
         parser.setInput(new ByteArrayInputStream(bytes), (String)null);

         for(int event = parser.getEventType(); event != 1; event = parser.next()) {
            String name = parser.getName();
            if (event == 2) {
               if ("si".equals(name)) {
                  current = new StringBuilder();
               } else if (current != null && "t".equals(name)) {
                  text = new StringBuilder();
               }
            } else if (event == 4) {
               if (text != null) {
                  text.append(parser.getText());
               }
            } else if (event == 3) {
               if ("t".equals(name)) {
                  if (current != null && text != null) {
                     current.append(text);
                  }

                  text = null;
               } else if ("si".equals(name)) {
                  list.add(current == null ? "" : current.toString());
                  current = null;
               }
            }
         }

         return list;
      } catch (Exception e) {
         throw new IOException(e);
      }
   }

   private List<Map<Integer, String>> parseSheet(byte[] bytes, List<String> shared) throws IOException {
      List<Map<Integer, String>> rows = new ArrayList();
      Map<Integer, String> row = null;
      StringBuilder text = null;
      String cellRef = null;
      String cellType = "";
      boolean inValue = false;
      boolean inInline = false;

      try {
         XmlPullParser parser = Xml.newPullParser();
         parser.setInput(new ByteArrayInputStream(bytes), (String)null);

         for(int event = parser.getEventType(); event != 1; event = parser.next()) {
            String name = parser.getName();
            if (event == 2) {
               if ("row".equals(name)) {
                  row = new HashMap();
               } else if (row != null && "c".equals(name)) {
                  cellRef = parser.getAttributeValue((String)null, "r");
                  cellType = parser.getAttributeValue((String)null, "t");
                  text = new StringBuilder();
               } else if ("v".equals(name)) {
                  inValue = true;
               } else if ("t".equals(name)) {
                  inInline = true;
               }
            } else if (event == 4) {
               if (text != null && (inValue || inInline)) {
                  text.append(parser.getText());
               }
            } else if (event == 3) {
               if ("v".equals(name)) {
                  inValue = false;
               } else if ("t".equals(name)) {
                  inInline = false;
               } else if ("c".equals(name)) {
                  if (row != null && cellRef != null) {
                     String value = text == null ? "" : text.toString();
                     if ("s".equals(cellType)) {
                        value = this.sharedString(shared, value);
                     }

                     row.put(columnIndex(cellRef), value);
                  }

                  cellRef = null;
                  cellType = "";
                  text = null;
               } else if ("row".equals(name)) {
                  if (row != null) {
                     rows.add(row);
                  }

                  row = null;
               }
            }
         }

         return rows;
      } catch (Exception e) {
         throw new IOException(e);
      }
   }

   private String sharedString(List<String> shared, String value) {
      try {
         int index = (int)Double.parseDouble(value.trim());
         return index >= 0 && index < shared.size() ? (String)shared.get(index) : "";
      } catch (NumberFormatException var4) {
         return "";
      }
   }

   private static int columnIndex(String ref) {
      if (ref == null) {
         return -1;
      } else {
         int index = 0;

         for(int i = 0; i < ref.length(); ++i) {
            char c = ref.charAt(i);
            if (c >= 'A' && c <= 'Z') {
               index = index * 26 + c - 65 + 1;
            } else {
               if (c < 'a' || c > 'z') {
                  break;
               }

               index = index * 26 + c - 97 + 1;
            }
         }

         return index - 1;
      }
   }

   private int findHeaderRow(List<Map<Integer, String>> rows) {
      for(int i = 0; i < rows.size(); ++i) {
         String line = this.rowText((Map)rows.get(i));
         if (line.contains("金额") && (line.contains("交易时间") || line.contains("日期"))) {
            return i;
         }
      }

      return -1;
   }

   private String rowText(Map<Integer, String> row) {
      StringBuilder sb = new StringBuilder();

      for(String value : row.values()) {
         if (value != null) {
            sb.append(value).append(' ');
         }
      }

      return sb.toString();
   }

   private int[] resolveIndexes(Map<Integer, String> header) {
      int date = this.findColumn(header, "交易时间", "日期");
      int type = this.findColumn(header, "收/支", "收支", "类型");
      int category = this.findColumn(header, "交易类型", "分类");
      int amount = this.findColumn(header, "金额");
      int merchant = this.findColumn(header, "交易对方", "商户");
      int remark = this.findColumn(header, "商品", "备注");
      return new int[]{date, type, category, amount, merchant, remark};
   }

   private int findColumn(Map<Integer, String> header, String... keywords) {
      for(String keyword : keywords) {
         for(Map.Entry<Integer, String> e : header.entrySet()) {
            String value = e.getValue() == null ? "" : ((String)e.getValue()).trim();
            if (value.contains(keyword)) {
               return (Integer)e.getKey();
            }
         }
      }

      return -1;
   }

   private BillEntity parseRow(Map<Integer, String> row, int[] indexes) {
      String[] dateTime = normalizeDateTime(cell(row, indexes[0]));
      if (dateTime == null) {
         return null;
      } else {
         int type = parseType(cell(row, indexes[1]));
         if (type < 0) {
            return null;
         } else {
            double amount = parseAmount(cell(row, indexes[3]));
            if (!Double.isNaN(amount) && !(amount <= (double)0.0F)) {
               BillEntity bill = new BillEntity();
               bill.type = type;
               bill.category = fallback(cell(row, indexes[2]), "其他");
               bill.amount = amount;
               bill.merchant = cleanMerchant(cell(row, indexes[4]));
               bill.remark = cell(row, indexes[5]).trim();
               bill.date = dateTime[0];
               bill.time = dateTime[1];
               bill.createTime = System.currentTimeMillis();
               return bill;
            } else {
               return null;
            }
         }
      }
   }

   private static String cell(Map<Integer, String> row, int index) {
      if (row != null && index >= 0) {
         String value = (String)row.get(index);
         return value == null ? "" : value.replace("\ufeff", "").trim();
      } else {
         return "";
      }
   }

   private static boolean isNeutral(String raw) {
      String text = raw == null ? "" : raw.trim();
      return text.equals("/") || text.contains("不计");
   }

   private static String[] normalizeDateTime(String raw) {
      String text = raw == null ? "" : raw.trim();
      if (text.isEmpty()) {
         return null;
      } else {
         try {
            double serial = Double.parseDouble(text);
            if (serial > (double)20000.0F && serial < (double)80000.0F) {
               long days = (long)Math.floor(serial);
               long seconds = Math.round((serial - (double)days) * (double)86400.0F);
               if (seconds >= 86400L) {
                  ++days;
                  seconds -= 86400L;
               }

               LocalDate date = LocalDate.of(1899, 12, 30).plusDays(days);
               String time = String.format(Locale.CHINA, "%02d:%02d:%02d", seconds / 3600L, seconds % 3600L / 60L, seconds % 60L);
               return new String[]{date.toString(), time};
            }
         } catch (NumberFormatException var10) {
         }

         String normalized = text.replace('/', '-').replace('.', '-').replace("年", "-").replace("月", "-").replace("日", " ").replace('：', ':').trim();
         String[] parts = normalized.split("\\s+", 2);
         LocalDate date = parseDate(parts[0]);
         if (date == null) {
            return null;
         } else {
            String time = parts.length > 1 ? normalizeTime(parts[1]) : "00:00:00";
            return new String[]{date.toString(), time};
         }
      }
   }

   private static LocalDate parseDate(String text) {
      String value;
      for(value = text.trim(); value.contains("--"); value = value.replace("--", "-")) {
      }

      String[] parts = value.split("-");
      if (parts.length != 3) {
         return null;
      } else {
         try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2].trim());
            return LocalDate.of(year, month, day);
         } catch (Exception var6) {
            return null;
         }
      }
   }

   private static String normalizeTime(String raw) {
      String text = raw.trim().replace('：', ':');
      String[] parts = text.split(":");
      if (parts.length < 2) {
         return "00:00:00";
      } else {
         try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            int second = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
            return hour <= 23 && minute <= 59 && second <= 59 ? String.format(Locale.CHINA, "%02d:%02d:%02d", hour, minute, second) : "00:00:00";
         } catch (NumberFormatException var6) {
            return "00:00:00";
         }
      }
   }

   private static int parseType(String raw) {
      String text = raw == null ? "" : raw.trim();
      if (text.contains("收")) {
         return 1;
      } else {
         return text.contains("支") ? 0 : -1;
      }
   }

   private static double parseAmount(String raw) {
      String text = raw.trim().replace("¥", "").replace("￥", "").replace(",", "").replace("元", "").replace(" ", "");

      try {
         return Math.abs(Double.parseDouble(text));
      } catch (NumberFormatException var3) {
         return Double.NaN;
      }
   }

   private static String fallback(String raw, String fallback) {
      String text = raw == null ? "" : raw.trim();
      return text.isEmpty() ? fallback : text;
   }

   private static String cleanMerchant(String raw) {
      String text = raw == null ? "" : raw.trim();
      return text.equals("/") ? "" : text;
   }
}
