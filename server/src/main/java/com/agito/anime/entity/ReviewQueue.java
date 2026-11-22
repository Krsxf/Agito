package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_queue")
public class ReviewQueue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String animePayload;
    private String reason;
    private Integer status;
    private String reviewer;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    /** 来源：IMPORT数据导入 / USER用户提交 */
    private String sourceType;
    private Long submitterId;
    private String submitterNote;
    private String platformName;
    private String platformUrl;
}
