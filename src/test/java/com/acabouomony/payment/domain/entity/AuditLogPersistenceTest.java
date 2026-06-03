package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.entity.AuditLog;
import com.acabouomony.payment.domain.entity.Transaction;
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
@ContextConfiguration(classes = {Transaction.class, AuditLog.class})
@Testcontainers
class AuditLogPersistenceTest {
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
    void canPersistAndLoadAuditLog() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID())
                .amount(5000)
                .currency("USD")
                .status("CREATED")
                .payloadHash("aabbcc")
                .maskedCard("510510XXXXXX5100")
                .cardTokenId("tokx")
                .acquirerReference("refz")
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        em.persist(tx);
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .transaction(tx)
                .oldStatus("CREATED")
                .newStatus("VALIDATED")
                .actor("system")
                .checksum("fakehash123")
                .createdAt(Instant.now())
                .build();
        em.persist(log);
        em.flush();
        em.clear();
        AuditLog found = em.find(AuditLog.class, log.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTransaction().getId()).isEqualTo(tx.getId());
        assertThat(found.getNewStatus()).isEqualTo("VALIDATED");
        assertThat(found.getActor()).isEqualTo("system");
        assertThat(found.getChecksum()).isEqualTo("fakehash123");
    }
}
