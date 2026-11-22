package com.agito.anime.service;

import com.agito.anime.entity.SysLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.agito.anime.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 统一日志横切服务（3.4.7 节）：操作/登录/跳转/采集四类日志，
 * DB 落 sys_log 表；文件日志由 Logback 滚动留存 180 天（application.yml 配置）。
 */
@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysLogMapper sysLogMapper;

    @Async
    public void log(Long userId, String logType, String action, String detail) {
        try {
            SysLog entity = new SysLog();
            entity.setUserId(userId);
            entity.setLogType(logType);
            entity.setAction(action);
            entity.setDetail(detail != null && detail.length() > 480 ? detail.substring(0, 480) : detail);
            entity.setIp("-");
            sysLogMapper.insert(entity);
        } catch (Exception ignored) {
            // 日志失败不影响主流程
        }
    }

    public Page<SysLog> page(long pageNum, long pageSize, String logType) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        qw.eq(logType != null && !logType.isEmpty(), SysLog::getLogType, logType)
                .orderByDesc(SysLog::getId);
        return sysLogMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }
}
