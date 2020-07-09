package com.chianxservice.crypto.chainx.gamecenter.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.chianxservice.crypto.chainx.gamecenter.interceptor.GeneralInterceptor;

@Configuration
public class WebConfigurer implements WebMvcConfigurer {
    // 这个方法用来注册拦截器，我们自己写好的拦截器需要通过这里添加注册才能生效

    @Autowired
    protected GeneralInterceptor generalInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(generalInterceptor);//new GeneralInterceptor());
    }
}