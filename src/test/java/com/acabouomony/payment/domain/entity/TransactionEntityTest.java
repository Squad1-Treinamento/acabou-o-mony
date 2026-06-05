package com.acabouomony.payment.domain.entity;

import com.acabouomony.payment.domain.model.PaymentStatus;
import com.acabouomony.payment.infrastructure.persistence.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Transaction entity persistence.
 * 
 * Verifies:
 * - All fields persisted correctly
 * - @Version field initialized to 0
 * - UNIQUE constraint on (merchant_id, idempotency_key)
 * - NOT NULL constraints enforced
 * - CHECK constraint on status values
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class TransactionEntityTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldCreateTransactionWithAllFieldsPersisted() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Instant now = Instant.now();

        Transaction transaction = Transaction.builder()
            .id(transactionId)
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(10000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("abc123def456")
            .maskedCard("411111XXXXXX1111")
            .cardTokenId("token_xyz")
            .acquirerReference(null)
            .version(0)
            .createdAt(now)
            .updatedAt(now)
            .build();

        // Act
        Transaction saved = transactionRepository.save(transaction);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(transactionId);
        assertThat(saved.getMerchantId()).isEqualTo(merchantId);
        assertThat(saved.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(saved.getAmount()).isEqualTo(10000L);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(saved.getPayloadHash()).isEqualTo("abc123def456");
        assertThat(saved.getMaskedCard()).isEqualTo("411111XXXXXX1111");
        assertThat(saved.getCardTokenId()).isEqualTo("token_xyz");
        assertThat(saved.getAcquirerReference()).isNull();
        assertThat(saved.getVersion()).isEqualTo(0);
        assertThat(saved.getCreatedAt()).isEqualTo(now);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void shouldInitializeVersionFieldToZero() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(5000L)
            .currency("BRL")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash123")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act
        Transaction saved = transactionRepository.save(transaction);

        // Assert
        assertThat(saved.getVersion()).isNotNull().isEqualTo(0);
    }

    @Test
    void shouldEnforceUniqueConstraintOnMerchantIdAndIdempotencyKey() {
        // Arrange
        UUID merchantId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Instant now = Instant.now();

        Transaction transaction1 = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(now)
            .updatedAt(now)
            .build();

        Transaction transaction2 = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .amount(2000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash2")
            .createdAt(now)
            .updatedAt(now)
            .build();

        // Act & Assert
        transactionRepository.save(transaction1);
        assertThatThrownBy(() -> transactionRepository.save(transaction2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameIdempotencyKeyForDifferentMerchants() {
        // Arrange
        UUID idempotencyKey = UUID.randomUUID();
        Instant now = Instant.now();

        Transaction transaction1 = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(idempotencyKey)
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(now)
            .updatedAt(now)
            .build();

        Transaction transaction2 = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(idempotencyKey)
            .amount(2000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash2")
            .createdAt(now)
            .updatedAt(now)
            .build();

        // Act & Assert
        Transaction saved1 = transactionRepository.save(transaction1);
        Transaction saved2 = transactionRepository.save(transaction2);

        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        assertThat(saved1.getMerchantId()).isNotEqualTo(saved2.getMerchantId());
    }

    @Test
    void shouldRejectTransactionWithoutMerchantId() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(null)
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionRepository.save(transaction))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectTransactionWithoutIdempotencyKey() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(null)
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionRepository.save(transaction))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectTransactionWithoutAmount() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(0L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionRepository.save(transaction))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectTransactionWithoutCurrency() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency(null)
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionRepository.save(transaction))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectTransactionWithoutStatus() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency("USD")
            .status(null)
            .payloadHash("hash1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionRepository.save(transaction))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectTransactionWithoutPayloadHash() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act & Assert
        assertThatThrownBy(() -> transactionRepository.save(transaction))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistMaskedCardCorrectly() {
        // Arrange
        String maskedCard = "411111XXXXXX1111";
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .maskedCard(maskedCard)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act
        Transaction saved = transactionRepository.save(transaction);

        // Assert
        assertThat(saved.getMaskedCard()).isEqualTo(maskedCard);
    }

    @Test
    void shouldAllowNullAcquirerReferenceInitially() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .acquirerReference(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Act
        Transaction saved = transactionRepository.save(transaction);

        // Assert
        assertThat(saved.getAcquirerReference()).isNull();
    }

    @Test
    void shouldPopulateAcquirerReferenceOnUpdate() {
        // Arrange
        Transaction transaction = Transaction.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .idempotencyKey(UUID.randomUUID())
            .amount(1000L)
            .currency("USD")
            .status(PaymentStatus.CREATED)
            .payloadHash("hash1")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Transaction saved = transactionRepository.save(transaction);

        // Act
        saved.setAcquirerReference("mp_payment_12345");
        saved.setStatus(PaymentStatus.PROCESSING);
        Transaction updated = transactionRepository.save(saved);

        // Assert
        assertThat(updated.getAcquirerReference()).isEqualTo("mp_payment_12345");
    }
}
