package com.agito.anime.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.Result;
import com.agito.anime.entity.Notification;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户中心 + 更新提醒（登录用户）
 */
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Tag(name = "08-提醒")
public class NotificationController {

    private final com.agito.anime.mapper.NotificationMapper notificationMapper;

    @Operation(summary = "未读提醒列表（侧边菜单红点）")
    @GetMapping("/list")
    public Result<List<Notification>> list() {
        return Result.ok(notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, StpUtil.getLoginIdAsLong())
                        .orderByDesc(Notification::getId)
                        .last("LIMIT 20")));
    }

    @Operation(summary = "未读数（红点数字角标）")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long cnt = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, StpUtil.getLoginIdAsLong())
                .eq(Notification::getIsRead, 0));
        return Result.ok(Map.of("count", cnt));
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        Notification upd = new Notification();
        upd.setIsRead(1);
        notificationMapper.update(upd, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, StpUtil.getLoginIdAsLong())
                .eq(Notification::getIsRead, 0));
        return Result.ok();
    }
}
