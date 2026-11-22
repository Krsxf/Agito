package com.agito.anime.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 番剧详情出参：信息分层（界面文档 4.6 节）
 */
@Data
public class AnimeDetailVO {
    private Long id;
    private String title;
    private String alias;
    private String coverUrl;
    private Integer year;
    private String region;
    private Integer airStatus;
    private Integer episodeCount;
    private String duration;
    private String studio;
    private String director;
    private String voiceActor;
    private Integer weekday;
    private String description;
    private BigDecimal totalScore;
    private BigDecimal heatScore;
    private Long playCount;
    private Integer followCount;
    private Integer ratingCount;
    private Integer socialDiscussion;
    private List<String> tags;
    /** 多源评分卡（综合 + 豆瓣 + B站 + MAL） */
    private List<ScoreItem> scoreBoard;
    /** 可观看平台（按权重排序，标注付费/限地区/失效） */
    private List<PlatformItem> platforms;

    @Data
    public static class ScoreItem {
        private String sourceName;
        private BigDecimal score;
        private Integer ratingCount;
    }

    @Data
    public static class PlatformItem {
        private Long id;
        private String platformName;
        private String url;
        private Integer isPaid;
        private Integer regionLimit;
        private Integer linkStatus;
    }
}
