package com.agito.anime.controller;

import com.agito.anime.common.Result;
import com.agito.anime.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 排行榜（游客可用）
 */
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
@Tag(name = "04-排行榜")
public class RankController {

    private final RankService rankService;

    @Operation(summary = "排行榜：三榜 Tab + 周/月/总榜")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int rankType,
            @RequestParam(defaultValue = "3") int period) {
        return Result.ok(rankService.list(rankType, period));
    }
}
