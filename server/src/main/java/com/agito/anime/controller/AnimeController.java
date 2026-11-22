package com.agito.anime.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.Result;
import com.agito.anime.dto.AnimeQueryDTO;
import com.agito.anime.service.AnimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 番剧中心 / 详情 / 搜索（游客只读开放）
 */
@RestController
@RequestMapping("/api/anime")
@RequiredArgsConstructor
@Tag(name = "02-番剧中心")
public class AnimeController {

    private final AnimeService animeService;
    private final com.agito.anime.service.RecommendService recommendService;

    @Operation(summary = "番剧列表：多维筛选 + 分页（游客可用）")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(AnimeQueryDTO query) {
        var page = animeService.list(query);
        return Result.ok(Map.of(
                "list", page.getRecords(),
                "total", page.getTotal(),
                "pageNum", page.getCurrent(),
                "pageSize", page.getSize()));
    }

    @Operation(summary = "番剧详情：多源评分 + 平台外链 + 标签")
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(animeService.detail(id));
    }

    @Operation(summary = "搜索联想（输入联想词，8 条）")
    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword) {
        return Result.ok(animeService.suggest(keyword));
    }

    @Operation(summary = "热门搜索词（空态推荐）")
    @GetMapping("/hot-words")
    public Result<?> hotWords() {
        return Result.ok(animeService.hotWords());
    }

    @Operation(summary = "推荐换一批（需登录，游客返回原推荐）")
    @PostMapping("/refresh")
    public Result<?> refresh(@RequestParam(required = false, defaultValue = "guess") String column) {
        Long uid = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return Result.ok(recommendService.refreshBatch(uid, column));
    }
}
