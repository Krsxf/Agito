package com.agito.anime.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j/OpenAPI3 接口文档配置（5.5 节接口规范，前后端联调辅助）
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfigure {

    @Bean
    public OpenAPI animeHelperOpenApi() {
        return new OpenAPI().info(new Info()
                .title("追番助手 API")
                .description("基于多元数据的个性化追番推荐与补番助手 - REST 接口文档")
                .version("1.0.0"));
    }
}
