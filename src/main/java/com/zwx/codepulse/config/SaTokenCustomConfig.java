package com.zwx.codepulse.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Configuration
@ConfigurationProperties(prefix = "sa-token-custom")
public class SaTokenCustomConfig {
    // 需要拦截的路径
    private List<String> includePaths;
    // 放行排除路径
    private List<String> excludePaths;

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
