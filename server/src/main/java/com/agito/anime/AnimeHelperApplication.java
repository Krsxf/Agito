package com.agito.anime;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 追番助手后端服务启动类。
 * 分层体系结构（实验五选型）：Controller-Service-Repository 分层 + 统一鉴权横切服务。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.agito.anime.mapper")
public class AnimeHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnimeHelperApplication.class, args);
    }
}
