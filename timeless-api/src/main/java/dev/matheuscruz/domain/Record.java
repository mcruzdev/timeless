package dev.matheuscruz.domain;

import dev.matheuscruz.presentation.data.UpdateRecordRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "records")
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private BigDecimal amount;
    private String description;
    @Enumerated(EnumType.STRING)
    private Transactions transaction;
    @Enumerated(EnumType.STRING)
    private Categories category;
    private Instant createdAt;

    protected Record() {
    }

    private Record(String userId, BigDecimal amount, String description, Transactions transaction,
            Categories category) {
        this.userId = userId;
        this.amount = amount;
        this.description = description;
        this.transaction = transaction;
        this.category = category;
        this.createdAt = Instant.now();
        this.category = Optional.ofNullable(category).orElse(Categories.NONE);
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Transactions getTransaction() {
        return transaction;
    }

    public Categories getCategory() {
        return Optional.ofNullable(category).orElse(Categories.NONE);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(UpdateRecordRequest request) {
        this.amount = Objects.requireNonNull(request.amount());
        this.description = Objects.requireNonNull(request.description());
        this.transaction = Objects.requireNonNull(request.transaction());
        this.category = Objects.requireNonNull(request.category());
    }

    public static class Builder {
        private String userId;
        private BigDecimal amount;
        private String description;
        private Transactions transaction;
        private Categories category;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder transaction(Transactions transaction) {
            this.transaction = transaction;
            return this;
        }

        public Builder category(Categories category) {
            this.category = category;
            return this;
        }

        public Record build() {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(amount, "amount must not be null");
            Objects.requireNonNull(description, "description must not be null");
            Objects.requireNonNull(transaction, "transaction must not be null");

            if (transaction == Transactions.IN) {
                category = Categories.NONE;
            }
            category = Optional.ofNullable(category).orElse(Categories.GENERAL);

            return new Record(userId, amount, description, transaction, category);
        }

    }
}
