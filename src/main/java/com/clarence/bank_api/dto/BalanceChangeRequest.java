package com.clarence.bank_api.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class BalanceChangeRequest {

    @NotNull(message="The amount must be a valid number!")
    @Positive(message="The amount must be positive!")
    private BigDecimal amount;

    private String description;

    public BalanceChangeRequest(){}

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
