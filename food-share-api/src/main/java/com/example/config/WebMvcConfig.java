package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 添加CORS映射
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:21091", "http://localhost:5000", "http://localhost:9000") // 允许前端和推荐系统域名访问
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                .allowedHeaders("*") // 允许所有请求头
                .exposedHeaders("Access-Control-Allow-Headers",
                        "Access-Control-Allow-Methods",
                        "Access-Control-Allow-Origin",
                        "Access-Control-Max-Age",
                        "X-Frame-Options")
                .allowCredentials(true) // 允许发送Cookie
                .maxAge(3600); // 预检请求缓存时间1小时
    }

    /**
     * 配置CORS过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许指定域名访问
        config.addAllowedOrigin("http://localhost:21091");
        config.addAllowedOrigin("http://localhost:5000");
        config.addAllowedOrigin("http://localhost:9000");

        // 允许发送Cookie
        config.setAllowCredentials(true);

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许所有方法
        config.addAllowedMethod("*");

        // 暴露响应头
        config.addExposedHeader("token");

        // 预检请求有效期
        config.setMaxAge(3600L);

        // 对所有接口应用CORS配置
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * 配置文件上传解析器
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    /**
     * 添加静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:upload/");
    }
}