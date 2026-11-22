package com.agito.anime.service;

import java.util.Map;

public interface JumpService {

    /**
     * 跳转外链服务（4.2.3.6 节）：链接有效性校验 + 失效标记 + 跳转日志。
     * 合规边界：系统仅提供正版平台外链跳转，不承载任何视频内容。
     */
    Map<String, Object> jump(Long animeId, String platformName);

    /** 后台/定时：批量校验失效链接并标红（管理端修复入口的数据源） */
    int checkLinks();
}
