package com.acabouomony.payment.infrastructure.config;

import com.acabouomony.payment.web.dto.PaymentResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisConfig")
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Test
    @DisplayName("Creates RedisTemplate with correct serializers")
    void testCreatesRedisTemplateWithCorrectSerializers() {
        RedisConfig config = new RedisConfig();

        RedisTemplate<String, PaymentResponseDTO> template = config.redisTemplate(connectionFactory);

        assertNotNull(template);
        assertNotNull(template.getConnectionFactory());
        assertInstanceOf(StringRedisSerializer.class, template.getKeySerializer());
        assertInstanceOf(Jackson2JsonRedisSerializer.class, template.getValueSerializer());
        assertInstanceOf(StringRedisSerializer.class, template.getHashKeySerializer());
        assertInstanceOf(Jackson2JsonRedisSerializer.class, template.getHashValueSerializer());
    }
}
