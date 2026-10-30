package com.campusbill.app.imports;

import com.campusbill.app.data.BillEntity;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BillPdfParser {
   private static final Pattern DATE_PATTERN = Pattern.compile("20\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2}");
   private static final Pattern TIME_PATTERN = Pattern.compile("[0-2]?\\d[:∶：][0-5]\\d(?:[:∶：][0-5]\\d)?");
   private static final int GROUP_NONE = 0;
   private static final int GROUP_MAIN = 1;
   private static final int GROUP_STATUS = 2;
   private static final int GROUP_AMOUNT = 3;
   private static final Pattern MONEY_PATTERN = Pattern.compile("[¥￥]\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)");
   private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)");

   public ImportResult parse(InputStream in) throws IOException {
      ImportResult result = new ImportResult();
      PDDocument document = PDDocument.load(in);

      try {
         List<MainRow> mainRows = new ArrayList();
         List<StatusRow> statusRows = new ArrayList();
         List<String> amounts = new ArrayList();
         int group = 0;
         float[] bounds = null;

         for(int page = 1; page <= document.getNumberOfPages(); ++page) {
            float pageWidth = document.getPage(page - 1).getCropBox().getWidth();
            List<List<Run>> rows = this.buildRows(this.collectRuns(document, page));
            int headerIndex = -1;
            int detected = 0;
            float[] detectedBounds = null;

            for(int i = 0; i < rows.size(); ++i) {
               String text = this.rowText((List)rows.get(i));
               if (text.contains("交易时间") && text.contains("交易对方")) {
                  detected = 1;
                  headerIndex = i;
                  detectedBounds = new float[]{this.findX((List)rows.get(i), "交易类型"), this.findX((List)rows.get(i), "交易对方")};
               } else if (text.contains("商品") && text.contains("收/支")) {
                  detected = 2;
                  headerIndex = i;
                  detectedBounds = new float[]{this.findX((List)rows.get(i), "收/支")};
               } else if (text.contains("金额(元)") || text.contains("金额（元）")) {
                  detected = 3;
                  headerIndex = i;
                  detectedBounds = new float[]{this.findX((List)rows.get(i), "支付方式")};
               }
            }

            if (detected != 0) {
               group = detected;
               bounds = this.fixBounds(detected, detectedBounds, pageWidth);
            }

            int start = headerIndex >= 0 ? headerIndex + 1 : 0;

            for(int i = start; i < rows.size(); ++i) {
               if (i != headerIndex) {
                  List<Run> row = (List)rows.get(i);
                  if (group == 1) {
                     MainRow main = this.parseMain(row, bounds);
                     if (main != null) {
                        mainRows.add(main);
                     }
                  } else if (group == 2) {
                     StatusRow status = this.parseStatus(row, bounds);
                     if (status != null) {
                        statusRows.add(status);
                     }
                  } else if (group == 3) {
                     String amount = this.cell(row, -1.0F, bounds == null ? -1.0F : bounds[0]);
                     if (amount.contains("¥") || amount.contains("￥")) {
                        amounts.add(amount);
                     }
                  }
               }
            }
         }

         int count = mainRows.size();
         if (count != 0 && statusRows.size() == count && amounts.size() == count) {
            for(int i = 0; i < count; ++i) {
               MainRow main = (MainRow)mainRows.get(i);
               StatusRow status = (StatusRow)statusRows.get(i);
               String typeRaw = status.type;
               if (!isNeutral(typeRaw)) {
                  int type = parseType(typeRaw);
                  if (type < 0) {
                     ++result.failed;
                  } else {
                     double amount = parseAmount((String)amounts.get(i));
                     if (!Double.isNaN(amount) && !(amount <= (double)0.0F)) {
                        BillEntity bill = new BillEntity();
                        bill.type = type;
                        bill.category = main.category.isEmpty() ? "其他" : main.category;
                        bill.amount = amount;
                        bill.merchant = main.merchant;
                        bill.remark = status.remark;
                        bill.date = main.date;
                        bill.time = main.time;
                        bill.createTime = System.currentTimeMillis();
                        result.bills.add(bill);
                     } else {
                        ++result.failed;
                     }
                  }
               }
            }

            return result;
         }

         return result;
      } finally {
         document.close();
      }
   }

   private List<Run> collectRuns(PDDocument document, int page) throws IOException {
      PageStripper stripper = new PageStripper();
      stripper.setStartPage(page);
      stripper.setEndPage(page);
      stripper.getText(document);
      return stripper.runs;
   }

   private List<List<Run>> buildRows(List<Run> runs) {
      List<Run> sorted = new ArrayList(runs);
      sorted.sort(Comparator.<Run>comparingDouble((r) -> (double) r.y)
              .thenComparingDouble((r) -> (double) r.x));
      List<List<Run>> rows = new ArrayList();
      List<Run> current = null;
      float currentY = 0.0F;

      for(Run run : sorted) {
         if (current == null || Math.abs(run.y - currentY) > 5.0F) {
            current = new ArrayList();
            rows.add(current);
            currentY = run.y;
         }

         current.add(run);
      }

      for(List<Run> row : rows) {
         row.sort(Comparator.comparingDouble((r) -> (double)r.x));
      }

      return rows;
   }

   private String rowText(List<Run> row) {
      StringBuilder sb = new StringBuilder();

      for(Run run : row) {
         if (sb.length() > 0) {
            sb.append(' ');
         }

         sb.append(run.text);
      }

      return sb.toString();
   }

   private float findX(List<Run> row, String keyword) {
      for(Run run : row) {
         if (run.text.contains(keyword)) {
            return run.x;
         }
      }

      return -1.0F;
   }

   private String cell(List<Run> row, float start, float end) {
      StringBuilder sb = new StringBuilder();

      for(Run run : row) {
         if (!(run.x < start) && (!(end >= 0.0F) || !(run.x >= end))) {
            if (sb.length() > 0) {
               sb.append(' ');
            }

            sb.append(run.text);
         }
      }

      return sb.toString().trim();
   }

   private float[] fixBounds(int group, float[] bounds, float pageWidth) {
      float width = pageWidth > 0.0F ? pageWidth : 595.0F;
      float[] defaults;
      if (group == 1) {
         defaults = new float[]{width * 0.306F, width * 0.526F};
      } else if (group == 2) {
         defaults = new float[]{width * 0.73F};
      } else {
         defaults = new float[]{width * 0.206F};
      }

      float[] fixed = defaults.clone();
      if (bounds != null) {
         for(int i = 0; i < fixed.length && i < bounds.length; ++i) {
            if (bounds[i] > 0.0F) {
               fixed[i] = bounds[i];
            }
         }
      }

      return fixed;
   }

   private MainRow parseMain(List<Run> row, float[] bounds) {
      float typeX = bounds == null ? -1.0F : bounds[0];
      float merchantX = bounds == null ? -1.0F : bounds[1];
      if (!(typeX < 0.0F) && !(merchantX < 0.0F)) {
         String head = this.cell(row, -Float.MAX_VALUE, typeX);
         Matcher dateMatcher = DATE_PATTERN.matcher(head);
         if (!dateMatcher.find()) {
            return null;
         } else {
            LocalDate date = parseDate(dateMatcher.group());
            if (date == null) {
               return null;
            } else {
               int headEnd = dateMatcher.end();
               Matcher timeMatcher = TIME_PATTERN.matcher(head);
               String time = "00:00:00";
               if (timeMatcher.find(headEnd)) {
                  time = normalizeTime(timeMatcher.group());
                  headEnd = timeMatcher.end();
               }

               String category = this.cell(row, typeX, merchantX);
               if (category.isEmpty() && headEnd < head.length()) {
                  category = head.substring(headEnd).trim();
               }

               String merchant = this.cleanCell(this.cell(row, merchantX, -1.0F));
               return new MainRow(date.toString(), time, category, merchant);
            }
         }
      } else {
         return null;
      }
   }

   private StatusRow parseStatus(List<Run> row, float[] bounds) {
      float statusX = bounds == null ? -1.0F : bounds[0];
      if (statusX < 0.0F) {
         return null;
      } else {
         String remark = this.cell(row, -Float.MAX_VALUE, statusX);
         String type = this.cell(row, statusX, -1.0F);
         return type.isEmpty() ? null : new StatusRow(this.cleanCell(remark), this.cleanCell(type));
      }
   }

   private String cleanCell(String raw) {
      String text = raw == null ? "" : raw.trim();
      return text.equals("/") ? "" : text;
   }

   private static LocalDate parseDate(String raw) {
      String text = raw.trim().replace('/', '-').replace('.', '-').replace("年", "-").replace("月", "-").replace("日", "");
      String[] parts = text.split("-");
      if (parts.length != 3) {
         return null;
      } else {
         try {
            return LocalDate.of(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
         } catch (Exception var4) {
            return null;
         }
      }
   }

   private static String normalizeTime(String raw) {
      String text = raw.trim().replace('∶', ':').replace('：', ':');
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

   private static boolean isNeutral(String raw) {
      String text = raw == null ? "" : raw.trim();
      return text.equals("/") || text.contains("不计");
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
      String text = raw == null ? "" : raw;
      Matcher money = MONEY_PATTERN.matcher(text);
      Matcher number = NUMBER_PATTERN.matcher(text);
      String value = null;
      if (money.find()) {
         value = money.group(1);
      } else if (number.find()) {
         value = number.group(1);
      }

      if (value == null) {
         return Double.NaN;
      } else {
         try {
            return Math.abs(Double.parseDouble(value.replace(",", "")));
         } catch (NumberFormatException var6) {
            return Double.NaN;
         }
      }
   }

   private static final class Run {
      final float x;
      final float y;
      final String text;

      Run(float x, float y, String text) {
         this.x = x;
         this.y = y;
         this.text = text;
      }
   }

   private static final class MainRow {
      final String date;
      final String time;
      final String category;
      final String merchant;

      MainRow(String date, String time, String category, String merchant) {
         this.date = date;
         this.time = time;
         this.category = category;
         this.merchant = merchant;
      }
   }

   private static final class StatusRow {
      final String remark;
      final String type;

      StatusRow(String remark, String type) {
         this.remark = remark;
         this.type = type;
      }
   }

   private static final class PageStripper extends PDFTextStripper {
      final List<Run> runs = new ArrayList();

      PageStripper() throws IOException {
      }

      protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
         if (text != null && textPositions != null && !textPositions.isEmpty()) {
            String value = text.replace(' ', ' ').trim();
            if (!value.isEmpty()) {
               TextPosition first = (TextPosition)textPositions.get(0);
               this.runs.add(new Run(first.getXDirAdj(), first.getYDirAdj(), value));
            }
         }
      }
   }
}
