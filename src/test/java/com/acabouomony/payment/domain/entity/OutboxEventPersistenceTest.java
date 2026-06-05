package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.OutboxEventStatus;
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
@ContextConfiguration(classes = {OutboxEvent.class})
@Testcontainers
class OutboxEventPersistenceTest {
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
    void canPersistAndLoadOutboxEvent() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventType("payment.created")
                .aggregateId(UUID.randomUUID())
                .payload("{\"id\":123}")
                .status(OutboxEventStatus.valueOf("PENDING"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        em.persist(event);
        em.flush();
        em.clear();
        OutboxEvent found = em.find(OutboxEvent.class, event.getId());
        assertThat(found).isNotNull();
        assertThat(found.getEventType()).isEqualTo("payment.created");
        assertThat(found.getAggregateId()).isEqualTo(event.getAggregateId());
        assertThat(found.getPayload()).contains("id");
        assertThat(found.getStatus()).isEqualTo("PENDING");
        assertThat(found.getRetryCount()).isEqualTo(0);
    }
}
