package com.agito.anime.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 推荐结果缓存（数据层降级方案）。
 * 架构上 Redis 承担缓存职责（表11），本机未部署 Redis 时降级为 JVM 内 Caffeine，
 * 接口签名不变，上线时切换 spring.data.redis 配置即可，体现"可替换构件"的分层设计。
 */
@Configuration
public class CacheConfigure {

    @Value("${anime.cache.minutes:30}")
    private int cacheMinutes;

    @Bean
    public Cache<String, Object> recCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(cacheMinutes, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }
}
