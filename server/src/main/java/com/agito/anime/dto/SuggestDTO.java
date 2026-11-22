package com.agito.anime.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户提交番剧补充建议（原案用户反馈闭环）：
 * 用户提交想上架的番剧名称与跳转链接 → 管理员审核（可修改）→ 上架
 */
@Data
public class SuggestDTO {
    @NotBlank(message = "番剧名称不能为空")
    private String title;

    /** 别名（可选，日文原名/英文等） */
    private String alias;

    /** 跳转链接（必填：用户知道的正版观看页） */
    @NotBlank(message = "观看链接不能为空")
    private String platformUrl;

    /** 平台名称（选填，如 哔哩哔哩；留空则由管理员审核时补全） */
    private String platformName;

    /** 备注意见（选填） */
    private String note;
}
