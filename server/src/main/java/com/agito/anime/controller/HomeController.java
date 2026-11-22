package com.agito.anime.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.Result;
import com.agito.anime.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页推荐（游客可用：游客走热门策略）
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "03-首页推荐")
public class HomeController {

    private final RecommendService recommendService;

    @Operation(summary = "首页三栏目推荐：今日推荐/猜你喜欢/新番速递")
    @GetMapping("/recommend")
    public Result<?> recommend() {
        Long uid = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        return Result.ok(recommendService.homeRecommend(uid));
    }
}
