package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("hot_rank")
public class HotRank {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long animeId;
    private Integer rankType;
    private Integer period;
    private Integer rankNo;
    private BigDecimal score;
    private LocalDate statDate;
    private LocalDateTime createTime;
}
