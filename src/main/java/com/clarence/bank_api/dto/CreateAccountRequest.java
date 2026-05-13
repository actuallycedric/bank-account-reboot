package com.clarence.bank_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotBlank(message="Your first name cannot be left blank!")
    private String firstName;

    @NotBlank(message="Your last name cannot be left blank!")
    private String lastName;

    @NotNull(message="Your deposit must be a valid number!")
    @PositiveOrZero(message="Your deposit must be greater than or equal to zero!")
    private BigDecimal initialDeposit;

    public CreateAccountRequest(){}

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
