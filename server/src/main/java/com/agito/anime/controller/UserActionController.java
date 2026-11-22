package com.agito.anime.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.Result;
import com.agito.anime.service.UserActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户行为：评分 / 收藏 / 追番 / 补番（写操作，须登录）
 */
@RestController
@RequestMapping("/api/action")
@RequiredArgsConstructor
@Tag(name = "06-用户行为")
public class UserActionController {

    private final UserActionService actionService;

    @Operation(summary = "评分（1~10）")
    @PostMapping("/rate/{animeId}")
    public Result<Void> rate(@PathVariable Long animeId, @RequestParam double score) {
        actionService.rate(animeId, score);
        return Result.ok();
    }

    @Operation(summary = "收藏 / 取消收藏")
    @PostMapping("/favorite/{animeId}")
    public Result<Void> favorite(@PathVariable Long animeId, @RequestParam boolean on) {
        actionService.favorite(animeId, on);
        return Result.ok();
    }

    @Operation(summary = "加入追番")
    @PostMapping("/follow/{animeId}")
    public Result<Void> follow(@PathVariable Long animeId) {
        actionService.follow(animeId);
        return Result.ok();
    }

    @Operation(summary = "追番进度一键确认（上次看到第 X 集）")
    @PostMapping("/follow/{animeId}/progress")
    public Result<Void> confirmProgress(@PathVariable Long animeId, @RequestParam int episode) {
        actionService.confirmProgress(animeId, episode);
        return Result.ok();
    }

    @Operation(summary = "取消追番")
    @DeleteMapping("/follow/{animeId}")
    public Result<Void> unfollow(@PathVariable Long animeId) {
        actionService.unfollow(animeId);
        return Result.ok();
    }

    @Operation(summary = "我的追番列表 + 周视图日历")
    @GetMapping("/follows")
    public Result<Map<String, Object>> myFollows() {
        return Result.ok(actionService.myFollows());
    }

    @Operation(summary = "加入补番清单")
    @PostMapping("/catchup/{animeId}")
    public Result<Void> addCatchup(@PathVariable Long animeId) {
        actionService.addCatchup(animeId);
        return Result.ok();
    }

    @Operation(summary = "补番完成 / 弃番")
    @PostMapping("/catchup/{animeId}/done")
    public Result<Void> catchupDone(@PathVariable Long animeId, @RequestParam boolean dropped) {
        actionService.catchupDone(animeId, dropped);
        return Result.ok();
    }

    @Operation(summary = "我的补番清单 + 进度横幅")
    @GetMapping("/catchup")
    public Result<Map<String, Object>> myCatchup() {
        return Result.ok(actionService.myCatchup());
    }

    @Operation(summary = "补番清单拖拽排序")
    @PostMapping("/catchup/reorder")
    public Result<Void> reorder(@RequestBody List<Long> orderedIds) {
        actionService.reorderCatchup(orderedIds);
        return Result.ok();
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/favorites")
    public Result<Map<String, Object>> myFavorites() {
        return Result.ok(actionService.myFavorites());
    }

    @Operation(summary = "我的评分")
    @GetMapping("/ratings")
    public Result<Map<String, Object>> myRatings() {
        return Result.ok(actionService.myRatings());
    }

    @Operation(summary = "提交番剧补充建议（想上架的番剧+观看链接，管理员审核后上架）")
    @PostMapping("/suggest")
    public Result<Void> suggest(@jakarta.validation.Valid @RequestBody com.agito.anime.dto.SuggestDTO dto) {
        actionService.suggestAnime(dto);
        return Result.ok();
    }

    @Operation(summary = "我的提交记录（审核状态）")
    @GetMapping("/suggest/mine")
    public Result<Map<String, Object>> mySuggestions() {
        return Result.ok(actionService.mySuggestions());
    }
}
