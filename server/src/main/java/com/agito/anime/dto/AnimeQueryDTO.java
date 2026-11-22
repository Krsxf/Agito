package com.agito.anime.dto;

import lombok.Data;

import java.util.List;

/**
 * 番剧中心 / 补番助手共用的多条件筛选入参
 */
@Data
public class AnimeQueryDTO {
    private String keyword;        // 关键词（支持拼音首字母联想，由前端映射）
    private String tag;            // 类型标签
    private Integer year;          // 年份
    private Integer yearFrom;      // 年份起（补番场景）
    private Integer yearTo;        // 年份止
    private String region;         // 地区
    private Integer airStatus;     // 播出状态：1连载中 2已完结 3待上线
    private Double scoreFrom;      // 评分区间下限（补番场景）
    private Double scoreTo;        // 评分区间上限
    private Integer episodeFrom;    // 集数区间（补番场景）
    private Integer episodeTo;
    private Long pageNum = 1L;
    private Long pageSize = 20L;
    /** 排序字段：heat 热度 / score 评分 / year 年份 */
    private String sortBy = "heat";
}
