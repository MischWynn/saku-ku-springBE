package com.binar.bc.saku_ku.config;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

// @EnableCaching di sini (bukan di kelas terpisah) biar 1 file yang megang semua konfigurasi
// Redis - bean StringRedisTemplate (dipakai manual: OTP, token blacklist) dan CacheManager
// (dipakai @Cacheable/@CacheEvict di bawah) beda mekanisme tapi sama-sama connect ke Redis
// yang sama (redisConnectionFactory), jadi masuk akal ditaro bareng.
@EnableCaching
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

    // Backing store buat @Cacheable/@CacheEvict (lihat PlafondService/BungaTenorService) - beda
    // dari StringRedisTemplate di atas yang manual (kamu yang eksplisit .set()/.get()), ini
    // otomatis: Spring yang nyimpen return value method ke Redis pas @Cacheable, dan otomatis
    // baca dari situ dulu di panggilan berikutnya sebelum method-nya beneran dieksekusi lagi.
    // ObjectMapper di-inject (reuse bean yang udah ada di SecurityConfig), bukan bikin instance
    // baru sendiri - biar cuma ada 1 konfigurasi Jackson di seluruh app. GenericJacksonJsonRedisSerializer
    // (BUKAN GenericJackson2JsonRedisSerializer - itu nama lama, dari Jackson 2, sekarang
    // deprecated-marked-for-removal di spring-data-redis 4.1) yang cocok sama project ini, soalnya
    // project ini Spring Boot 4.1 = Jackson 3 (tools.jackson.*, lihat catatan CLAUDE.md).
    @Bean
    public RedisCacheManager cacheManager(LettuceConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // TTL 30 menit - bukan "gak pernah expired". Kalau superadmin edit tier plafond
                // dari device/sesi lain yang somehow ngelewatin @CacheEvict (harusnya gak pernah
                // kejadian kalau semua write lewat PlafondService, tapi tetap ini jaring pengaman
                // biar cache gak permanen basi selamanya).
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value di-serialize JSON (bukan Java serialization bawaan) - lebih portable,
                // dan gak mengharuskan PlafondEntity/BungaTenorEntity implements Serializable.
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJacksonJsonRedisSerializer(objectMapper)))
                // Prefix key di Redis biar gampang dibedain dari key manual (OTP/blacklist pakai
                // prefix "vili:..." sendiri, lihat TokenBlacklistService/OtpService) - defaultnya
                // Spring pakai nama cache doang tanpa pemisah, ini nambahin ":" biar konsisten.
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":");

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}
