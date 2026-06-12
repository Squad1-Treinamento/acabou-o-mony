package com.acabouomony.payment.infrastructure.config;

import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(name = "cache.backend", havingValue = "redis")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, PaymentResponseDTO> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, PaymentResponseDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<PaymentResponseDTO> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(PaymentResponseDTO.class);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
