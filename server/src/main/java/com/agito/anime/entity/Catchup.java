package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("catchup")
public class Catchup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long animeId;
    private Integer priority;
    private Integer catchupStatus;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
