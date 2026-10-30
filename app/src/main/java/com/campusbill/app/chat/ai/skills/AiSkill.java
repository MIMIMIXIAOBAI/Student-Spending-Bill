package com.campusbill.app.chat.ai.skills;

/** 一条内置 AI 技能：名称、说明与提示词模板 */
public class AiSkill {

    public final String name;
    public final String description;
    public final String prompt;

    public AiSkill(String name, String description, String prompt) {
        this.name = name;
        this.description = description;
        this.prompt = prompt;
    }
}
