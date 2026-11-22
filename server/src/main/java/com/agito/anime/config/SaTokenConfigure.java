package com.agito.anime.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置：路由级鉴权（5.5.3 节）
 * 游客可访问的只读接口白名单在此放行；写操作与管理端接口须登录。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                // 游客可浏览的只读接口（需求 6.3：游客可完整浏览只读内容）
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/anime/list",
                        "/api/anime/detail/**",
                        "/api/anime/search",
                        "/api/rank/list",
                        "/api/home/recommend",
                        "/api/anime/hot-words"
                );
    }
}
