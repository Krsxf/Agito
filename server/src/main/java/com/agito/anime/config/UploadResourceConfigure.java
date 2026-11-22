package com.agito.anime.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 上传静态资源映射：/uploads/** → 服务器本地 upload 目录。
 * 所有用户通过 HTTP 访问同一 URL（封面等上传文件），与部署环境无关；
 * 生产环境可由 Nginx 直接托管同一目录以加速静态分发。
 */
@Configuration
public class UploadResourceConfigure implements WebMvcConfigurer {

    @Value("${anime.upload.dir:./upload}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute + java.io.File.separator);
    }
}
