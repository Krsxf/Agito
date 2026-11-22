package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("interest_tag")
public class InterestTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagName;
    private String tagCode;
    private Integer sortNo;
}
