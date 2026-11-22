package com.agito.anime.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.agito.anime.common.Result;
import com.agito.anime.dto.LoginDTO;
import com.agito.anime.dto.RegisterDTO;
import com.agito.anime.entity.InterestTag;
import com.agito.anime.mapper.InterestTagMapper;
import com.agito.anime.service.AuthService;
import com.agito.anime.service.impl.AuthServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：注册 / 登录 / 用户信息（游客可注册登录，写操作才需 token）
 */
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "01-认证")
public class AuthController {

    private final AuthService authService;
    private final AuthServiceImpl authServiceImpl;
    private final InterestTagMapper interestTagMapper;

    @Operation(summary = "注册（含 12 类兴趣标签冷启动画像）")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto) {
        String token = authService.register(dto);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.ok(data);
    }

    @Operation(summary = "登录（密码错误 5 次锁定 30 分钟）")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        String token = authService.login(dto);
        authServiceImpl.bindRolesToSession(Long.parseLong(StpUtil.getLoginIdAsString()));
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.ok(data);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @Operation(summary = "当前用户信息（角色/兴趣标签/行为数）")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(authService.userInfo());
    }

    @Operation(summary = "兴趣标签列表（注册页 12 类多选）")
    @GetMapping("/interests")
    public Result<?> interests() {
        return Result.ok(interestTagMapper.selectList(
                new LambdaQueryWrapper<InterestTag>().orderByAsc(InterestTag::getSortNo)));
    }
}
