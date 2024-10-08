package com.matching.config;

import com.matching.utils.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return new SnowflakeIdGenerator(1, 1); // 需要根據實際情況設置 workerId 和 datacenterId
    }

}
