package com.acabouomony.payment.domain.entity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = {Merchant.class})
@Testcontainers
class MerchantPersistenceTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired
    private javax.persistence.EntityManager em;

    @Test
    void canPersistAndLoadMerchant() {
        Merchant merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .apiKeyHash("$argon2id$fakehash")
                .webhookUrl("https://webhook.site/x")
                .createdAt(Instant.now())
                .build();
        em.persist(merchant);
        em.flush();
        em.clear();
        Merchant found = em.find(Merchant.class, merchant.getId());
        assertThat(found).isNotNull();
        assertThat(found.getMerchantId()).isEqualTo(merchant.getMerchantId());
        assertThat(found.getApiKeyHash()).isEqualTo(merchant.getApiKeyHash());
        assertThat(found.getWebhookUrl()).isEqualTo("https://webhook.site/x");
    }
}
