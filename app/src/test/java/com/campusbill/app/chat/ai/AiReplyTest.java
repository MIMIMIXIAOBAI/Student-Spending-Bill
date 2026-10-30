package com.campusbill.app.chat.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.campusbill.app.data.CategoryAmount;
import com.campusbill.app.data.MerchantAmount;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地意图识别与回复模板的单元测试（纯 Java，不依赖设备）。
 */
public class AiReplyTest {

    @Test
    public void intentMatcher_按优先级识别7类意图() {
        assertEquals(IntentMatcher.Intent.BUDGET, IntentMatcher.match("本月预算还剩多少"));
        assertEquals(IntentMatcher.Intent.BUDGET, IntentMatcher.match("我是不是超支了"));
        assertEquals(IntentMatcher.Intent.TOTAL, IntentMatcher.match("本月花了多少钱"));
        assertEquals(IntentMatcher.Intent.TOTAL, IntentMatcher.match("今天花了多少"));
        assertEquals(IntentMatcher.Intent.CATEGORY, IntentMatcher.match("哪类花最多"));
        assertEquals(IntentMatcher.Intent.CATEGORY, IntentMatcher.match("餐饮花了多少"));
        assertEquals(IntentMatcher.Intent.RANK, IntentMatcher.match("花得最多的商户是谁"));
        assertEquals(IntentMatcher.Intent.ADVICE, IntentMatcher.match("给点省钱建议"));
        assertEquals(IntentMatcher.Intent.HELP, IntentMatcher.match("怎么导入账单"));
        assertEquals(IntentMatcher.Intent.FALLBACK, IntentMatcher.match("你好呀"));
    }

    @Test
    public void intentMatcher_识别今日查询() {
        assertTrue(IntentMatcher.isTodayQuery("今天花了多少"));
        assertTrue(IntentMatcher.isTodayQuery("今日消费"));
        assertFalse(IntentMatcher.isTodayQuery("本月总支出"));
    }

    @Test
    public void generator_预算有剩余与超支() {
        String positive = ReplyGenerator.budget(500, 432.26);
        assertTrue(positive.contains("还剩 ¥67.74"));

        String negative = ReplyGenerator.budget(300, 432.26);
        assertTrue(negative.contains("超支 ¥132.26"));

        assertEquals(AiReplyConstants.BUDGET_UNSET, ReplyGenerator.budget(0, 432.26));
    }

    @Test
    public void generator_总额与分类排行() {
        String month = ReplyGenerator.totalMonth("2026-10", 432.26);
        assertTrue(month.contains("2026年10月"));
        assertTrue(month.contains("¥432.26"));

        String day = ReplyGenerator.totalDay("2026-10-31", 26.5);
        assertTrue(day.contains("10月31日"));
        assertTrue(day.contains("¥26.50"));

        String category = ReplyGenerator.category("2026-10", fakeCategories());
        assertTrue(category.contains("1. 餐饮 ¥134.90"));
        assertTrue(category.contains("2. 购物 ¥130.78"));
        assertTrue(category.contains("约占 31.2%"));
    }

    @Test
    public void generator_商户排行与省钱建议() {
        String rank = ReplyGenerator.merchantTop("2026-10", fakeMerchants());
        assertTrue(rank.contains("Top2"));
        assertTrue(rank.contains("1. 拼多多平台商户 ¥130.78"));

        String advice = ReplyGenerator.advice("2026-10", 432.26, fakeCategories());
        assertTrue(advice.contains("「餐饮」占 31.2%"));
        assertTrue(advice.contains("¥121.41"));
        assertTrue(advice.contains("约每周 ¥30.35"));
    }

    @Test
    public void generator_空数据使用兜底文案() {
        assertTrue(ReplyGenerator.totalMonth("2026-10", 0).contains("还没有消费记录"));
        assertTrue(ReplyGenerator.category("2026-10", new ArrayList<>()).contains("没有分类数据"));
        assertTrue(ReplyGenerator.merchantTop("2026-10", new ArrayList<>()).contains("没有商户排行"));
        assertEquals(AiReplyConstants.ADVICE_EMPTY, ReplyGenerator.advice("2026-10", 0, fakeCategories()));
        assertEquals(AiReplyConstants.HELP, ReplyGenerator.help());
        assertEquals(AiReplyConstants.FALLBACK, ReplyGenerator.fallback());
    }

    private List<CategoryAmount> fakeCategories() {
        List<CategoryAmount> list = new ArrayList<>();
        list.add(category("餐饮", 134.90));
        list.add(category("购物", 130.78));
        list.add(category("交通", 67.00));
        list.add(category("其他", 99.58));
        return list;
    }

    private CategoryAmount category(String name, double total) {
        CategoryAmount item = new CategoryAmount();
        item.category = name;
        item.total = total;
        return item;
    }

    private List<MerchantAmount> fakeMerchants() {
        List<MerchantAmount> list = new ArrayList<>();
        list.add(merchant("拼多多平台商户", 130.78));
        list.add(merchant("学校食堂", 96.00));
        return list;
    }

    private MerchantAmount merchant(String name, double total) {
        MerchantAmount item = new MerchantAmount();
        item.merchant = name;
        item.total = total;
        return item;
    }
}
