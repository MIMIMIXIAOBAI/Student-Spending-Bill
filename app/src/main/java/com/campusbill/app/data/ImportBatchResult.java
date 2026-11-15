package com.campusbill.app.data;

/**
 * 一次导入的去重落库结果。
 * <p>inserted 为实际新写入条数；skipped 为按「付款日期 + 时间 + 金额 + 商户 + 类型」
 * 判定已存在而跳过的重复条数；batchId 为生成的导入批次 id，全部重复时为 -1。</p>
 */
public class ImportBatchResult {
   public int inserted;
   public int skipped;
   public long batchId;
}
