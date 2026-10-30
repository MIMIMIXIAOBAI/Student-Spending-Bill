package com.campusbill.app.chat.ai;

/**
 * AI 回复话术模板：所有回复文案集中在此，通过 String.format 替换数值占位符。
 */
public final class AiReplyConstants {

    /** 预算有剩余 */
    public static final String BUDGET_POSITIVE =
            "本月预算还剩 ¥%s（预算 ¥%s，已花 ¥%s）。\n"
                    + "还可以放心花，但建议留一点弹性，别把额度一次用光。";

    /** 预算超支 */
    public static final String BUDGET_NEGATIVE =
            "本月已经超支 ¥%s（预算 ¥%s，已花 ¥%s）。\n"
                    + "建议先停掉非必要消费，把餐饮、娱乐的额度收紧一点。";

    /** 未设置预算 */
    public static final String BUDGET_UNSET =
            "你还没有设置本月预算。\n"
                    + "在首页预算区输入金额后，我就能帮你算剩余和超支情况啦。";

    /** 本月支出总额 */
    public static final String TOTAL_MONTH =
            "%s 本月共支出 ¥%s。\n"
                    + "想看得更细的话，可以问我「消费分类」或「消费排行」。";

    /** 当日支出总额 */
    public static final String TOTAL_DAY =
            "%s 当天共支出 ¥%s。\n"
                    + "当天的小额消费也记得补记一下哦。";

    /** 本月无消费记录 */
    public static final String TOTAL_EMPTY =
            "%s 还没有消费记录，记账后我就能帮你分析啦。";

    /** 分类排行：参数为月份、排行文本、第一名分类、第一名占比 */
    public static final String CATEGORY =
            "%s 消费分类排行：\n%s\n"
                    + "其中「%s」花得最多，约占 %s%%。";

    /** 无分类数据 */
    public static final String CATEGORY_EMPTY =
            "%s 还没有消费记录，暂时没有分类数据。";

    /** 商户排行：参数为月份、TopN、排行文本 */
    public static final String RANK =
            "%s 商户消费 Top%d：\n%s\n"
                    + "想省钱可以优先看看常去的这几家。";

    /** 无商户数据 */
    public static final String RANK_EMPTY =
            "%s 还没有消费记录，暂时没有商户排行。";

    /** 省钱建议：参数为月份、总支出、第一分类、占比、分类金额、建议月度额度、建议周额度 */
    public static final String ADVICE =
            "%s 你的支出是 ¥%s，其中「%s」占 %s%%（¥%s）。\n"
                    + "建议把这一类控制到 ¥%s 以内（约每周 ¥%s），先从减少高频小额消费开始。";

    /** 无数据时的省钱建议 */
    public static final String ADVICE_EMPTY =
            "本月还没有消费记录，先记几笔我再来给你建议吧。";

    /** 帮助 / 功能指引 */
    public static final String HELP =
            "我可以帮你：\n"
                    + "1. 查本月 / 今日消费总额\n"
                    + "2. 查预算剩余或超支情况\n"
                    + "3. 查消费分类占比和商户排行\n"
                    + "4. 给省钱建议\n\n"
                    + "记账：点首页底部的「手动记一笔」；\n"
                    + "导入：在导入账单页按提示选择文件即可。";

    /** 兜底回复 */
    public static final String FALLBACK =
            "这个我还不太懂哦～\n"
                    + "我可以帮你查本月总支出、预算剩余、消费分类、消费排行，也可以给你省钱建议。\n"
                    + "试试点下面的快捷提问吧。";

    private AiReplyConstants() {
    }
}
