package uy.eleven.canasta.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${canasta.cache.prices.ttl:300000}")
    private long pricesTtl;

    @Value("${canasta.cache.categories.ttl:900000}")
    private long categoriesTtl;

    @Value("${canasta.cache.analytics.ttl:3600000}")
    private long analyticsTtl;

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(Object.class)));
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put(
                "prices", defaultCacheConfiguration().entryTtl(Duration.ofMillis(pricesTtl)));

        cacheConfigs.put(
                "categories",
                defaultCacheConfiguration().entryTtl(Duration.ofMillis(categoriesTtl)));

        cacheConfigs.put(
                "analytics", defaultCacheConfiguration().entryTtl(Duration.ofMillis(analyticsTtl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
