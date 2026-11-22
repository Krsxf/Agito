package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rec_config")
public class RecConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private BigDecimal wProfile;
    private BigDecimal wCf;
    private BigDecimal wHot;
    private BigDecimal wRandom;
    private Integer homeSize;
    private Integer cacheMinutes;
    private Integer coldThreshold;
    private LocalDateTime lastRecalc;
    private LocalDateTime updateTime;
}
