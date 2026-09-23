package com.zwx.codepulse;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.zwx.codepulse.mapper")
@EnableAsync
public class CodePulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodePulseApplication.class, args);
    }

}
