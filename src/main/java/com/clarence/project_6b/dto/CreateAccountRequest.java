package com.clarence.project_6b.dto;

import java.math.BigDecimal;

public class CreateAccountRequest {
    private String firstName;
    private String lastName;
    private BigDecimal initialDeposit;

    CreateAccountRequest(){}

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }
}
