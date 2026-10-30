package com.campusbill.app.data;

import java.util.Locale;

/**
 * 本地分类细化：根据商户、备注与原始分类关键词，把导入账单归入更细的消费 / 收入分类。
 * 全部在本机完成，不联网，也不改变金额、时间等其它字段。
 */
public final class CategoryClassifier {

    private CategoryClassifier() {
    }

    /**
     * @param type     账单类型 TYPE_EXPENSE / TYPE_INCOME
     * @param category 原始分类（可能来自导入文件的交易类型）
     * @param merchant 商户名称
     * @param remark   备注
     * @return 细化后的分类；无法识别时尽量保留原分类
     */
    public static String classify(int type, String category, String merchant, String remark) {
        String text = normalize(category) + ' ' + normalize(merchant) + ' ' + normalize(remark);
        String refined = type == BillEntity.TYPE_INCOME ? classifyIncome(text) : classifyExpense(text);
        if (refined != null) {
            return refined;
        }
        return fallback(type, category);
    }

    private static String classifyExpense(String text) {
        if (has(text, "食堂", "饭堂", "伙食")) {
            return "食堂";
        }
        if (has(text, "奶茶", "咖啡", "饮品", "茶饮", "瑞幸", "星巴克", "蜜雪", "喜茶", "奈雪", "茶百道", "古茗", "沪上阿姨")) {
            return "奶茶";
        }
        if (has(text, "外卖", "美团外卖", "饿了么")) {
            return "外卖";
        }
        if (has(text, "餐饮", "餐厅", "美食", "小吃", "食品", "饭店", "烧烤", "火锅", "面馆", "米粉", "快餐", "汉堡", "肯德基", "麦当劳")) {
            return "餐饮";
        }
        if (has(text, "超市", "便利店", "百货", "淘宝", "天猫", "京东", "拼多多", "唯品会", "得物", "商城", "盒马", "购物", "全家", "罗森", "零食", "货柜")) {
            return "购物";
        }
        if (has(text, "书店", "文具", "图书", "课程", "学费", "培训", "教育", "考试", "打印", "学习")) {
            return "学习";
        }
        if (has(text, "会员", "续费", "订阅", "包月", "vip", "月卡", "年卡")) {
            return "续费";
        }
        if (has(text, "电信", "移动", "联通", "话费", "流量", "宽带", "通信")) {
            return "话费";
        }
        if (has(text, "滴滴", "打车", "出租", "公交", "地铁", "高铁", "火车", "单车", "哈啰", "交通", "车费")) {
            return "交通";
        }
        if (has(text, "医院", "药房", "药店", "诊所", "医疗", "口腔", "体检")) {
            return "医疗";
        }
        if (has(text, "电影", "游戏", "娱乐", "网吧", "ktv", "音乐", "视频", "演出", "剧本杀")) {
            return "娱乐";
        }
        if (has(text, "水电", "燃气", "物业", "缴费", "电费", "水费", "供暖", "热水")) {
            return "生活缴费";
        }
        if (has(text, "转账", "红包", "亲属卡", "群收款")) {
            return "转账";
        }
        if (has(text, "日用", "洗发", "沐浴", "洗衣", "理发", "纸巾", "清洁")) {
            return "日用";
        }
        return null;
    }

    private static String classifyIncome(String text) {
        if (has(text, "生活费")) {
            return "生活费";
        }
        if (has(text, "奖学金", "助学金", "补贴", "奖金")) {
            return "奖学金";
        }
        if (has(text, "红包")) {
            return "红包";
        }
        if (has(text, "退款", "退货", "返现")) {
            return "退款";
        }
        if (has(text, "报销")) {
            return "报销";
        }
        if (has(text, "兼职", "工资", "薪酬", "家教", "实习", "劳务")) {
            return "兼职";
        }
        if (has(text, "转账", "转入", "收款")) {
            return "转账";
        }
        return null;
    }

    private static String fallback(int type, String category) {
        String value = category == null ? "" : category.trim();
        if (value.isEmpty()) {
            return "其他";
        }
        String generic = value.toLowerCase(Locale.CHINA);
        if (type == BillEntity.TYPE_INCOME) {
            if (generic.contains("收款") || generic.contains("转账")) {
                return "转账";
            }
            if (generic.contains("消费") || generic.contains("商户")) {
                return "其他";
            }
        } else if (generic.contains("消费") || generic.contains("商户")) {
            return "其他";
        }
        return value;
    }

    private static boolean has(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.CHINA);
    }
}
