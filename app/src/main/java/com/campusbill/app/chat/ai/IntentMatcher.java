package com.campusbill.app.chat.ai;

import java.util.regex.Pattern;

/**
 * 意图识别引擎：纯本地关键词正则匹配，无模型、无网络依赖。
 * 按优先级从高到低匹配，命中即返回。
 */
public final class IntentMatcher {

    /** 7 类意图，声明顺序即优先级顺序 */
    public enum Intent {
        /** 预算查询：预算、剩余、超支、还能花多少 */
        BUDGET,
        /** 总额查询：本月花了多少、总支出、今日消费 */
        TOTAL,
        /** 分类查询：哪类花最多、分类占比 */
        CATEGORY,
        /** 排行查询：花得最多的商户、消费排行 */
        RANK,
        /** 建议类：省钱建议、消费建议 */
        ADVICE,
        /** 帮助类：怎么记账、怎么导入 */
        HELP,
        /** 兜底：未命中以上意图 */
        FALLBACK
    }

    private static final Pattern P_BUDGET =
            Pattern.compile("预算|剩余|超支|还能花多少|还能剩多少");
    private static final Pattern P_TOTAL =
            Pattern.compile("本月花了多少|本月支出|花了多少钱|总支出|总消费|今日消费|今天花了|今日花了|今天支出");
    private static final Pattern P_CATEGORY =
            Pattern.compile("哪类花最多|哪一类花|分类占比|消费分类|分类|餐饮|购物|交通|娱乐|学习|通讯|医疗|日用");
    private static final Pattern P_RANK =
            Pattern.compile("花得最多的商户|消费排行|商户排行|排行|TOP\\s*商户", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_ADVICE =
            Pattern.compile("省钱|怎么省|消费建议|给点建议|建议|节约");
    private static final Pattern P_HELP =
            Pattern.compile("怎么记账|如何记账|怎么导入|如何导入|功能说明|怎么用|使用方法|记账怎么|导入");

    private IntentMatcher() {
    }

    /** 匹配用户输入，命中优先级最高的意图，否则返回兜底 */
    public static Intent match(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Intent.FALLBACK;
        }
        String input = text.trim();
        if (P_BUDGET.matcher(input).find()) {
            return Intent.BUDGET;
        }
        if (P_TOTAL.matcher(input).find()) {
            return Intent.TOTAL;
        }
        if (P_CATEGORY.matcher(input).find()) {
            return Intent.CATEGORY;
        }
        if (P_RANK.matcher(input).find()) {
            return Intent.RANK;
        }
        if (P_ADVICE.matcher(input).find()) {
            return Intent.ADVICE;
        }
        if (P_HELP.matcher(input).find()) {
            return Intent.HELP;
        }
        return Intent.FALLBACK;
    }

    /** 总额查询是否需要看「今日」数据 */
    public static boolean isTodayQuery(String text) {
        if (text == null) {
            return false;
        }
        return text.contains("今日") || text.contains("今天");
    }
}
