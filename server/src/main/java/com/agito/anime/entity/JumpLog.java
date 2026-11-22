package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jump_log")
public class JumpLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long animeId;
    private String platformName;
    private String url;
    private Integer isValid;
    private LocalDateTime jumpTime;
}
