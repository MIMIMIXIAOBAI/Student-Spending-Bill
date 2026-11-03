package com.campusbill.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import com.campusbill.app.R.string;
import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.CategoryClassifier;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.databinding.ActivityImportBinding;
import com.campusbill.app.ui.PressScaleHelper;
import com.campusbill.app.imports.BillCsvParser;
import com.campusbill.app.imports.BillPasteParser;
import com.campusbill.app.imports.BillPdfParser;
import com.campusbill.app.imports.BillXlsxParser;
import com.campusbill.app.imports.ImportResult;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportActivity extends AppCompatActivity {
   private static final String[] CSV_MIME_TYPES = new String[]{"text/csv", "text/comma-separated-values", "application/csv", "text/plain", "application/vnd.ms-excel"};
   private static final String[] DOC_MIME_TYPES = new String[]{"application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
   private ActivityImportBinding binding;
   private final ExecutorService executor = Executors.newSingleThreadExecutor();
   private ActivityResultLauncher<String[]> csvPicker;
   private ActivityResultLauncher<String[]> docPicker;

   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.binding = ActivityImportBinding.inflate(this.getLayoutInflater());
      this.setContentView(this.binding.getRoot());
      PressScaleHelper.attach(this.binding.btnPasteImport);
      PressScaleHelper.attach(this.binding.btnImportCsv);
      PressScaleHelper.attach(this.binding.btnImportPdfXlsx);
      this.binding.btnBack.setOnClickListener((v) -> this.finish());
      PDFBoxResourceLoader.init(this.getApplicationContext());
      this.csvPicker = this.registerForActivityResult(new ActivityResultContracts.OpenDocument(), (uri) -> {
         if (uri != null) {
            this.importCsv(uri);
         }

      });
      this.docPicker = this.registerForActivityResult(new ActivityResultContracts.OpenDocument(), (uri) -> {
         if (uri != null) {
            this.readDoc(uri);
         }

      });
      this.binding.btnPasteImport.setOnClickListener((v) -> this.pasteImport());
      this.binding.btnImportCsv.setOnClickListener((v) -> this.openCsvPicker());
      this.binding.btnImportPdfXlsx.setOnClickListener((v) -> this.docPicker.launch(DOC_MIME_TYPES));
      this.applyBottomInset();
   }

   protected void onDestroy() {
      super.onDestroy();
      this.executor.shutdown();
   }

   private void applyBottomInset() {
      int side = (int)(16.0F * this.getResources().getDisplayMetrics().density);
      int baseBottom = (int)(16.0F * this.getResources().getDisplayMetrics().density);
      int totalTop = this.binding.tvPrivacyTip.getPaddingTop();
      ViewCompat.setOnApplyWindowInsetsListener(this.binding.getRoot(), (v, insets) -> {
         int bottom = insets.getInsets(Type.navigationBars()).bottom;
         this.binding.tvPrivacyTip.setPadding(side, totalTop, side, baseBottom + bottom);
         return insets;
      });
   }

   private void pasteImport() {
      String text = this.readClipboardText();
      if (text.isEmpty()) {
         Toast.makeText(this, string.import_clipboard_empty, 0).show();
         this.openCsvPicker();
      } else {
         ImportResult result = (new BillPasteParser()).parse(text);
         if (result.bills.isEmpty()) {
            Toast.makeText(this, string.import_parse_none, 0).show();
            this.openCsvPicker();
         } else {
            this.saveBills(result, this.getString(string.import_batch_paste), "PASTE");
         }
      }
   }

   private String readClipboardText() {
      ClipboardManager manager = (ClipboardManager)this.getSystemService("clipboard");
      if (manager != null && manager.hasPrimaryClip()) {
         ClipData clip = manager.getPrimaryClip();
         if (clip != null && clip.getItemCount() != 0) {
            CharSequence text = clip.getItemAt(0).coerceToText(this);
            return text == null ? "" : text.toString().trim();
         } else {
            return "";
         }
      } else {
         return "";
      }
   }

   private void openCsvPicker() {
      this.csvPicker.launch(CSV_MIME_TYPES);
   }

   private void importCsv(Uri uri) {
      String name = this.queryDisplayName(uri);
      if (name != null && name.toLowerCase(Locale.CHINA).endsWith(".csv")) {
         Toast.makeText(this, string.import_parsing, 0).show();
         this.executor.execute(() -> {
            try {
               InputStream in = this.getContentResolver().openInputStream(uri);

               try {
                  if (in == null) {
                     throw new IOException("empty stream");
                  }

                  ImportResult result = (new BillCsvParser()).parse(in);
                  this.runOnUiThread(() -> {
                     if (result.bills.isEmpty()) {
                        this.showResult(0, result.failed);
                     } else {
                        this.saveBills(result, name, "CSV");
                     }

                  });
               } catch (Throwable var7) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable x2) {
                        var7.addSuppressed(x2);
                     }
                  }

                  throw var7;
               }

               if (in != null) {
                  in.close();
               }
            } catch (Exception var8) {
               this.runOnUiThread(() -> Toast.makeText(this, string.import_read_failed, 0).show());
            }

         });
      } else {
         Toast.makeText(this, string.import_file_invalid, 0).show();
      }
   }

   private void readDoc(Uri uri) {
      String name = this.queryDisplayName(uri);
      String lower = name == null ? "" : name.toLowerCase(Locale.CHINA);
      boolean isPdf = lower.endsWith(".pdf");
      boolean isXlsx = lower.endsWith(".xlsx") || lower.endsWith(".xls");
      if (!isPdf && !isXlsx) {
         Toast.makeText(this, string.import_file_invalid, 0).show();
      } else {
         Toast.makeText(this, string.import_parsing, 0).show();
         this.executor.execute(() -> {
            try {
               InputStream in = this.getContentResolver().openInputStream(uri);

               try {
                  if (in == null) {
                     throw new IOException("empty stream");
                  }

                  ImportResult result = isPdf ? (new BillPdfParser()).parse(in) : (new BillXlsxParser()).parse(in);
                  this.runOnUiThread(() -> {
                     if (result.bills.isEmpty()) {
                        this.showDocFailed();
                     } else {
                        this.saveBills(result, name == null ? "" : name, isPdf ? "PDF" : "XLSX");
                     }

                  });
               } catch (Throwable var8) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable x2) {
                        var8.addSuppressed(x2);
                     }
                  }

                  throw var8;
               }

               if (in != null) {
                  in.close();
               }
            } catch (Exception var9) {
               this.runOnUiThread(() -> Toast.makeText(this, string.import_read_failed, 0).show());
            }

         });
      }
   }

   private void saveBills(final ImportResult result, String batchName, String source) {
      for (BillEntity bill : result.bills) {
         bill.category = CategoryClassifier.classify(bill.type, bill.category, bill.merchant, bill.remark);
      }
      DBHelper.getInstance(this).insertBatch(batchName, source, result.bills, new DBHelper.DataCallback<Integer>() {
         public void onSuccess(Integer rows) {
            int success = rows == null ? 0 : rows;
            ImportActivity.this.showResult(success, result.failed);
         }

         public void onError(Exception e) {
            Toast.makeText(ImportActivity.this, string.import_read_failed, 0).show();
         }
      });
   }

   private void showResult(int success, int failed) {
      (new AlertDialog.Builder(this)).setTitle(string.import_result_title).setMessage(this.getString(string.import_result_format, new Object[]{success, failed})).setPositiveButton(string.btn_close, (DialogInterface.OnClickListener)null).show();
   }

   private void showDocFailed() {
      (new AlertDialog.Builder(this)).setTitle(string.import_doc_failed_title).setMessage(string.import_doc_failed_message).setPositiveButton(string.btn_close, (DialogInterface.OnClickListener)null).show();
   }

   private String queryDisplayName(Uri uri) {
      try {
         Cursor cursor = this.getContentResolver().query(uri, (String[])null, (String)null, (String[])null, (String)null);

         String var4;
         label54: {
            try {
               if (cursor != null && cursor.moveToFirst()) {
                  int index = cursor.getColumnIndex("_display_name");
                  if (index >= 0) {
                     var4 = cursor.getString(index);
                     break label54;
                  }
               }
            } catch (Throwable var6) {
               if (cursor != null) {
                  try {
                     cursor.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (cursor != null) {
               cursor.close();
            }

            return uri.getLastPathSegment();
         }

         if (cursor != null) {
            cursor.close();
         }

         return var4;
      } catch (Exception var7) {
         return uri.getLastPathSegment();
      }
   }
}
