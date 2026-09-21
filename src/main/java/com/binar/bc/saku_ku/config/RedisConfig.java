package com.binar.bc.saku_ku.config;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${app.redis.host}")
    private String host;

    @Value("${app.redis.port}")
    private int port;

    @Value("${app.redis.database}")
    private int database;

    @Value("${app.redis.username}")
    private String username;

    @Value("${app.redis.password}")
    private String password;

    @Value("${app.redis.timeout}")
    private Duration timeout;

    @Value("${app.redis.lettuce-pool-max-active}")
    private int poolMaxActive;

    @Value("${app.redis.lettuce-pool-max-wait}")
    private Duration poolMaxWait;

    @Value("${app.redis.lettuce-pool-max-idle}")
    private int poolMaxIdle;

    @Value("${app.redis.lettuce-pool-min-idle}")
    private int poolMinIdle;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        standalone.setDatabase(database);
        if (username != null && !username.isBlank()) {
            standalone.setUsername(username);
        }
        standalone.setPassword(password);

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolMaxActive);
        poolConfig.setMaxWait(poolMaxWait);
        poolConfig.setMaxIdle(poolMaxIdle);
        poolConfig.setMinIdle(poolMinIdle);

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(timeout)
                .poolConfig(poolConfig)
                .build();

        return new LettuceConnectionFactory(standalone, clientConfig);
    }

    @Bean
    public StringRedisTemplate redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
