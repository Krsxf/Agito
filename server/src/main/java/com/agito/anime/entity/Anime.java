package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("anime")
public class Anime {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String alias;
    private String coverUrl;
    private Integer year;
    private java.time.LocalDate premiereDate;
    private String region;
    private Integer airStatus;
    private Integer episodeCount;
    private String duration;
    private String studio;
    private String director;
    private String voiceActor;
    private Integer weekday;
    private String description;
    private String source;
    private String sourceId;
    private Long playCount;
    private Integer followCount;
    private Integer ratingCount;
    private Integer socialDiscussion;
    private java.math.BigDecimal totalScore;
    private java.math.BigDecimal heatScore;
    private Integer state;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
