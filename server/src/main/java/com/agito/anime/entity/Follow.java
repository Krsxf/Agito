package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("follow")
public class Follow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long animeId;
    private Integer currentEpisode;
    private Integer totalEpisode;
    private Integer progress;
    private Integer followStatus;
    private LocalDateTime nextEpisodeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
