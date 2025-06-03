package dev.matheuscruz.domain;

import java.math.BigDecimal;

public class AmountAndTypeOnly {

    private final BigDecimal amount;
    private final Transactions transaction;

    public AmountAndTypeOnly(BigDecimal amount, Transactions transaction) {
        this.amount = amount;
        this.transaction = transaction;
    }

    public Transactions getTransaction() {
        return transaction;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
