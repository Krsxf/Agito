package com.agito.anime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("import_task")
public class ImportTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private String source;
    private String fileName;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer status;
    private String detail;
    private String operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
