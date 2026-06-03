package com.acabouomony.payment.domain.entity;

import jakarta.persistence.EntityManager;
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
@ContextConfiguration(classes = {Transaction.class})
@Testcontainers
class TransactionPersistenceTest {
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
    private EntityManager em;

    @Test
    void canPersistAndLoadTransaction() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID())
                .amount(123_45)
                .currency("BRL")
                .status("CREATED")
                .payloadHash("abc123def456")
                .maskedCard("411111XXXXXX1111")
                .cardTokenId("tok123")
                .acquirerReference("ref456")
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        em.persist(tx);
        em.flush();
        em.clear();
        Transaction found = em.find(Transaction.class, tx.getId());
        assertThat(found).isNotNull();
        assertThat(found.getMerchantId()).isEqualTo(tx.getMerchantId());
        assertThat(found.getIdempotencyKey()).isEqualTo(tx.getIdempotencyKey());
        assertThat(found.getAmount()).isEqualTo(tx.getAmount());
        assertThat(found.getCurrency()).isEqualTo("BRL");
        assertThat(found.getStatus()).isEqualTo("CREATED");
        assertThat(found.getVersion()).isEqualTo(0);
    }
}
