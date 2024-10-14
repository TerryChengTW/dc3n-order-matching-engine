package com.matching.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    // 從 application.properties 或 application-prod.properties 中獲取 Redis 主機和埠
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(150);           // 最大活躍連接數
        poolConfig.setMaxIdle(100);            // 最大空閒連接數
        poolConfig.setMinIdle(20);             // 最小空閒連接數
        poolConfig.setMaxWait(Duration.ofMillis(1500)); // 獲取連接的最大等待時間

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofMillis(3000))  // 設置連接超時時間
                .build();

        // 使用注入的 redisHost 和 redisPort 來動態配置 RedisStandaloneConfiguration
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(redisHost, redisPort);

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // ZSet 使用 String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Key 序列化為字串
        template.setKeySerializer(stringSerializer);

        // Value 和 HashKey 使用 String 序列化器
        template.setValueSerializer(stringSerializer); // ZSet 存取用 String
        template.setHashKeySerializer(stringSerializer); // HashKey 存取用 String
        template.setHashValueSerializer(stringSerializer); // **將 HashValue 改為 String 序列化**

        return template;
    }
}
