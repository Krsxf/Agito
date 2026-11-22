package com.agito.anime.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.Result;
import com.agito.anime.service.JumpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 网页跳转：链接校验 + 跳转日志（游客可跳转，写操作边界在行为日志侧处理）
 */
@RestController
@RequestMapping("/api/jump")
@RequiredArgsConstructor
@Tag(name = "05-网页跳转")
public class JumpController {

    private final JumpService jumpService;

    @Operation(summary = "去观看：校验链接有效性并返回平台 URL（合规边界：只跳正版平台）")
    @PostMapping("/{animeId}")
    public Result<Map<String, Object>> jump(@PathVariable Long animeId,
                                            @RequestParam String platform) {
        return Result.ok(jumpService.jump(animeId, platform));
    }
}
