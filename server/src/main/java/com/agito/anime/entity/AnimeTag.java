package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("anime_tag")
public class AnimeTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long animeId;
    private String tag;
}
