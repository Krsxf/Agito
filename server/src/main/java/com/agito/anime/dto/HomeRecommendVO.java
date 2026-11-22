package com.agito.anime.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页三栏目推荐出参（界面文档 4.4 节）：
 * today 今日推荐 / guess 猜你喜欢 / newAnime 新番速递
 */
@Data
public class HomeRecommendVO {
    private List<Card> today;
    private List<Card> guess;
    private List<NewCard> newAnime;
    /** 冷启动状态提示（行为不足 20 条时为 true，前端可展示引导文案） */
    private Boolean coldStart;

    @Data
    public static class Card {
        private Long id;
        private String title;
        private String tag;
        private BigDecimal score;
        private Integer airStatus;
        /** 封面 URL（服务器上传静态资源，为空时前端渐变占位） */
        private String coverUrl;
    }

    @Data
    public static class NewCard {
        private Long id;
        private String title;
        private Integer weekday;
        private Integer episodeCount;
        private String studio;
        private String coverUrl;
    }
}
