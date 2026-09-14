package com.zwx.codepulse.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

	@Resource
	private SaTokenCustomConfig saTokenCustomConfig;
	// 注册拦截器
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
		registry.addInterceptor(new SaInterceptor(handle -> {
					// 放行 CORS 预检请求：浏览器预检不可能携带 token，拦截会导致跨域请求被中止
					if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
						return;
					}
					StpUtil.checkLogin();
				}))
				.addPathPatterns(saTokenCustomConfig.getIncludePaths())
				.excludePathPatterns(saTokenCustomConfig.getExcludePaths());
	}
}