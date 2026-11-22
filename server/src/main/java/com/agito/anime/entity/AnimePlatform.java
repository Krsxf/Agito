package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("anime_platform")
public class AnimePlatform {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long animeId;
    private String platformName;
    private String url;
    private Integer weight;
    private Integer isPaid;
    private Integer regionLimit;
    private Integer linkStatus;
    private LocalDateTime checkTime;
}
