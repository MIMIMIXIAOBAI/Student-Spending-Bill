package com.campusbill.app.imports;

import com.campusbill.app.data.BillEntity;
import java.util.ArrayList;
import java.util.List;

public class ImportResult {
   public final List<BillEntity> bills = new ArrayList();
   public int failed;
}
