package com.wingedsheep.gameserver.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@ConfigurationProperties(prefix = "cache.redis")
data class RedisProperties(
    val enabled: Boolean = false,
    val host: String = "localhost",
    val port: Int = 6379,
    val ttlMinutes: Long = 1440,
    val keyPrefix: String = "argentum:"
)

@Configuration
@EnableConfigurationProperties(RedisProperties::class)
@ConditionalOnProperty(name = ["cache.redis.enabled"], havingValue = "true")
class CacheConfig(
    private val redisProperties: RedisProperties
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(CacheConfig::class.java)

    init {
        logger.info("Redis caching ENABLED - connecting to ${redisProperties.host}:${redisProperties.port}")
    }

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration(redisProperties.host, redisProperties.port)
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        return template
    }
}
