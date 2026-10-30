package com.campusbill.app.chat.ai.presets;

/**
 * AI 预设配置：系统提示词与快捷提问，供接入真实 AI 时使用。
 * 后续可在此目录继续补充其他预设。
 */
public final class AiPresets {

    /** 系统提示词：限定 AI 扮演校园消费助手 */
    public static final String SYSTEM_PROMPT =
            "你是校园消费账单 App 的 AI 消费助手，面向学生用户。"
                    + "下方会附带用户本机的真实账单快照（收支、分类、商户、近期流水），"
                    + "回答时必须优先基于这些账单数据分析，引用其中的具体金额与分类；"
                    + "回答要简短、口语化、可执行，优先给出具体的省钱或预算建议，"
                    + "不要编造快照里不存在的账单数据。";

    /** 快捷提问：对话页可展示的预设问题 */
    public static final String[] QUICK_QUESTIONS = {
            "这个月我花得最多的是哪一类？",
            "帮我看看哪些消费可以省下来",
            "按我的消费，预算定多少比较合适？"
    };

    private AiPresets() {
    }
}
