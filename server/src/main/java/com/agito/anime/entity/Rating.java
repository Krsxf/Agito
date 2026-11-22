package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rating")
public class Rating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long animeId;
    private BigDecimal score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
