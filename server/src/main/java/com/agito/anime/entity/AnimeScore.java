package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("anime_score")
public class AnimeScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long animeId;
    private String sourceName;
    private BigDecimal score;
    private Integer ratingCount;
    private BigDecimal confidence;
    private LocalDateTime createTime;
}
