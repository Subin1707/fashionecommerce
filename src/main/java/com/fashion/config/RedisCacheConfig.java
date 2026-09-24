package com.fashion.config;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();
        RedisSerializationContext.SerializationPair<Object> valueSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(valueSerializer)
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("products", defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("productList", defaults.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("categoryList", defaults.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("categories", defaults.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("carts", defaults.entryTtl(Duration.ofHours(2)))
                .transactionAware()
                .build();
    }
}
