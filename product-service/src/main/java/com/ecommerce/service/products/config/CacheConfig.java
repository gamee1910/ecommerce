package com.ecommerce.service.products.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PRODUCT = "product";
    public static final String CACHE_PRODUCT_PAGE = "product-page";
    public static final String CACHE_CATEGORY = "category";

    @Value("${cache.product.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${cache.product.l1-max-size:500}")
    private long l1MaxSize;

    @Bean
    @Primary
    CacheManager caffeineCacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(CACHE_PRODUCT, CACHE_PRODUCT_PAGE, CACHE_CATEGORY);
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .recordStats());
        return mgr;
    }

    @Bean
    RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }
}
