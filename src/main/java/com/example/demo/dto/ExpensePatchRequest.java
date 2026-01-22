package com.example.demo.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExpensePatchRequest {

    private UUID categoryId;

    @Size(min = 1)
    private String name;

    @Positive
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;

    private OffsetDateTime spentAt;

    @Size(max = 255)
    private String location;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OffsetDateTime getSpentAt() {
        return spentAt;
    }

    public void setSpentAt(OffsetDateTime spentAt) {
        this.spentAt = spentAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isEmpty() {
        return categoryId == null
                && name == null
                && amount == null
                && currency == null
                && spentAt == null
                && location == null;
    }
}
