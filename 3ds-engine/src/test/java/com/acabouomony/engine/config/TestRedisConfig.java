package com.acabouomony.engine.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.acabouomony.engine.model.AuthResult;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    ReactiveRedisTemplate<String, Object> testReactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        var keySerializer = StringRedisSerializer.UTF_8;
        var valueSerializer = new GenericJackson2JsonRedisSerializer();
        var serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }

    @Bean
    ReactiveRedisTemplate<String, AuthResult> testAuthResultRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        var keySerializer = StringRedisSerializer.UTF_8;
        var valueSerializer = new Jackson2JsonRedisSerializer<>(AuthResult.class);
        var serializationContext = RedisSerializationContext
                .<String, AuthResult>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
}
