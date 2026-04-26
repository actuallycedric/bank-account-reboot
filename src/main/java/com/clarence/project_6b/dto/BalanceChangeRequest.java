package com.clarence.project_6b.dto;


public class BalanceChangeRequest {
    private String amount;
    private String description;

    BalanceChangeRequest(){}

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
