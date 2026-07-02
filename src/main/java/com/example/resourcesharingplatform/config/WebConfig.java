package com.example.resourcesharingplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 前端路由全部转发到 index.html，由 Vue Router 处理
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/resources").setViewName("forward:/index.html");
        registry.addViewController("/resource/**").setViewName("forward:/index.html");
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/register").setViewName("forward:/index.html");
        registry.addViewController("/upload").setViewName("forward:/index.html");
        registry.addViewController("/profile").setViewName("forward:/index.html");
        registry.addViewController("/admin/**").setViewName("forward:/index.html");
    }
}
