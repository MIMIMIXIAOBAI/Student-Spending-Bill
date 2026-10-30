package com.campusbill.app.chat.ai.skills;

/**
 * 内置技能列表：接入真实 AI 时按用户意图匹配对应技能。
 * 当前为预留配置，未参与对话逻辑。
 */
public final class AiSkills {

    public static final AiSkill[] BUILT_IN = {
            new AiSkill(
                    "预算规划",
                    "根据本月账单给出下月预算建议",
                    "请参考本月各项消费金额，给出下个月的分项预算建议，并说明依据。"),
            new AiSkill(
                    "消费诊断",
                    "找出占比较高的分类与可压缩支出",
                    "请分析本月账单中占比最高的消费分类，指出最值得压缩的两项，并给出替代方案。"),
            new AiSkill(
                    "记账提醒",
                    "提醒用户补记遗漏的日常消费",
                    "请用一句话提醒用户及时补记当天的餐饮、交通等小额消费。")
    };

    private AiSkills() {
    }
}
